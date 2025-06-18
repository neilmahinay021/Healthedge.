package com.example.healthedge

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.healthedge.adapters.WorkoutSectionAdapter
import com.example.healthedge.api.ApiClient
import com.example.healthedge.api.ApiService
import com.example.healthedge.models.DiseaseWorkout
import com.example.healthedge.models.UserWorkoutsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.os.Build
import androidx.core.content.ContextCompat
import android.widget.ProgressBar
import android.preference.PreferenceManager
import com.example.healthedge.models.WorkoutHistoryLog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log
import com.example.healthedge.models.Diagnosis

class FitnessActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var workoutSectionAdapter: WorkoutSectionAdapter
    private lateinit var workoutsRecyclerView: RecyclerView
    private lateinit var noDiagnosesLayout: View
    private var timerJob: kotlinx.coroutines.Job? = null
    private var timerPaused = false
    private var timeLeftMillis: Long = 30000L // default 30 seconds
    private var stepSensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var stepsAtStart: Float? = null
    private var totalSteps: Int = 0
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null
    private var totalDistance: Float = 0f
    private var locationCallback: LocationCallback? = null
    private var stepListener: SensorEventListener? = null
    private var pendingStepTracking: Pair<TextView, TextView>? = null
    private var stepTrackingStarted = false
    private var walkingTimerJob: kotlinx.coroutines.Job? = null
    private var walkingStartTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fitness)

        apiService = ApiClient.apiService

        // Load the Homer Simpson workout GIF into the recommendation card
        val recommendationImage = findViewById<ImageView>(R.id.fitnessRecommendationImage)
        Glide.with(this)
            .asGif()
            .load("https://www.gympaws.com/wp-content/uploads/2018/07/Homer-Simpson-Funny-Workout-GIF-GymPaws-Gloves.gif")
            .placeholder(R.drawable.ic_launcher_background)
            .into(recommendationImage)

        setupViews()
        loadUserWorkouts()

        findViewById<View>(R.id.seeDoctorButton).setOnClickListener {
            // TODO: Implement doctor search/finding functionality
            Toast.makeText(this, "Doctor search coming soon!", Toast.LENGTH_SHORT).show()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_workout -> {
                    // Already here
                    true
                }
                R.id.nav_history -> {
                    showHistoryLogsDialog()
                    true
                }
                R.id.nav_logout -> {
                    prefs.edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_workout
    }

    private fun setupViews() {
        workoutsRecyclerView = findViewById(R.id.workoutsRecyclerView)
        noDiagnosesLayout = findViewById(R.id.noDiagnosesLayout)

        workoutSectionAdapter = WorkoutSectionAdapter { workout ->
            showWorkoutDetailDialog(workout)
        }

        workoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FitnessActivity)
            adapter = workoutSectionAdapter
        }
    }

    private fun loadUserWorkouts() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        Log.d("FitnessActivity", "Retrieved user_id from prefs: $userId")
        if (userId == -1) {
            showNoDiagnosesMessage("Please log in to view your workouts")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("FitnessActivity", "Fetching diagnoses for user_id: $userId")
                val diagnosesResponse = apiService.getUserDiagnoses(userId)
                if (diagnosesResponse.isSuccessful) {
                    val diagnoses = diagnosesResponse.body()
                    Log.d("FitnessActivity", "Diagnoses response: $diagnoses")
                    val latestDiagnosis = diagnoses?.maxByOrNull { it.createdAt ?: "" }
                    if (latestDiagnosis != null && latestDiagnosis.diagnosis != null) {
                        val diagnosisName = latestDiagnosis.diagnosis
                        Log.d("FitnessActivity", "Latest diagnosis: $diagnosisName")
                        // Fetch disease list to map diagnosis name to disease_id
                        val diseaseListResponse = apiService.getDiagnosesList()
                        if (diseaseListResponse.isSuccessful) {
                            val diseaseList = diseaseListResponse.body()
                            val disease = diseaseList?.find { it.name.equals(diagnosisName, ignoreCase = true) }
                            if (disease != null) {
                                val diseaseId = disease.id
                                Log.d("FitnessActivity", "Using disease_id: $diseaseId for diagnosis: $diagnosisName")
                                fetchWorkoutsByDisease(diseaseId)
                            } else {
                                Log.e("FitnessActivity", "No disease_id found for diagnosis: $diagnosisName")
                                showNoDiagnosesMessage("Find a doctor to get a diagnosis and see workouts.")
                            }
                        } else {
                            Log.e("FitnessActivity", "Failed to fetch disease list: ${diseaseListResponse.code()}")
                            showNoDiagnosesMessage("Failed to load disease list.")
                        }
                    } else {
                        Log.e("FitnessActivity", "No diagnosis found for user.")
                        showNoDiagnosesMessage("Find a doctor to get a diagnosis and see workouts.")
                    }
                } else {
                    Log.e("FitnessActivity", "Failed to fetch diagnoses: ${diagnosesResponse.code()}")
                    showNoDiagnosesMessage("Failed to load diagnoses.")
                }
            } catch (e: Exception) {
                Log.e("FitnessActivity", "Error loading user diagnosis or workouts", e)
                showNoDiagnosesMessage("Error: ${e.message}")
            }
        }
    }

    private fun fetchWorkoutsByDisease(diseaseId: Int) {
        Log.d("FitnessActivity", "Making API call to getWorkoutsByDisease with disease_id: $diseaseId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getWorkoutsByDisease(diseaseId)
                withContext(Dispatchers.Main) {
                    Log.d("FitnessActivity", "API response isSuccessful: ${response.isSuccessful}")
                    if (response.isSuccessful) {
                        Log.d("FitnessActivity", "API response body: ${response.body()}")
                        response.body()?.let { handleDiseaseWorkoutsResponse(it) }
                    } else {
                        Log.e("FitnessActivity", "Failed to load workouts: ${response.code()} ${response.errorBody()?.string()}")
                        showError("Failed to load workouts")
                    }
                }
            } catch (e: Exception) {
                Log.e("FitnessActivity", "Error loading workouts", e)
                withContext(Dispatchers.Main) {
                    showError("Error: ${e.message}")
                }
            }
        }
    }

    private fun handleDiseaseWorkoutsResponse(workouts: List<DiseaseWorkout>) {
        if (workouts.isEmpty()) {
            showNoDiagnosesMessage("No workouts available for your disease")
            return
        }
        val section = WorkoutSectionAdapter.WorkoutSection(
            diseaseName = "Workouts",
            workouts = workouts
        )
        workoutSectionAdapter.updateSections(listOf(section))
        workoutsRecyclerView.visibility = View.VISIBLE
        noDiagnosesLayout.visibility = View.GONE
    }

    private fun showNoDiagnosesMessage(message: String) {
        workoutsRecyclerView.visibility = View.GONE
        noDiagnosesLayout.visibility = View.VISIBLE
        findViewById<TextView>(R.id.noDiagnosesText).text = message
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showWorkoutDetailDialog(workout: DiseaseWorkout) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_workout_detail, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val gifDemo = view.findViewById<ImageView>(R.id.gifDemo)
        val workoutName = view.findViewById<TextView>(R.id.workoutNameDetail)
        val workoutDesc = view.findViewById<TextView>(R.id.workoutDesc)
        val timeText = view.findViewById<TextView>(R.id.timeText)
        val startWorkoutButton = view.findViewById<Button>(R.id.startWorkoutButton)
        val countdownTimerText = view.findViewById<TextView>(R.id.countdownTimerText)
        val stopTimerButton = view.findViewById<Button>(R.id.stopTimerButton)
        val stepsText = view.findViewById<TextView>(R.id.stepsText)
        val distanceText = view.findViewById<TextView>(R.id.distanceText)
        val progressBar = view.findViewById<ProgressBar>(R.id.workoutProgressBar)
        val progressText = view.findViewById<TextView>(R.id.progressText)
        val doneButton = view.findViewById<Button>(R.id.doneButton)
        val pauseTimerButton = view.findViewById<Button>(R.id.pauseTimerButton)
        
        stepsText.visibility = View.GONE
        distanceText.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressText.visibility = View.GONE
        doneButton.visibility = View.GONE
        pauseTimerButton.visibility = View.GONE
        stepTrackingStarted = false

        Glide.with(this).asGif().load(workout.gif_url).into(gifDemo)
        workoutName.text = workout.workout_name
        workoutDesc.text = workout.description
        timeText.text = workout.duration

        startWorkoutButton.setOnClickListener {
            updateWorkoutCardProgress(workout, 0)
            if (workout.workout_name.contains("walk", ignoreCase = true)) {
                if (!stepTrackingStarted) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        stepTrackingStarted = true
                        stepsText.visibility = View.VISIBLE
                        distanceText.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE
                        progressText.visibility = View.VISIBLE
                        doneButton.visibility = View.VISIBLE
                        pauseTimerButton.visibility = View.GONE // No pause for walk
                        startStepAndDistanceTracking(stepsText, distanceText)
                        startWorkoutButton.visibility = View.GONE
                        stopTimerButton.visibility = View.VISIBLE
                        updateProgress(progressBar, progressText, 0) // Start at 0%
                        // Show and start count-up timer for walking
                        countdownTimerText.visibility = View.VISIBLE
                        startWalkingTimer(countdownTimerText)
                    } else {
                        stepsText.visibility = View.GONE
                        distanceText.visibility = View.GONE
                        Toast.makeText(this, "Location permission is required to track distance.", Toast.LENGTH_LONG).show()
                        pendingStepTracking = Pair(stepsText, distanceText)
                        requestLocationPermissions()
                    }
                }
            } else {
                // Existing timer logic for other workouts
                startWorkoutButton.visibility = View.GONE
                countdownTimerText.visibility = View.VISIBLE
                stopTimerButton.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                doneButton.visibility = View.VISIBLE
                pauseTimerButton.visibility = View.VISIBLE
                updateProgress(progressBar, progressText, 0) // Start at 0%
                val totalMillis = parseDurationToMillis(workout.duration) ?: 30000L
                timeLeftMillis = totalMillis // Reset timer for this workout
                startTimer(countdownTimerText, totalMillis, progressBar, progressText, workout)
            }
        }

        pauseTimerButton.setOnClickListener {
            timerPaused = !timerPaused
            if (timerPaused) {
                pauseTimerButton.text = "Resume"
            } else {
                pauseTimerButton.text = "Pause"
                // Resume the timer
                val totalMillis = parseDurationToMillis(workout.duration) ?: 30000L
                startTimer(countdownTimerText, totalMillis, progressBar, progressText, workout)
            }
        }

        doneButton.setOnClickListener {
            updateProgress(progressBar, progressText, 100) // Set to 100% when done
            updateWorkoutCardProgress(workout, 100)
            stopStepAndDistanceTracking()
            stopWalkingTimer()
            Toast.makeText(this, "Workout completed!", Toast.LENGTH_SHORT).show()

            // Save workout log to backend
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)
            if (userId != -1) {
                val isWalking = workout.workout_name.contains("walk", ignoreCase = true)
                val durationMinutes = if (isWalking) {
                    // Calculate duration from walking timer
                    val elapsedMillis = System.currentTimeMillis() - walkingStartTime
                    (elapsedMillis / 1000 / 60).toInt().coerceAtLeast(1) // at least 1 min
                } else {
                    // Parse from workout.duration (e.g., "20 min")
                    val regex = Regex("(\\d+)")
                    val match = regex.find(workout.duration)
                    match?.value?.toIntOrNull() ?: 0
                }
                val steps = totalSteps
                val log = com.example.healthedge.models.WorkoutHistoryLog(
                    user_id = userId,
                    date = java.time.LocalDate.now().toString(),
                    workout_type = workout.workout_name,
                    duration_minutes = durationMinutes,
                    steps = steps,
                    calories = null
                )
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        com.example.healthedge.api.ApiClient.apiService.saveWorkoutHistory(log)
                    } catch (_: Exception) {}
                }
            }

            dialog.dismiss()
        }

        stopTimerButton.setOnClickListener {
            stopStepAndDistanceTracking()
            stopWalkingTimer()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateWorkoutCardProgress(workout: DiseaseWorkout, progress: Int) {
        // Find and update the progress of the correct DiseaseWorkout in the adapter's data
        val sections = workoutSectionAdapter.getSections()
        for (section in sections) {
            val match = section.workouts.find { it.workout_name == workout.workout_name && it.description == workout.description }
            if (match != null) {
                match.progress = progress
            }
        }
        workoutSectionAdapter.notifyDataSetChanged()
    }

    private fun startStepAndDistanceTracking(stepsText: TextView, distanceText: TextView) {
        // Step tracking
        stepSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = stepSensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        var usingDetector = false
        if (stepCounterSensor == null) {
            stepCounterSensor = stepSensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            usingDetector = true
            if (stepCounterSensor == null) {
                stepsText.text = "No step sensor available"
                stepsText.visibility = View.VISIBLE
                return
            } else {
                stepsText.text = "Steps: 0"
            }
        } else {
            stepsText.text = "Steps: 0"
        }
        stepsText.visibility = View.VISIBLE
        stepsAtStart = null
        totalSteps = 0
        stepListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (usingDetector) {
                    totalSteps += event.values.size // One event per step
                } else {
                    if (stepsAtStart == null) stepsAtStart = event.values[0]
                    totalSteps = (event.values[0] - (stepsAtStart ?: 0f)).toInt()
                }
                stepsText.text = "Steps: $totalSteps"
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        stepSensorManager?.registerListener(stepListener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)

        // Distance tracking
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        totalDistance = 0f
        lastLocation = null
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    lastLocation?.let {
                        totalDistance += it.distanceTo(location)
                    }
                    lastLocation = location
                    distanceText.text = "Distance: %.2f km".format(totalDistance / 1000)
                }
            }
        }
        val locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Check and request location permissions
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // We have fine location permission, start location updates
                startLocationUpdates(locationRequest, distanceText)
            }
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // We have coarse location permission, start location updates
                startLocationUpdates(locationRequest, distanceText)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show explanation why we need location permission
                Toast.makeText(
                    this,
                    "Location permission is required to track your distance",
                    Toast.LENGTH_LONG
                ).show()
                requestLocationPermissions()
            }
            else -> {
                // Request both permissions
                requestLocationPermissions()
            }
        }
    }

    private fun startLocationUpdates(locationRequest: LocationRequest, distanceText: TextView) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, mainLooper)
        } else {
            distanceText.text = "Location permission required"
            Toast.makeText(
                this,
                "Location permission is required to track distance",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun stopStepAndDistanceTracking() {
        // Unregister step listener
        stepListener?.let { stepSensorManager?.unregisterListener(it) }
        stepListener = null
        // Remove location updates
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }

    private fun updateProgress(progressBar: ProgressBar, progressText: TextView, progress: Int) {
        progressBar.progress = progress
        progressText.text = "$progress%"
    }

    private fun startTimer(
        timerText: TextView, 
        totalMillis: Long,
        progressBar: ProgressBar,
        progressText: TextView,
        workout: DiseaseWorkout
    ): kotlinx.coroutines.Job {
        return lifecycleScope.launch(Dispatchers.Main) {
            var millis = timeLeftMillis
            val initialMillis = totalMillis
            while (millis > 0 && !timerPaused) {
                updateTimerText(timerText, millis)
                // Calculate progress based on time elapsed
                val elapsedMillis = initialMillis - millis
                val progress = 0 + ((elapsedMillis.toFloat() / initialMillis.toFloat()) * 100).toInt()
                updateProgress(progressBar, progressText, progress.coerceAtMost(99)) // Cap at 99% until done button
                updateWorkoutCardProgress(workout, progress.coerceAtMost(99))
                kotlinx.coroutines.delay(1000)
                millis -= 1000
                timeLeftMillis = millis
            }
            if (millis <= 0) {
                updateTimerText(timerText, 0)
                updateProgress(progressBar, progressText, 99) // Cap at 99% until done button
                updateWorkoutCardProgress(workout, 99)
                Toast.makeText(this@FitnessActivity, "Time's up! Click Done to complete.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTimerText(timerText: TextView, millis: Long) {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 1000) / 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun parseDurationToMillis(duration: String?): Long? {
        if (duration == null) return null
        val regex = Regex("(\\d+)")
        val match = regex.find(duration)
        val minutes = match?.value?.toLongOrNull() ?: return null
        return minutes * 60 * 1000
    }

    private fun startWalkingTimer(timerText: TextView) {
        walkingStartTime = System.currentTimeMillis()
        walkingTimerJob?.cancel()
        walkingTimerJob = lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                val elapsedMillis = System.currentTimeMillis() - walkingStartTime
                val seconds = (elapsedMillis / 1000) % 60
                val minutes = (elapsedMillis / 1000) / 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun stopWalkingTimer() {
        walkingTimerJob?.cancel()
        walkingTimerJob = null
    }

    private fun showHistoryLogsDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_history_logs)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = HistoryLogsAdapter()
        recyclerView.adapter = adapter

        // New: Find the steps and training time views
        val stepsCount = dialog.findViewById<TextView>(R.id.stepsCount)
        val stepsProgressBar = dialog.findViewById<ProgressBar>(R.id.stepsProgressBar)
        val trainingPercentage = dialog.findViewById<TextView>(R.id.trainingPercentage)
        val trainingProgressBar = dialog.findViewById<ProgressBar>(R.id.trainingProgressBar)

        // Fetch logs from backend
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val response = ApiClient.apiService.getWorkoutHistory(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val logs = response.body() ?: emptyList()
                        adapter.submitList(logs)
                        // Calculate total steps and total duration
                        val totalSteps = logs.sumOf { it.steps }
                        val totalDuration = logs.sumOf { it.duration_minutes }
                        // Update steps card
                        stepsCount?.text = "$totalSteps/2000"
                        stepsProgressBar?.progress = totalSteps.coerceAtMost(2000)
                        // Update training time card (show percent of 60 min goal)
                        val percent = if (totalDuration > 60) 100 else (totalDuration * 100 / 60)
                        trainingPercentage?.text = "$percent%"
                        trainingProgressBar?.progress = percent
                    } else {
                        adapter.submitList(emptyList())
                        stepsCount?.text = "0/2000"
                        stepsProgressBar?.progress = 0
                        trainingPercentage?.text = "0%"
                        trainingProgressBar?.progress = 0
                    }
                }
            }
        }

        val resetButton = dialog.findViewById<Button>(R.id.resetHistoryButton)
        resetButton?.setOnClickListener {
            // Confirm with the user
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset Workout History")
                .setMessage("Are you sure you want to delete all your workout history? This cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        val userId = prefs.getInt("user_id", -1)
                        val response = ApiClient.apiService.resetWorkoutHistory(mapOf("user_id" to userId))
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@FitnessActivity, "Workout history reset!", Toast.LENGTH_SHORT).show()
                                // Refresh the history list and summary cards
                                adapter.submitList(emptyList())
                                stepsCount?.text = "0/2000"
                                stepsProgressBar?.progress = 0
                                trainingPercentage?.text = "0%"
                                trainingProgressBar?.progress = 0
                            } else {
                                Toast.makeText(this@FitnessActivity, "Failed to reset history", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        dialog.show()
    }

    // Simple adapter for history logs
    inner class HistoryLogsAdapter : RecyclerView.Adapter<HistoryLogsAdapter.HistoryViewHolder>() {
        private var logs: List<WorkoutHistoryLog> = emptyList()
        fun submitList(list: List<WorkoutHistoryLog>) {
            logs = list
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_log, parent, false)
            return HistoryViewHolder(view)
        }
        override fun getItemCount() = logs.size
        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(logs[position])
        }
        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val logDate: TextView = itemView.findViewById(R.id.logDate)
            private val logWorkout: TextView = itemView.findViewById(R.id.logWorkout)
            private val logDuration: TextView = itemView.findViewById(R.id.logDuration)
            private val logSteps: TextView = itemView.findViewById(R.id.logSteps)
            fun bind(log: WorkoutHistoryLog) {
                logDate.text = log.date
                logWorkout.text = log.workout_type
                logDuration.text = "${log.duration_minutes} min"
                logSteps.text = "  •  ${log.steps} steps"
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED || 
                     grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // At least one location permission was granted
                    pendingStepTracking?.let { (stepsText, distanceText) ->
                        stepsText.visibility = View.VISIBLE
                        distanceText.visibility = View.VISIBLE
                        if (!stepTrackingStarted) {
                            stepTrackingStarted = true
                            startStepAndDistanceTracking(stepsText, distanceText)
                        }
                        pendingStepTracking = null
                    }
                } else {
                    // Both permissions were denied
                    pendingStepTracking?.let { (stepsText, distanceText) ->
                        stepsText.visibility = View.GONE
                        distanceText.visibility = View.GONE
                        pendingStepTracking = null
                    }
                    Toast.makeText(
                        this,
                        "Location permission is required to track distance",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
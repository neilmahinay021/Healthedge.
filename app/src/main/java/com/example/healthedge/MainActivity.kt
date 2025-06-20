package com.example.healthedge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.healthedge.api.ApiClient
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.google.android.material.card.MaterialCardView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.app.Dialog
import android.view.View
import android.view.Window
import android.view.ViewGroup
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.example.healthedge.adapters.NotificationAdapter
import com.example.healthedge.models.Notification
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.widget.ImageView
import android.graphics.PorterDuff
import android.util.Log
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var bloodPressureText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var bloodOxygenText: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var userSpinner: Spinner
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private var users: List<com.example.healthedge.models.User> = emptyList()
    private lateinit var fitnessButton: MaterialCardView
    private lateinit var vitalsButton: MaterialCardView
    private lateinit var diagnosisButton: MaterialCardView
    private lateinit var faceScanButton: MaterialCardView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var notificationBadge: BadgeDrawable? = null
    private var notifications: List<Notification> = emptyList()

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }.toTypedArray()
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAllPermissions()
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val role = prefs.getString("role", null)
        if (role == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        if (role == "doctor") {
            startActivity(Intent(this, DoctorDashboardActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        initializeViews()
        setupClickListeners()
        setupToolbar()
        loadNotifications()

        // Greeting and Weather
        val greetingText = findViewById<TextView>(R.id.greetingText)
        val weatherDate = findViewById<TextView>(R.id.weatherDate)
        val weatherCondition = findViewById<TextView>(R.id.weatherCondition)
        val weatherTemp = findViewById<TextView>(R.id.weatherTemp)
        val weatherIcon = findViewById<ImageView>(R.id.weatherIcon)
        val userName = prefs.getString("name", "User")
        
        // Get current hour to determine greeting
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            currentHour < 12 -> "Good morning"  // 12 AM to 11:59 AM
            currentHour < 18 -> "Good afternoon"  // 12 PM to 5:59 PM
            else -> "Good evening"  // 6 PM onwards
        }
        greetingText.text = "$greeting, $userName"
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        weatherDate.text = dateFormat.format(Date())
        // Fetch real weather
        fetchWeather(weatherTemp, weatherCondition, weatherIcon)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.nav_workout -> {
                    startActivity(Intent(this, FitnessActivity::class.java))
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
        bottomNav.selectedItemId = R.id.nav_dashboard
    }

    private fun initializeViews() {
        fitnessButton = findViewById(R.id.fitnessButton)
        vitalsButton = findViewById(R.id.vitalsButton)
        diagnosisButton = findViewById(R.id.diagnosisButton)
        faceScanButton = findViewById(R.id.faceScanButton)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "HealthEdge"
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.primaryBackground))
        notificationBadge = BadgeDrawable.create(this).apply {
            isVisible = false
            backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.primaryBackground)
        }
    }

    private fun setupClickListeners() {
        fitnessButton.setOnClickListener {
            startActivity(Intent(this, FitnessActivity::class.java))
        }
        vitalsButton.setOnClickListener {
            startActivity(Intent(this, VitalsActivity::class.java))
        }
        diagnosisButton.setOnClickListener {
            startActivity(Intent(this, DiagnosisActivity::class.java))
        }
        faceScanButton.setOnClickListener {
            startActivity(Intent(this, UserFaceScanActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val notificationsItem = menu.findItem(R.id.action_notifications)
        if (notificationsItem != null) {
            BadgeUtils.attachBadgeDrawable(
                notificationBadge!!,
                toolbar,
                R.id.action_notifications
            )
            notificationsItem.icon?.setColorFilter(ContextCompat.getColor(this, R.color.primaryBackground), PorterDuff.Mode.SRC_IN)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                showNotificationsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            try {
                val userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getInt("user_id", -1)
                if (userId != -1) {
                    val response = ApiClient.apiService.getNotifications(userId)
                    if (response.isSuccessful) {
                        notifications = response.body()?.sortedByDescending { it.created_at } ?: emptyList()
                        updateNotificationBadge()
                    } else {
                        Log.e("NOTIF_DEBUG", "Failed to load notifications: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("NOTIF_DEBUG", "Failed to load notifications", e)
            }
        }
    }

    private fun updateNotificationBadge() {
        val unreadCount = notifications.count { it.is_read == 0 }
        notificationBadge?.apply {
            isVisible = unreadCount > 0
            number = unreadCount
        }
        invalidateOptionsMenu()
    }

    private fun showNotificationsDialog() {
        try {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_notifications)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val recyclerView = dialog.findViewById<RecyclerView>(R.id.notificationsRecyclerView)
            val noNotificationsText = dialog.findViewById<TextView>(R.id.noNotificationsText)

            if (notifications.isEmpty()) {
                noNotificationsText?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
            } else {
                noNotificationsText?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                recyclerView?.layoutManager = LinearLayoutManager(this)
                recyclerView?.adapter = NotificationAdapter(notifications) { notification ->
                    if (notification.is_read == 0) {
                        markNotificationAsRead(notification.id)
                    }
                    dialog.dismiss()
                }
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("NOTIF_DEBUG", "Error showing notifications dialog", e)
            Toast.makeText(this, "Error showing notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markNotificationAsRead(notificationId: Int) {
        Log.d("NOTIF_DEBUG", "Attempting to mark notification as read: id=$notificationId")
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.markNotificationRead(mapOf("notification_id" to notificationId))
                Log.d("NOTIF_DEBUG", "API response for markNotificationAsRead: success=${response.isSuccessful}, body=${response.body()}")
                if (response.isSuccessful) {
                    // Update local notifications list
                    notifications = notifications.map { 
                        if (it.id == notificationId) it.copy(is_read = 1) else it 
                    }
                    updateNotificationBadge()
                } else {
                    Log.e("NOTIF_DEBUG", "Failed to mark as read: ${response.errorBody()?.string()}")
                    Toast.makeText(this@MainActivity, "Failed to mark notification as read", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NOTIF_DEBUG", "Exception in markNotificationAsRead", e)
                Toast.makeText(this@MainActivity, "Error marking notification as read", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAllPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = permissions.zip(grantResults.toTypedArray()).filter { it.second != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                Toast.makeText(this, "Some permissions are required for full functionality.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Fetch weather for Manila, PH using OpenWeatherMap
    private fun fetchWeather(tempView: TextView, conditionView: TextView, iconView: ImageView) {
        val apiKey = "bd5e378503939ddaee76f12ad7a97608" // TODO: Replace with your OpenWeatherMap API key
        val city = "Manila"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city,PH&appid=$apiKey&units=metric"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tempView.text = "--°C"
                    conditionView.text = "N/A"
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    try {
                        val json = JSONObject(body)
                        if (!json.has("main") || !json.has("weather")) {
                            runOnUiThread {
                                tempView.text = "--°C"
                                conditionView.text = "N/A"
                                iconView.setImageResource(android.R.drawable.ic_menu_help)
                            }
                            return
                        }
                        val main = json.getJSONObject("main")
                        val temp = main.getDouble("temp")
                        val weather = json.getJSONArray("weather").getJSONObject(0)
                        val condition = weather.getString("main")
                        runOnUiThread {
                            tempView.text = "${temp.toInt()}°C"
                            conditionView.text = condition
                            // Set icon based on condition
                            when (condition) {
                                "Clear" -> iconView.setImageResource(R.drawable.sun)
                                "Clouds" -> iconView.setImageResource(R.drawable.cloudy)
                                "Rain" -> iconView.setImageResource(R.drawable.rainy_day)
                                "Thunderstorm" -> iconView.setImageResource(R.drawable.storm)
                                "Drizzle" -> iconView.setImageResource(R.drawable.rainy_day)
                                "Snow" -> iconView.setImageResource(R.drawable.snow)
                                "Hot" -> iconView.setImageResource(R.drawable.hot)
                                else -> iconView.setImageResource(R.drawable.weather)
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            tempView.text = "--°C"
                            conditionView.text = "N/A"
                            iconView.setImageResource(android.R.drawable.ic_menu_help)
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
} 
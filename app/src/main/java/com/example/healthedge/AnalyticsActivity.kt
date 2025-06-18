package com.example.healthedge

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.WorkoutHistoryLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var dateSelectorRecyclerView: RecyclerView
    private lateinit var stepsValue: TextView
    private lateinit var stepsProgressBar: ProgressBar
    private lateinit var trainingTimeValue: TextView
    private lateinit var trainingTimeProgress: ProgressBar
    private lateinit var dateAdapter: DateSelectorAdapter
    private var selectedDate: LocalDate = LocalDate.now()
    private val dateList = mutableListOf<LocalDate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        dateSelectorRecyclerView = findViewById(R.id.dateSelectorRecyclerView)
        stepsValue = findViewById(R.id.stepsValue)
        stepsProgressBar = findViewById(R.id.stepsProgressBar)
        trainingTimeValue = findViewById(R.id.trainingTimeValue)
        trainingTimeProgress = findViewById(R.id.trainingTimeProgress)

        // Generate last 14 days for selector
        for (i in 0..13) {
            dateList.add(LocalDate.now().minusDays((13 - i).toLong()))
        }
        dateAdapter = DateSelectorAdapter(dateList, selectedDate) { date ->
            selectedDate = date
            dateAdapter.selectedDate = date
            dateAdapter.notifyDataSetChanged()
            loadHistoryForDate(date)
        }
        dateSelectorRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        dateSelectorRecyclerView.adapter = dateAdapter

        loadHistoryForDate(selectedDate)
    }

    private fun loadHistoryForDate(date: LocalDate) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) return
        CoroutineScope(Dispatchers.IO).launch {
            val response = ApiClient.apiService.getWorkoutHistory(userId, date = date.toString())
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val logs = response.body() ?: emptyList()
                    val totalSteps = logs.sumOf { it.steps }
                    val totalMinutes = logs.sumOf { it.duration_minutes }
                    stepsValue.text = "$totalSteps"
                    stepsProgressBar.progress = totalSteps
                    trainingTimeValue.text = "$totalMinutes min"
                    trainingTimeProgress.progress = totalMinutes
                } else {
                    stepsValue.text = "0"
                    stepsProgressBar.progress = 0
                    trainingTimeValue.text = "0 min"
                    trainingTimeProgress.progress = 0
                }
            }
        }
    }

    // Adapter for horizontal date selector
    inner class DateSelectorAdapter(
        private val dates: List<LocalDate>,
        var selectedDate: LocalDate,
        private val onDateSelected: (LocalDate) -> Unit
    ) : RecyclerView.Adapter<DateSelectorAdapter.DateViewHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DateViewHolder {
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            return DateViewHolder(view)
        }
        override fun getItemCount() = dates.size
        override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
            val date = dates[position]
            val textView = holder.itemView as TextView
            val formatter = DateTimeFormatter.ofPattern("E d")
            textView.text = date.format(formatter)
            textView.setBackgroundResource(if (date == selectedDate) android.R.color.holo_green_light else android.R.color.transparent)
            textView.setOnClickListener { onDateSelected(date) }
        }
        inner class DateViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)
    }
} 
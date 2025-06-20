package com.example.healthedge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.ImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.widget.TextView
import androidx.core.widget.NestedScrollView

class DoctorDashboardActivity : AppCompatActivity() {
    private lateinit var patientDetailsButton: MaterialCardView
    private lateinit var diagnosisButton: MaterialCardView
    private lateinit var faceScanCard: MaterialCardView
    private lateinit var debugLogsCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        // Greeting and Weather logic
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = prefs.getString("name", "Doctor")
        val greetingText = findViewById<TextView>(R.id.greetingText)
        val weatherDate = findViewById<TextView>(R.id.weatherDate)
        val weatherCondition = findViewById<TextView>(R.id.weatherCondition)
        val weatherTemp = findViewById<TextView>(R.id.weatherTemp)
        val weatherIcon = findViewById<ImageView>(R.id.weatherIcon)

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            currentHour < 12 -> "Good morning"
            currentHour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
        greetingText.text = "$greeting, $userName"

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        weatherDate.text = dateFormat.format(Date())
        fetchWeather(weatherTemp, weatherCondition, weatherIcon)

        patientDetailsButton = findViewById(R.id.patientDetailsButton)
        diagnosisButton = findViewById(R.id.diagnosisButton)
        faceScanCard = findViewById(R.id.faceScanCard)
        debugLogsCard = findViewById(R.id.debugLogsCard)

        patientDetailsButton.setOnClickListener {
            openPatientDetails()
        }
        diagnosisButton.setOnClickListener {
            startActivity(Intent(this, DiagnosisPanelActivity::class.java))
        }

        faceScanCard.setOnClickListener {
            startActivity(Intent(this, DoctorFaceScanActivity::class.java))
        }

        debugLogsCard.setOnClickListener {
            startActivity(Intent(this, DebugLogViewerActivity::class.java))
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            when (item.itemId) {
                R.id.nav_doctor_home -> {
                    // Already here
                    true
                }
                R.id.nav_diagnosis -> {
                    startActivity(Intent(this, DiagnosisPanelActivity::class.java))
                    true
                }
                R.id.nav_scan_logs -> {
                    startActivity(Intent(this, DebugLogViewerActivity::class.java))
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
        bottomNav.selectedItemId = R.id.nav_doctor_home

        val scrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)
        val bottomNavCard = findViewById<MaterialCardView>(R.id.bottom_nav_card)
        scrollView.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, _ ->
            val view = v.getChildAt(v.childCount - 1)
            val diff = view.bottom - (v.height + scrollY)
            if (diff == 0) {
                // At the bottom
                bottomNavCard.alpha = 0.5f // 50% opacity
            } else {
                bottomNavCard.alpha = 1.0f // fully opaque
            }
        }
    }

    private fun openPatientDetails() {
        val intent = Intent(this, PatientCarouselActivity::class.java)
        startActivity(intent)
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
} 
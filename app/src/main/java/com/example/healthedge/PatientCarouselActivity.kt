package com.example.healthedge

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.User
import kotlinx.coroutines.launch

class PatientCarouselActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_carousel)

        val viewPager = findViewById<ViewPager2>(R.id.patientViewPager)

        // Fetch all users from API
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getUsers()
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    if (users.isNotEmpty()) {
                        viewPager.adapter = PatientPagerAdapter(this@PatientCarouselActivity, users)
                    } else {
                        Toast.makeText(this@PatientCarouselActivity, "No patients found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PatientCarouselActivity, "Failed to load patients", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PatientCarouselActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 
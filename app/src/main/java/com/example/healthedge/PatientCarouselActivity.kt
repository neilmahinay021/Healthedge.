package com.example.healthedge

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.User
import kotlinx.coroutines.launch
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import kotlinx.coroutines.CancellationException

class PatientCarouselActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_carousel)

        val viewPager = findViewById<ViewPager2>(R.id.patientViewPager)
        val searchEditText = findViewById<EditText>(R.id.searchPatientEditText)
        var allUsers: List<User> = emptyList()

        // Fetch all users from API
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getUsers()
                if (response.isSuccessful) {
                    allUsers = response.body() ?: emptyList()
                    if (allUsers.isNotEmpty()) {
                        viewPager.adapter = PatientPagerAdapter(this@PatientCarouselActivity, allUsers)
                    } // else: do nothing
                } // else: do nothing
            } catch (e: Exception) {
                // No error toasts
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                val filtered = if (query.isEmpty()) allUsers else allUsers.filter { it.name.lowercase().contains(query) }
                viewPager.adapter = PatientPagerAdapter(this@PatientCarouselActivity, filtered)
            }
        })

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
    }
} 
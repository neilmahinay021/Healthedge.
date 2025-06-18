package com.example.healthedge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class DoctorDashboardActivity : AppCompatActivity() {
    private lateinit var patientDetailsButton: MaterialCardView
    private lateinit var diagnosisButton: MaterialCardView
    private lateinit var faceScanCard: MaterialCardView
    private lateinit var debugLogsCard: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

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
                    // Not yet implemented
                    android.widget.Toast.makeText(this, "Scan Logs coming soon!", android.widget.Toast.LENGTH_SHORT).show()
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

    private fun openPatientDetails() {
        val intent = Intent(this, PatientCarouselActivity::class.java)
        startActivity(intent)
    }
} 
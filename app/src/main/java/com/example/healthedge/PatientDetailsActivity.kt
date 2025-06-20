package com.example.healthedge

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PatientDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_details)

        // Mock data (replace with real data loading logic)
        val patientCode = 11
        val fullName = "kayte"
        val gender = "Female"
        val address = "Sampaloc Manila"
        val age = 23
        val bloodPressure = "120"
        val heartRate = "72 bpm"
        val temperature = "37 C"
        val weight = "150"
        val height = "170"
        val bloodOxygen = "98%"
        val respirationRate = "16 breaths per minute"

        findViewById<TextView>(R.id.patientCode).text = "Patient Code: $patientCode"
        findViewById<TextView>(R.id.patientFullName).text = "Patient Fullname: $fullName"
        findViewById<TextView>(R.id.patientGender).text = "Gender: $gender"
        findViewById<TextView>(R.id.patientAddress).text = "Address: $address"
        findViewById<TextView>(R.id.patientAge).text = "Age: $age"

        findViewById<TextView>(R.id.vitalBloodPressure).text = "Blood Pressure: $bloodPressure"
        findViewById<TextView>(R.id.vitalHeartRate).text = "Heart Rate: $heartRate"
        findViewById<TextView>(R.id.vitalTemperature).text = "Temperature: $temperature"
        findViewById<TextView>(R.id.vitalWeight).text = "Weight: $weight"
        findViewById<TextView>(R.id.vitalHeight).text = "Height: $height"
        findViewById<TextView>(R.id.vitalBloodOxygen).text = "Blood Oxygen: $bloodOxygen"
        findViewById<TextView>(R.id.vitalRespirationRate).text = "Respiration Rate: $respirationRate"

        // Removed QR code logic: delete any lines with qrCodeUrl, qrImage, and Glide
    }
}

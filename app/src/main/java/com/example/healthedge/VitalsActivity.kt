package com.example.healthedge

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.Vitals
import kotlinx.coroutines.launch

class VitalsActivity : AppCompatActivity() {
    private lateinit var vitalsReadOnlyLayout: LinearLayout
    private lateinit var vitalsFormLayout: LinearLayout
    private lateinit var vitalsSuccessMessage: TextView
    private lateinit var bloodPressureView: TextView
    private lateinit var heartRateView: TextView
    private lateinit var temperatureView: TextView
    private lateinit var weightView: TextView
    private lateinit var heightView: TextView
    private lateinit var bloodOxygenView: TextView
    private lateinit var respirationRateView: TextView
    private lateinit var bloodPressureInput: EditText
    private lateinit var heartRateInput: EditText
    private lateinit var temperatureInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var bloodOxygenInput: EditText
    private lateinit var respirationRateInput: EditText
    private lateinit var updateVitalsButton: Button
    private var userId: Int = -1
    private var isEditMode = false
    private var currentVitals: Vitals? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vitals)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        vitalsReadOnlyLayout = findViewById(R.id.vitalsReadOnlyLayout)
        vitalsFormLayout = findViewById(R.id.vitalsFormLayout)
        vitalsSuccessMessage = findViewById(R.id.vitalsSuccessMessage)
        bloodPressureView = findViewById(R.id.bloodPressureView)
        heartRateView = findViewById(R.id.heartRateView)
        temperatureView = findViewById(R.id.temperatureView)
        weightView = findViewById(R.id.weightView)
        heightView = findViewById(R.id.heightView)
        bloodOxygenView = findViewById(R.id.bloodOxygenView)
        respirationRateView = findViewById(R.id.respirationRateView)
        bloodPressureInput = findViewById(R.id.bloodPressureInput)
        heartRateInput = findViewById(R.id.heartRateInput)
        temperatureInput = findViewById(R.id.temperatureInput)
        weightInput = findViewById(R.id.weightInput)
        heightInput = findViewById(R.id.heightInput)
        bloodOxygenInput = findViewById(R.id.bloodOxygenInput)
        respirationRateInput = findViewById(R.id.respirationRateInput)
        updateVitalsButton = findViewById(R.id.updateVitalsButton)

        // Set user name in the card
        val userNameView = findViewById<TextView>(R.id.userNameView)
        val userName = prefs.getString("user_name", "User Name")
        userNameView.text = userName

        // Set patient icon
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)
        profileIcon.setImageResource(R.drawable.ic_patient)

        loadUserVitals(userId)

        updateVitalsButton.setOnClickListener {
            if (!isEditMode) {
                // Switch to edit mode
                showEditMode()
            } else {
                // Save/update vitals
                val vitals = Vitals(
                    id = currentVitals?.id ?: 0,
                    userId = userId,
                    bloodPressure = bloodPressureInput.text.toString().trim(),
                    heartRate = heartRateInput.text.toString().trim(),
                    temperature = temperatureInput.text.toString().trim(),
                    weight = weightInput.text.toString().trim(),
                    height = heightInput.text.toString().trim(),
                    bloodOxygen = bloodOxygenInput.text.toString().trim(),
                    respirationRate = respirationRateInput.text.toString().trim(),
                    qrCodeImage = null
                )
                updateUserVitals(vitals)
            }
        }
    }

    private fun loadUserVitals(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getVitals(userId)
                if (response.isSuccessful) {
                    val vitals = response.body()?.firstOrNull()
                    currentVitals = vitals
                    if (vitals != null) {
                        // Show read-only mode
                        showReadOnlyMode(vitals)
                    } else {
                        // No vitals yet, show edit mode
                        showEditMode()
                    }
                } else {
                    Toast.makeText(this@VitalsActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VitalsActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReadOnlyMode(vitals: Vitals) {
        isEditMode = false
        vitalsReadOnlyLayout.visibility = View.VISIBLE
        vitalsFormLayout.visibility = View.GONE
        updateVitalsButton.text = "Update Vitals"
        vitalsSuccessMessage.visibility = View.GONE
        bloodPressureView.text = vitals.bloodPressure ?: ""
        heartRateView.text = vitals.heartRate ?: ""
        temperatureView.text = vitals.temperature ?: ""
        weightView.text = vitals.weight ?: ""
        heightView.text = vitals.height ?: ""
        bloodOxygenView.text = vitals.bloodOxygen ?: ""
        respirationRateView.text = vitals.respirationRate ?: ""
    }

    private fun showEditMode() {
        isEditMode = true
        vitalsReadOnlyLayout.visibility = View.GONE
        vitalsFormLayout.visibility = View.VISIBLE
        updateVitalsButton.text = "Save"
        vitalsSuccessMessage.visibility = View.GONE
        // Pre-fill fields if data exists
        currentVitals?.let { v ->
            bloodPressureInput.setText(v.bloodPressure ?: "")
            heartRateInput.setText(v.heartRate ?: "")
            temperatureInput.setText(v.temperature ?: "")
            weightInput.setText(v.weight ?: "")
            heightInput.setText(v.height ?: "")
            bloodOxygenInput.setText(v.bloodOxygen ?: "")
            respirationRateInput.setText(v.respirationRate ?: "")
        }
    }

    private fun updateUserVitals(vitals: Vitals) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.addVitals(vitals)
                if (response.isSuccessful && response.body()?.get("success") == true) {
                    // After update, reload and show read-only mode
                    loadUserVitals(userId)
                    Toast.makeText(this@VitalsActivity, "Vitals updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VitalsActivity, "Failed to update vitals", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VitalsActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
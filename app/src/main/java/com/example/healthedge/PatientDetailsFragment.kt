package com.example.healthedge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PatientDetailsFragment : Fragment() {
    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_USER_GENDER = "user_gender"
        private const val ARG_USER_ADDRESS = "user_address"
        private const val ARG_USER_AGE = "user_age"

        fun newInstance(user: User): PatientDetailsFragment {
            val fragment = PatientDetailsFragment()
            val args = Bundle()
            args.putInt(ARG_USER_ID, user.id)
            args.putString(ARG_USER_NAME, user.name)
            args.putString(ARG_USER_GENDER, user.gender)
            args.putString(ARG_USER_ADDRESS, user.address)
            args.putInt(ARG_USER_AGE, user.age ?: 0)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_patient_details, container, false)

        val userId = arguments?.getInt(ARG_USER_ID) ?: 0
        val userName = arguments?.getString(ARG_USER_NAME) ?: ""
        val userGender = arguments?.getString(ARG_USER_GENDER) ?: ""
        val userAddress = arguments?.getString(ARG_USER_ADDRESS) ?: ""
        val userAge = arguments?.getInt(ARG_USER_AGE) ?: 0

        // Display patient info
        view.findViewById<TextView>(R.id.patientCode).text = "Patient Code: $userId"
        view.findViewById<TextView>(R.id.patientFullName).text = "Patient Fullname: $userName"
        view.findViewById<TextView>(R.id.patientGender).text = "Gender: $userGender"
        view.findViewById<TextView>(R.id.patientAddress).text = "Address: $userAddress"
        view.findViewById<TextView>(R.id.patientAge).text = "Age: $userAge"

        // Fetch vitals from API
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getVitals(userId)
                if (response.isSuccessful) {
                    val vitalsList = response.body()
                    if (!vitalsList.isNullOrEmpty()) {
                        val vitals = vitalsList.first()
                        view.findViewById<TextView>(R.id.vitalBloodPressure).text = "Blood Pressure: ${vitals.bloodPressure ?: "N/A"}"
                        view.findViewById<TextView>(R.id.vitalHeartRate).text = "Heart Rate: ${vitals.heartRate ?: "N/A"}"
                        view.findViewById<TextView>(R.id.vitalTemperature).text = "Temperature: ${vitals.temperature ?: "N/A"}"
                        view.findViewById<TextView>(R.id.vitalWeight).text = "Weight: ${vitals.weight ?: "N/A"}"
                        view.findViewById<TextView>(R.id.vitalHeight).text = "Height: ${vitals.height ?: "N/A"}"
                        view.findViewById<TextView>(R.id.vitalBloodOxygen).text = "Blood Oxygen: ${vitals.bloodOxygen ?: "N/A"}"
                        view.findViewById<TextView>(R.id.vitalRespirationRate).text = "Respiration Rate: ${vitals.respirationRate ?: "N/A"}"
                    } else {
                        // No vitals found, show N/A for all fields
                        view.findViewById<TextView>(R.id.vitalBloodPressure).text = "Blood Pressure: N/A"
                        view.findViewById<TextView>(R.id.vitalHeartRate).text = "Heart Rate: N/A"
                        view.findViewById<TextView>(R.id.vitalTemperature).text = "Temperature: N/A"
                        view.findViewById<TextView>(R.id.vitalWeight).text = "Weight: N/A"
                        view.findViewById<TextView>(R.id.vitalHeight).text = "Height: N/A"
                        view.findViewById<TextView>(R.id.vitalBloodOxygen).text = "Blood Oxygen: N/A"
                        view.findViewById<TextView>(R.id.vitalRespirationRate).text = "Respiration Rate: N/A"
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load vitals", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                // else: ignore cancellation exception
            }
        }

        return view
    }
} 
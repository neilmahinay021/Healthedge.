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
                        view.findViewById<TextView>(R.id.vitalBloodPressure).text = vitals.bloodPressure ?: "N/A"
                        view.findViewById<TextView>(R.id.vitalHeartRate).text = vitals.heartRate ?: "N/A"
                        view.findViewById<TextView>(R.id.vitalTemperature).text = vitals.temperature ?: "N/A"
                        view.findViewById<TextView>(R.id.vitalWeight).text = vitals.weight ?: "N/A"
                        view.findViewById<TextView>(R.id.vitalHeight).text = vitals.height ?: "N/A"
                        view.findViewById<TextView>(R.id.vitalBloodOxygen).text = vitals.bloodOxygen ?: "N/A"
                        view.findViewById<TextView>(R.id.vitalRespirationRate).text = vitals.respirationRate ?: "N/A"

                        // Generate QR code URL for the patient
                        val qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=100x100&data=Patient$userId"
                        val qrImage = view.findViewById<ImageView>(R.id.vitalQrCode)
                        Glide.with(this@PatientDetailsFragment)
                            .load(qrCodeUrl)
                            .into(qrImage)
                    } else {
                        // No vitals found, show N/A for all fields
                        view.findViewById<TextView>(R.id.vitalBloodPressure).text = "N/A"
                        view.findViewById<TextView>(R.id.vitalHeartRate).text = "N/A"
                        view.findViewById<TextView>(R.id.vitalTemperature).text = "N/A"
                        view.findViewById<TextView>(R.id.vitalWeight).text = "N/A"
                        view.findViewById<TextView>(R.id.vitalHeight).text = "N/A"
                        view.findViewById<TextView>(R.id.vitalBloodOxygen).text = "N/A"
                        view.findViewById<TextView>(R.id.vitalRespirationRate).text = "N/A"
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load vitals", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.vitalEditButton).setOnClickListener {
            Toast.makeText(requireContext(), "Edit vitals clicked!", Toast.LENGTH_SHORT).show()
        }

        return view
    }
} 
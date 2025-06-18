package com.example.healthedge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.databinding.FragmentDoctorRegisterBinding
import com.example.healthedge.models.DoctorRegistration
import kotlinx.coroutines.launch

class DoctorRegisterFragment : Fragment() {
    private var _binding: FragmentDoctorRegisterBinding? = null
    private val binding get() = _binding!!
    private var isIdVerified = false

    companion object {
        private const val REQUEST_ID_SCAN = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorRegisterBinding.inflate(inflater, container, false)

        binding.scanIdButton.setOnClickListener {
            startActivityForResult(Intent(requireContext(), DoctorIdScanActivity::class.java), REQUEST_ID_SCAN)
        }

        binding.registerButton.setOnClickListener {
            if (!isIdVerified) {
                Toast.makeText(requireContext(), "Please scan and verify your medical license ID first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (validateInputs()) {
                registerDoctor()
            }
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ID_SCAN && resultCode == Activity.RESULT_OK) {
            isIdVerified = data?.getBooleanExtra(DoctorIdScanActivity.EXTRA_ID_VERIFIED, false) ?: false
            if (isIdVerified) {
                binding.scanIdButton.text = "Medical License ID Verified ✓"
                binding.scanIdButton.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            }
        }
    }

    private fun validateInputs(): Boolean {
        with(binding) {
            if (nameInput.text.toString().trim().isEmpty()) {
                nameInput.error = "Name is required"
                return false
            }
            if (emailInput.text.toString().trim().isEmpty()) {
                emailInput.error = "Email is required"
                return false
            }
            if (passwordInput.text.toString().trim().isEmpty()) {
                passwordInput.error = "Password is required"
                return false
            }
            if (specializationInput.text.toString().trim().isEmpty()) {
                specializationInput.error = "Specialization is required"
                return false
            }
        }
        return true
    }

    private fun registerDoctor() {
        binding.registerButton.isEnabled = false
        binding.progressBar.show()
        val doctorData = DoctorRegistration(
            name = binding.nameInput.text.toString().trim(),
            email = binding.emailInput.text.toString().trim(),
            password = binding.passwordInput.text.toString().trim(),
            specialization = binding.specializationInput.text.toString().trim(),
            idVerified = isIdVerified
        )
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.registerDoctor(doctorData)
                val responseBody = response.body()
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("DoctorRegisterFragment", "isSuccessful: ${response.isSuccessful}")
                android.util.Log.e("DoctorRegisterFragment", "Response body: $responseBody")
                android.util.Log.e("DoctorRegisterFragment", "Error body: $errorBody")
                android.util.Log.e("DoctorRegisterFragment", "Message: ${response.message()}")
                android.util.Log.e("DoctorRegisterFragment", "Code: ${response.code()}")

                if (response.isSuccessful && responseBody?.get("success") == true) {
                    Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                } else {
                    val errorMessage = responseBody?.get("error") as? String ?: errorBody ?: "Unknown error"
                    Toast.makeText(requireContext(), "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                    binding.registerButton.isEnabled = true
                    binding.progressBar.hide()
                }
            } catch (e: Exception) {
                android.util.Log.e("DoctorRegisterFragment", "Exception: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                binding.progressBar.hide()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
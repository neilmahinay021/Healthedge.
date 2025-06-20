package com.example.healthedge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.DoctorRegistration
import com.example.healthedge.databinding.ActivityDoctorRegisterBinding
import kotlinx.coroutines.launch

class DoctorRegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorRegisterBinding
    private var isIdVerified = false

    companion object {
        private const val REQUEST_ID_SCAN = 1
        private const val REQUEST_LICENSE_INSTRUCTION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanIdButton.setOnClickListener {
            val intent = Intent(this, LicenseInstructionActivity::class.java)
            startActivityForResult(intent, REQUEST_LICENSE_INSTRUCTION)
        }

        binding.registerButton.setOnClickListener {
            if (!isIdVerified) {
                Toast.makeText(this, "Please scan and verify your medical license ID first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (validateInputs()) {
                registerDoctor()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ID_SCAN && resultCode == RESULT_OK) {
            isIdVerified = data?.getBooleanExtra(DoctorIdScanActivity.EXTRA_ID_VERIFIED, false) ?: false
            if (isIdVerified) {
                binding.scanIdButton.text = "Medical License ID Verified ✓"
                binding.scanIdButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, theme))
            }
        } else if (requestCode == REQUEST_LICENSE_INSTRUCTION && resultCode == RESULT_OK) {
            startActivityForResult(Intent(this, DoctorIdScanActivity::class.java), REQUEST_ID_SCAN)
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
                Log.e("DoctorRegister", "isSuccessful: ${response.isSuccessful}")
                Log.e("DoctorRegister", "Response body: $responseBody")
                Log.e("DoctorRegister", "Error body: $errorBody")
                Log.e("DoctorRegister", "Message: ${response.message()}")
                Log.e("DoctorRegister", "Code: ${response.code()}")

                if (response.isSuccessful && responseBody?.get("success") == true) {
                    Toast.makeText(this@DoctorRegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@DoctorRegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    val errorMessage = responseBody?.get("error") as? String ?: errorBody ?: "Unknown error"
                    Toast.makeText(this@DoctorRegisterActivity, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                    binding.registerButton.isEnabled = true
                    binding.progressBar.hide()
                }
            } catch (e: Exception) {
                Log.e("DoctorRegister", "Exception: ${e.message}", e)
                Toast.makeText(this@DoctorRegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                binding.progressBar.hide()
            }
        }
    }
} 
package com.example.healthedge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.api.FaceApi
import com.example.healthedge.api.FaceRegistrationRequest
import com.example.healthedge.databinding.FragmentPatientRegisterBinding
import com.example.healthedge.models.UserRegistration
import kotlinx.coroutines.launch
import com.example.healthedge.ui.FaceCaptureActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun Context.appendLogToFile(filename: String, text: String) {
    try {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $text\n"
        
        // Use internal storage directory
        val file = File(filesDir, filename)
        file.appendText(logEntry)
    } catch (e: Exception) {
        Log.e("FileLogging", "Error writing to log file: ${e.message}")
    }
}

fun Context.readLogFile(filename: String): String {
    return try {
        val file = File(filesDir, filename)
        if (file.exists()) {
            file.readText()
        } else {
            "No log file found"
        }
    } catch (e: Exception) {
        Log.e("FileLogging", "Error reading log file: ${e.message}")
        "Error reading log file: ${e.message}"
    }
}

fun Context.clearLogFile(filename: String) {
    try {
        val file = File(filesDir, filename)
        if (file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        Log.e("FileLogging", "Error clearing log file: ${e.message}")
    }
}

class PatientRegisterFragment : Fragment() {
    private var _binding: FragmentPatientRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var faceApi: FaceApi
    private var userId: String? = null
    private val faceCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        requireContext().appendLogToFile("face_registration_debug.txt", "ActivityResultCallback called: resultCode=${result.resultCode}, data=${result.data}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val faceEmbedding = result.data?.getStringExtra(FaceCaptureActivity.EXTRA_FACE_EMBEDDING)
            requireContext().appendLogToFile("face_registration_debug.txt", "Received faceEmbedding: ${faceEmbedding?.take(100)}")
            if (faceEmbedding != null && userId != null) {
                registerFace(faceEmbedding)
            } else {
                requireContext().appendLogToFile("face_registration_debug.txt", "Face capture failed or userId is null")
                Toast.makeText(requireContext(), "Face capture failed", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                binding.progressBar.hide()
            }
        } else {
            requireContext().appendLogToFile("face_registration_debug.txt", "Face registration cancelled or failed")
            Toast.makeText(requireContext(), "Face registration cancelled", Toast.LENGTH_SHORT).show()
            binding.registerButton.isEnabled = true
            binding.progressBar.hide()
        }
    }

    private val cameraInstructionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startFaceRegistration()
        } else {
            binding.registerButton.isEnabled = true
            binding.progressBar.hide()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientRegisterBinding.inflate(inflater, container, false)
        requireContext().appendLogToFile("registration_debug.txt", "TEST: onCreateView called")
        requireContext().appendLogToFile("face_registration_debug.txt", "TEST: onCreateView called")
        faceApi = ApiClient.retrofit.create(FaceApi::class.java)
        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }
        return binding.root
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
            if (ageInput.text.toString().trim().isEmpty()) {
                ageInput.error = "Age is required"
                return false
            }
            if (genderInput.text.toString().trim().isEmpty()) {
                genderInput.error = "Gender is required"
                return false
            }
            if (contactInput.text.toString().trim().isEmpty()) {
                contactInput.error = "Contact number is required"
                return false
            }
            if (addressInput.text.toString().trim().isEmpty()) {
                addressInput.error = "Address is required"
                return false
            }
        }
        return true
    }

    private fun registerUser() {
        binding.registerButton.isEnabled = false
        binding.progressBar.show()
        val userData = UserRegistration(
            name = binding.nameInput.text.toString().trim(),
            email = binding.emailInput.text.toString().trim(),
            password = binding.passwordInput.text.toString().trim(),
            age = binding.ageInput.text.toString().trim().toInt(),
            gender = binding.genderInput.text.toString().trim(),
            contact_no = binding.contactInput.text.toString().trim(),
            address = binding.addressInput.text.toString().trim()
        )
        lifecycleScope.launch {
            try {
                requireContext().appendLogToFile("registration_debug.txt", "SENDING REQUEST: ${userData.toString()}")
                val response = ApiClient.apiService.registerUser(userData)
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    requireContext().appendLogToFile("registration_debug.txt", "ERROR RESPONSE: $errorBody")
                    Toast.makeText(requireContext(), "Registration failed: $errorBody", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    binding.progressBar.hide()
                    return@launch
                }

                val responseBody = response.body()
                requireContext().appendLogToFile("registration_debug.txt", "RESPONSE BODY: $responseBody")
                
                if (responseBody == null) {
                    requireContext().appendLogToFile("registration_debug.txt", "NULL RESPONSE BODY")
                    Toast.makeText(requireContext(), "Registration failed: Server returned null response", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    binding.progressBar.hide()
                    return@launch
                }

                val success = responseBody["success"] as? Boolean
                val userId = responseBody["id"]?.toString()
                val error = responseBody["error"] as? String

                if (success == true && userId != null) {
                    this@PatientRegisterFragment.userId = userId
                    // Show camera instruction before face registration
                    val intent = Intent(requireContext(), CameraInstructionActivity::class.java)
                    cameraInstructionLauncher.launch(intent)
                } else {
                    val errorMessage = error ?: "Unknown error occurred"
                    requireContext().appendLogToFile("registration_debug.txt", "REGISTRATION FAILED: $errorMessage")
                    Toast.makeText(requireContext(), "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    binding.progressBar.hide()
                }
            } catch (e: Exception) {
                requireContext().appendLogToFile("registration_debug.txt", "EXCEPTION: ${e.message}\n${e.stackTraceToString()}")
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                binding.progressBar.hide()
            }
        }
    }

    private fun startFaceRegistration() {
        val intent = Intent(requireContext(), FaceCaptureActivity::class.java)
        faceCaptureLauncher.launch(intent)
    }

    private fun registerFace(faceEmbedding: String) {
        lifecycleScope.launch {
            try {
                val request = FaceRegistrationRequest(userId!!, faceEmbedding)
                requireContext().appendLogToFile("face_registration_debug.txt", "REQUEST: userId=$userId, faceEmbedding=${faceEmbedding.take(100)}...")
                val response = faceApi.registerFace(request)
                requireContext().appendLogToFile("face_registration_debug.txt", "RESPONSE: success=${response.success}, message=${response.message}")
                if (response.success) {
                    Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                } else {
                    Toast.makeText(requireContext(), "Face registration failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    binding.registerButton.isEnabled = true
                    binding.progressBar.hide()
                }
            } catch (e: Exception) {
                requireContext().appendLogToFile("face_registration_debug.txt", "EXCEPTION: ${e.message}")
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.registerButton.isEnabled = true
                binding.progressBar.hide()
            }
        }
    }

    companion object {
        private const val FACE_CAPTURE_REQUEST_CODE = 1001
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
package com.example.healthedge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthedge.api.FaceApi
import com.example.healthedge.api.FaceVerificationRequest
import com.example.healthedge.databinding.ActivityUserFaceScanBinding
import com.example.healthedge.ui.FaceCaptureActivity
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.Diagnosis
import com.example.healthedge.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UserFaceScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserFaceScanBinding
    private lateinit var faceApi: FaceApi
    private lateinit var diagnosisAdapter: DiagnosisHistoryAdapter
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserFaceScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from preferences
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserId = prefs.getInt("user_id", -1)
        if (currentUserId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRetrofit()
        setupVitalsDisplay()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRetrofit() {
        faceApi = ApiClient.faceApi
    }

    private fun setupVitalsDisplay() {
        binding.vitalsLayout.visibility = View.GONE
        binding.diagnosisLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        diagnosisAdapter = DiagnosisHistoryAdapter { diagnosis ->
            generateAndSharePdf(diagnosis)
        }
        binding.diagnosisRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserFaceScanActivity)
            adapter = diagnosisAdapter
        }
    }

    private fun setupClickListeners() {
        binding.scanFaceButton.setOnClickListener {
            startFaceCapture()
        }
    }

    private fun startFaceCapture() {
        val intent = Intent(this, FaceCaptureActivity::class.java)
        startActivityForResult(intent, FACE_CAPTURE_REQUEST_CODE)
    }

    private fun verifyFaceAndGetVitals(faceEmbedding: String) {
        binding.statusText.text = "Verifying face..."
        binding.scanFaceButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = FaceVerificationRequest(faceEmbedding)
                val response = faceApi.verifyFace(request)
                
                if (response.success) {
                    response.vitals?.let { vitals ->
                        // Verify that the scanned face matches the logged-in user
                        val scannedUserId = (vitals["user_id"] as? Number)?.toInt() ?: vitals["user_id"].toString().replace(".0", "").toInt()
                        if (scannedUserId == currentUserId) {
                            displayVitals(vitals)
                            fetchAndDisplayDiagnoses(currentUserId)
                        } else {
                            binding.statusText.text = "Face verification failed: Not your account"
                            binding.vitalsLayout.visibility = View.GONE
                            binding.diagnosisLayout.visibility = View.GONE
                        }
                    } ?: run {
                        binding.statusText.text = "No vitals available"
                        binding.vitalsLayout.visibility = View.GONE
                        binding.diagnosisLayout.visibility = View.GONE
                    }
                } else {
                    binding.statusText.text = "Face verification failed"
                    binding.vitalsLayout.visibility = View.GONE
                    binding.diagnosisLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.statusText.text = "Error: ${e.message}"
                binding.vitalsLayout.visibility = View.GONE
                binding.diagnosisLayout.visibility = View.GONE
            } finally {
                binding.scanFaceButton.isEnabled = true
            }
        }
    }

    private fun displayVitals(vitals: Map<String, Any>) {
        binding.vitalsLayout.visibility = View.VISIBLE
        
        // Display user code and email
        val userId = (vitals["user_id"] as? Number)?.toInt() ?: vitals["user_id"].toString().replace(".0", "")
        binding.patientNameText.text = "User Code: $userId"
        binding.patientEmailText.text = "Email: ${vitals["email"]}"
        
        // Display main vitals
        binding.heartRateText.text = "Heart Rate: ${vitals["heart_rate"]}"
        binding.bloodPressureText.text = "Blood Pressure: ${vitals["blood_pressure"]}"
        binding.temperatureText.text = "Temperature: ${vitals["temperature"]}"
        val oxygenSaturation = vitals["oxygen_saturation"] ?: vitals["blood_oxygen"] ?: "N/A"
        binding.oxygenText.text = "Oxygen Saturation: $oxygenSaturation"

        // Display additional vitals in the card
        val extraVitals = StringBuilder()
        extraVitals.append("Weight: ${vitals["weight"]}\n")
        extraVitals.append("Height: ${vitals["height"]}\n")
        extraVitals.append("Respiration Rate: ${vitals["respiration_rate"]}\n")

        // Format and display timestamp if available
        val timestamp = vitals["timestamp"] as? String
        if (timestamp != null) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                binding.timestampText.text = "Last Updated: ${outputFormat.format(date)}"
            } catch (e: Exception) {
                binding.timestampText.text = "Last Updated: $timestamp"
            }
        }
        
        binding.statusText.text = "Vitals retrieved successfully"
        // Add extra vitals to the card
        binding.oxygenText.append("\n" + extraVitals.toString())
    }

    private fun fetchAndDisplayDiagnoses(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getUserDiagnoses(userId)
                if (response.isSuccessful) {
                    val diagnoses = response.body()
                    if (!diagnoses.isNullOrEmpty()) {
                        binding.diagnosisLayout.visibility = View.VISIBLE
                        diagnosisAdapter.submitList(diagnoses)
                    } else {
                        binding.diagnosisLayout.visibility = View.GONE
                    }
                } else {
                    binding.diagnosisLayout.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.diagnosisLayout.visibility = View.GONE
            }
        }
    }

    private fun generateAndSharePdf(diagnosis: Diagnosis) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfFile = PdfGenerator(this@UserFaceScanActivity).generateDiagnosisPdf(diagnosis, currentUserId)
                withContext(Dispatchers.Main) {
                    sharePdf(pdfFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserFaceScanActivity, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sharePdf(pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Diagnosis PDF"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FACE_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            val faceEmbedding = data?.getStringExtra(FaceCaptureActivity.EXTRA_FACE_EMBEDDING)
            if (faceEmbedding != null) {
                verifyFaceAndGetVitals(faceEmbedding)
            } else {
                binding.statusText.text = "No face detected"
            }
        }
    }

    companion object {
        private const val FACE_CAPTURE_REQUEST_CODE = 1001
    }
} 
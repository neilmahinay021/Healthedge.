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
import com.example.healthedge.api.FaceVerificationResponse
import com.example.healthedge.databinding.ActivityDoctorFaceScanBinding
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

class DoctorFaceScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorFaceScanBinding
    private lateinit var faceApi: FaceApi
    private lateinit var diagnosisAdapter: DiagnosisHistoryAdapter
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorFaceScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()
        setupVitalsDisplay()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRetrofit() {
        faceApi = com.example.healthedge.api.ApiClient.faceApi
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
            layoutManager = LinearLayoutManager(this@DoctorFaceScanActivity)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FACE_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            val faceEmbedding = data?.getStringExtra(FaceCaptureActivity.EXTRA_FACE_EMBEDDING)
            if (faceEmbedding != null) {
                verifyFace(faceEmbedding)
            } else {
                Toast.makeText(this, "Face capture failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyFace(faceEmbedding: String) {
        lifecycleScope.launch {
            try {
                val request = FaceVerificationRequest(faceEmbedding)
                val response = faceApi.verifyFace(request)
                if (response.success) {
                    val vitals = response.vitals
                    if (vitals != null) {
                        val userId = (vitals["user_id"] as? Number)?.toInt() ?: vitals["user_id"].toString().replace(".0", "").toInt()
                        val patientName = vitals["name"] as? String ?: "Unknown"
                        currentUserId = userId
                        updateVitalsDisplay(vitals, patientName)
                        fetchAndDisplayDiagnoses(userId)
                        // Log successful face scan
                        logFaceScan(userId, true, "Face verification successful for patient: $patientName")
                    } else {
                        Toast.makeText(this@DoctorFaceScanActivity, "No vitals data available", Toast.LENGTH_SHORT).show()
                        logFaceScan(-1, false, "No vitals data available")
                    }
                } else {
                    Toast.makeText(this@DoctorFaceScanActivity, "Face verification failed", Toast.LENGTH_SHORT).show()
                    // Log failed face scan
                    logFaceScan(-1, false, "Face verification failed")
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorFaceScanActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                // Log error
                logFaceScan(-1, false, "Error: ${e.message}")
            }
        }
    }

    private fun logFaceScan(userId: Int, success: Boolean, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] User ID: $userId, Status: ${if (success) "Success" else "Failed"}, Message: $message\n"
        
        // Write to recent scans
        appendLogToFile("recent_face_scans.txt", logEntry)
        
        // Also write to history
        appendLogToFile("face_scan_history.txt", logEntry)
    }

    private fun appendLogToFile(filename: String, text: String) {
        try {
            val file = File(filesDir, filename)
            file.appendText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateVitalsDisplay(vitals: Map<String, Any>, patientName: String) {
        binding.vitalsLayout.visibility = View.VISIBLE
        binding.patientNameText.text = "Patient: $patientName"
        binding.bloodPressureText.text = "Blood Pressure: ${vitals["blood_pressure"] ?: "N/A"}"
        binding.heartRateText.text = "Heart Rate: ${vitals["heart_rate"] ?: "N/A"}"
        binding.temperatureText.text = "Temperature: ${vitals["temperature"] ?: "N/A"}"
        binding.oxygenText.text = "Oxygen Saturation: ${vitals["oxygen_saturation"] ?: "N/A"}"
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
        if (currentUserId == -1) {
            Toast.makeText(this, "Patient information not available", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfFile = PdfGenerator(this@DoctorFaceScanActivity).generateDiagnosisPdf(diagnosis, currentUserId)
                withContext(Dispatchers.Main) {
                    sharePdf(pdfFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DoctorFaceScanActivity, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Diagnosis PDF"))
    }

    companion object {
        private const val FACE_CAPTURE_REQUEST_CODE = 1001
    }
} 
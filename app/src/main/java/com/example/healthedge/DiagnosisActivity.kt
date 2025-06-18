package com.example.healthedge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthedge.api.ApiClient
import com.example.healthedge.models.Diagnosis
import com.example.healthedge.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DiagnosisActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DiagnosisAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnosis)
        recyclerView = findViewById(R.id.diagnosisRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DiagnosisAdapter { diagnosis ->
            generateAndSharePdf(diagnosis)
        }
        recyclerView.adapter = adapter

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        Log.d("DiagnosisActivity", "Retrieved user_id from prefs: $userId")
        
        if (userId != -1) {
            lifecycleScope.launch {
                try {
                    Log.d("DiagnosisActivity", "Making API call with user_id: $userId")
                    val response = ApiClient.apiService.getUserDiagnoses(userId)
                    if (response.isSuccessful) {
                        val diagnoses = response.body()
                        Log.d("DiagnosisActivity", "API Response: $diagnoses")
                        val latest = diagnoses?.maxByOrNull { it.createdAt ?: "" }
                        Log.d("DiagnosisActivity", "Latest Diagnosis: $latest")
                        if (latest != null) {
                            adapter.submitList(listOf(latest))
                        } else {
                            adapter.submitList(emptyList())
                        }
                    } else {
                        Log.e("DiagnosisActivity", "Failed to load diagnoses: ${response.code()}")
                        Toast.makeText(this@DiagnosisActivity, "Failed to load diagnoses", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("DiagnosisActivity", "Network error: ${e.localizedMessage}", e)
                    Toast.makeText(this@DiagnosisActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAndSharePdf(diagnosis: Diagnosis) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfFile = PdfGenerator(this@DiagnosisActivity).generateDiagnosisPdf(diagnosis, userId)
                withContext(Dispatchers.Main) {
                    sharePdf(pdfFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DiagnosisActivity, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
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
} 
package com.example.healthedge

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.healthedge.api.ApiClient
import com.example.healthedge.databinding.ActivityDoctorIdScanBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class DoctorIdScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorIdScanBinding
    private var currentPhotoPath: String? = null
    private var photoFile: File? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        const val EXTRA_ID_VERIFIED = "id_verified"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorIdScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.captureButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        binding.retakeButton.setOnClickListener {
            binding.previewImage.setImageBitmap(null)
            binding.captureButton.visibility = android.view.View.VISIBLE
            binding.retakeButton.visibility = android.view.View.GONE
            binding.verifyButton.visibility = android.view.View.GONE
        }

        binding.verifyButton.setOnClickListener {
            verifyId()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                photoFile = createImageFile()
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            photoFile?.let { file ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(file))
                binding.previewImage.setImageBitmap(bitmap)
                binding.captureButton.visibility = android.view.View.GONE
                binding.retakeButton.visibility = android.view.View.VISIBLE
                binding.verifyButton.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun verifyId() {
        binding.progressBar.show()
        binding.verifyButton.isEnabled = false

        photoFile?.let { file ->
            // Log file existence and size
            android.util.Log.e("DoctorIdScan", "File path: ${file.absolutePath}, exists: ${file.exists()}, size: ${file.length()}")
            if (!file.exists() || file.length() == 0L) {
                Toast.makeText(this, "Image file is missing or empty!", Toast.LENGTH_SHORT).show()
                binding.verifyButton.isEnabled = true
                binding.progressBar.hide()
                return
            }

            // Set correct MIME type
            val mimeType = if (file.extension.equals("png", ignoreCase = true)) "image/png" else "image/jpeg"
            val requestFile = RequestBody.create(mimeType.toMediaTypeOrNull(), file)
            val body = MultipartBody.Part.createFormData("idImage", file.name, requestFile)

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.verifyDoctorId(body)
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DoctorIdScan", "isSuccessful: ${response.isSuccessful}")
                    android.util.Log.e("DoctorIdScan", "Error body: $errorBody")
                    if (response.isSuccessful && response.body()?.get("success") == true) {
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_ID_VERIFIED, true)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        val errorMessage = response.body()?.get("error") as? String ?: errorBody ?: "Unknown error"
                        Toast.makeText(this@DoctorIdScanActivity, "ID verification failed: $errorMessage", Toast.LENGTH_SHORT).show()
                        binding.verifyButton.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@DoctorIdScanActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.verifyButton.isEnabled = true
                } finally {
                    binding.progressBar.hide()
                }
            }
        } ?: run {
            Toast.makeText(this, "No image file to upload!", Toast.LENGTH_SHORT).show()
            binding.verifyButton.isEnabled = true
            binding.progressBar.hide()
        }
    }
}
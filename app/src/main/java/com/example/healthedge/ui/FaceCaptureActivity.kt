package com.example.healthedge.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.healthedge.databinding.ActivityFaceCaptureBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import com.example.healthedge.utils.FaceEmbeddingService
import android.os.Environment
import java.io.File
import android.graphics.BitmapFactory
import com.example.healthedge.appendLogToFile

class FaceCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceCaptureBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector
    private var imageCapture: ImageCapture? = null
    private lateinit var faceEmbeddingService: FaceEmbeddingService
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private var camera: Camera? = null
    private var isTorchOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.captureButton.isEnabled = false // Disable by default

        // Add camera switch button logic
        binding.switchCameraButton.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            startCamera()
        }

        binding.flashToggleButton.setOnClickListener {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                isTorchOn = !isTorchOn
                camera?.cameraControl?.enableTorch(isTorchOn)
            } else {
                Toast.makeText(this, "Flash is only available on the back camera", Toast.LENGTH_SHORT).show()
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        setupFaceDetector()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceEmbeddingService = FaceEmbeddingService(this)

        binding.captureButton.setOnClickListener {
            captureImage()
        }
    }

    private fun setupFaceDetector() {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(options)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                isTorchOn = false
                binding.captureButton.postDelayed({
                    binding.captureButton.isEnabled = true
                }, 700)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        val photoFile = File.createTempFile("face_capture_", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    processBitmap(bitmap)
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(this@FaceCaptureActivity, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    applicationContext.appendLogToFile("face_registration_debug.txt", "Photo capture failed: ${exception.message}")
                }
            }
        )
    }

    private fun processBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null after file-based capture")
            Toast.makeText(this, "Camera error: Could not capture image", Toast.LENGTH_LONG).show()
            applicationContext.appendLogToFile("face_registration_debug.txt", "Bitmap is null after file-based capture")
            return
        }
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    try {
                        val face = faces[0]
                        val croppedFace = cropFace(bitmap, face.boundingBox)
                        Log.d(TAG, "Cropped face size: ${croppedFace.width}x${croppedFace.height}")
                        applicationContext.appendLogToFile("face_registration_debug.txt", "Cropped face size: ${croppedFace.width}x${croppedFace.height}")
                        val embedding = faceEmbeddingService.getFaceEmbedding(croppedFace)
                        val embeddingJson = JSONArray(embedding.toList()).toString()
                        val resultIntent = Intent()
                        resultIntent.putExtra(EXTRA_FACE_EMBEDDING, embeddingJson)
                        Log.d(TAG, "Face embedding generated, returning result")
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Embedding error: ${e.message}", e)
                        Toast.makeText(this, "Embedding error: ${e.message}", Toast.LENGTH_LONG).show()
                        applicationContext.appendLogToFile("face_registration_debug.txt", "Embedding error: ${e.message}\n${Log.getStackTraceString(e)}")
                    }
                } else {
                    Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show()
                    applicationContext.appendLogToFile("face_registration_debug.txt", "No face detected in image")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Face detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                applicationContext.appendLogToFile("face_registration_debug.txt", "Face detection failed: ${e.message}\n${Log.getStackTraceString(e)}")
            }
    }

    private fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        val x = boundingBox.left.coerceAtLeast(0)
        val y = boundingBox.top.coerceAtLeast(0)
        val width = boundingBox.width().coerceAtMost(bitmap.width - x).coerceAtLeast(1)
        val height = boundingBox.height().coerceAtMost(bitmap.height - y).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "FaceCaptureActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val EXTRA_FACE_EMBEDDING = "face_embedding"
    }
} 
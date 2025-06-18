package com.example.healthedge.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FaceDetectionService {
    private val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        
        FaceDetection.getClient(options)
    }

    suspend fun detectFace(bitmap: Bitmap): Result<com.google.mlkit.vision.face.Face> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val face = detectFaceInImage(image)
            Result.success(face)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun detectFaceInImage(image: InputImage): com.google.mlkit.vision.face.Face {
        return suspendCancellableCoroutine { continuation ->
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        continuation.resume(faces[0])
                    } else {
                        continuation.resumeWithException(Exception("No face detected"))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
} 
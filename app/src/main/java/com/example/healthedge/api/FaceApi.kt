package com.example.healthedge.api

import retrofit2.http.Body
import retrofit2.http.POST

interface FaceApi {
    @POST("face_register")
    suspend fun registerFace(@Body request: FaceRegistrationRequest): FaceRegistrationResponse

    @POST("face_verify")
    suspend fun verifyFace(@Body request: FaceVerificationRequest): FaceVerificationResponse
}

data class FaceRegistrationRequest(
    val userId: String,
    val faceEmbedding: String
)

data class FaceRegistrationResponse(
    val success: Boolean,
    val message: String
)

data class FaceVerificationRequest(
    val faceEmbedding: String
)

data class FaceVerificationResponse(
    val success: Boolean,
    val message: String,
    val vitals: Map<String, Any>? = null
) 
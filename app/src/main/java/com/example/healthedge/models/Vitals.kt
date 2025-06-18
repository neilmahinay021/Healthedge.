package com.example.healthedge.models

import com.google.gson.annotations.SerializedName

data class Vitals(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("blood_pressure") val bloodPressure: String?,
    @SerializedName("heart_rate") val heartRate: String?,
    @SerializedName("temperature") val temperature: String?,
    @SerializedName("weight") val weight: String?,
    @SerializedName("height") val height: String?,
    @SerializedName("blood_oxygen") val bloodOxygen: String?,
    @SerializedName("respiration_rate") val respirationRate: String?,
    @SerializedName("qr_code_image") val qrCodeImage: String?
) 
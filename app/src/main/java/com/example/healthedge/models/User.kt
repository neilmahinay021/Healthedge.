package com.example.healthedge.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String? = null,
    @SerializedName("blood_pressure") val bloodPressure: String? = null,
    @SerializedName("heart_rate") val heartRate: String? = null,
    @SerializedName("temperature") val temperature: String? = null,
    @SerializedName("weight") val weight: String? = null,
    @SerializedName("height") val height: String? = null,
    @SerializedName("blood_oxygen") val bloodOxygen: String? = null,
    @SerializedName("respiration_rate") val respirationRate: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("contact_no") val contactNo: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("qr_code_path") val qrCodePath: String? = null
) 
package com.example.healthedge.models

import java.util.Date
import com.google.gson.annotations.SerializedName

data class Diagnosis(
    val id: Int,
    @SerializedName("patient_code") val patientCode: Int,
    val address: String?,
    val weight: Double?,
    val height: Double?,
    @SerializedName("blood_pressure") val bloodPressure: String?,
    @SerializedName("referred_by") val referredBy: String?,
    val diagnosis: String?,
    @SerializedName("medicine_name") val medicineName: String?,
    val dosage: String?,
    val duration: String?,
    @SerializedName("advice_given") val adviceGiven: String?,
    val signature: String?,
    @SerializedName("next_visit") val nextVisit: String?,
    @SerializedName("created_at") val createdAt: String?
) 
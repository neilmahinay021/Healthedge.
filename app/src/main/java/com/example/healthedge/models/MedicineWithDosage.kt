package com.example.healthedge.models

data class MedicineWithDosage(
    val id: Int,
    val name: String,
    val genericName: String,
    val dosage: String,
    val frequency: String,
    val duration: String
) 
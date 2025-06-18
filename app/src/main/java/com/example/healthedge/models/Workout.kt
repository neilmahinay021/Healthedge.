package com.example.healthedge.models

data class Workout(
    val id: Int,
    val diagnosisId: Int?,
    val name: String,
    val description: String,
    val duration: String?,
    val intensity: String?
) 
package com.example.healthedge.models

data class DiseaseWorkout(
    val workout_name: String,
    val description: String,
    val duration: String,
    val intensity: String,
    val gif_url: String,
    var progress: Int = 0 // Progress percentage (0-100)
) 
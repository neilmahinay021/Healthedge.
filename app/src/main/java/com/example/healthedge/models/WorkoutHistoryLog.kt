package com.example.healthedge.models

data class WorkoutHistoryLog(
    val id: Int? = null,
    val user_id: Int,
    val date: String, // YYYY-MM-DD
    val workout_type: String,
    val duration_minutes: Int,
    val steps: Int,
    val calories: Int?,
    val created_at: String? = null
) 
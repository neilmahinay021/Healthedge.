package com.example.healthedge.models
 
data class UserWorkoutsResponse(
    val has_diagnoses: Boolean,
    val message: String? = null,
    val workouts_by_category: Map<String, Map<String, List<DiseaseWorkout>>>? = null
) 
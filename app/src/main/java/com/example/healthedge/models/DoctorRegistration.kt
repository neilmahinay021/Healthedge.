package com.example.healthedge.models

data class DoctorRegistration(
    val name: String,
    val email: String,
    val password: String,
    val specialization: String,
    val idVerified: Boolean = false
) 
package com.example.healthedge.models

data class UserRegistration(
    val name: String,
    val email: String,
    val password: String,
    val age: Int,
    val gender: String,
    val contact_no: String,
    val address: String
) 
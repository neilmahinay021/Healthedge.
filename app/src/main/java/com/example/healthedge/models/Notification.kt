package com.example.healthedge.models

data class Notification(
    val id: Int,
    val user_id: Int,
    val message: String,
    val is_read: Int,
    val created_at: String
) 
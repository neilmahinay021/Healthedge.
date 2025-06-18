package com.example.healthedge.models

import com.google.gson.annotations.SerializedName

// The user field can be null if login fails
// The error field can be null if login succeeds

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: User?,
    @SerializedName("error") val error: String?
) 
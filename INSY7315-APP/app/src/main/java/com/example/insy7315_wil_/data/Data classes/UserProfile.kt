package com.example.insy7315_wil_.data.`Data classes`

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String = "member",
    val remindersEnabled: Boolean = true,
    val createdAtMillis: Long? = null,
)

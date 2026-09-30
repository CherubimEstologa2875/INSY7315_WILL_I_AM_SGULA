package com.example.insy7315_wil_.data.`Data classes`

data class AudioSession(
    val sessionId: String = "",
    val userId: String,
    val audioId: String,
    val completed: Boolean = false,
    val durationSeconds: Int = 0,
    val pointsAwarded: Boolean = false,
)

package com.example.insy7315_wil_.data.`Data classes`

data class SucculentState(
    val userId: String,
    val totalPoints: Int = 0,
    val stage: String = "seed",
    val lastActivityAtMillis: Long? = null,
    val wilted: Boolean = false,
)

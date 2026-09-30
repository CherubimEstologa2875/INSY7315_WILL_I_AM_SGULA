package com.example.insy7315_wil_.data.`Data classes`

data class MoodEntry(
    val entryId: String = "",
    val userId: String,
    val moodLevel: Int,
    val note: String = "",
    val createdAtMillis: Long? = null,
)

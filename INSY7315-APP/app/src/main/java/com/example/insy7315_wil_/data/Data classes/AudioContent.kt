package com.example.insy7315_wil_.data.`Data classes`

data class AudioContent(
    val audioId: String = "",
    val title: String,
    val description: String = "",
    val category: String = "",
    val storagePath: String = "",
    val durationSeconds: Int = 0,
    val active: Boolean = true,
)

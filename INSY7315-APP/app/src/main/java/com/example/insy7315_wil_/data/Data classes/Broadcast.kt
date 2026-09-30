package com.example.insy7315_wil_.data.`Data classes`

data class Broadcast(
    val broadcastId: String = "",
    val title: String,
    val message: String,
    val publishedAtMillis: Long? = null,
)

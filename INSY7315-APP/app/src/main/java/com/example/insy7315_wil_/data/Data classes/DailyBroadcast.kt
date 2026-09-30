package com.example.insy7315_wil_.data.`Data classes`

/** Document written by the scheduled daily affirmation function. */
data class DailyBroadcast(
    val broadcastId: String = "current",
    val affirmationId: String = "",
    val text: String,
    val dateDisplayed: String = "",
)

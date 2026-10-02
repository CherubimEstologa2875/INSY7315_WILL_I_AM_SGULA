package com.example.insy7315_wil_.data.`Data classes`

/**
 * One broadcast: a short recording the therapist makes herself.
 *
 * publishedAtMillis is when it should reach clients, not when it was uploaded, so a broadcast
 * can sit in the future until its time comes around.
 */
data class Broadcast(
    val broadcastId: String = "",
    val title: String,
    val message: String,
    val publishedAtMillis: Long? = null,
    val storagePath: String = "",
    val durationSeconds: Int = 0,
    /** Who recorded it. Defaults to the admin who uploaded it, but the form allows any name. */
    val author: String = "",
)

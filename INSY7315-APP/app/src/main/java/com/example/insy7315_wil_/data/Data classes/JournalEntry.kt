package com.example.insy7315_wil_.data.`Data classes`

data class JournalEntry(
    val entryId: String = "",
    val userId: String,
    val content: String,
    val prompt: String = "",
    val createdAtMillis: Long? = null,
)

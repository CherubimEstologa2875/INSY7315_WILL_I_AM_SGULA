package com.example.insy7315_wil_.data.`Data classes`

data class Affirmation(
    val affirmationId: String = "",
    val text: String,
    val category: String = "motivation",
    val dateDisplayed: String = "",
) {
    companion object {
        // Admins sometimes type their own quote marks, so any are stripped before the card adds
        // its own pair, otherwise the home screen shows ""Small steps still count.""
        fun forDisplay(text: String?): String? {
            val cleaned = text.orEmpty().trim().trim('"', '\u201C', '\u201D').trim()
            return if (cleaned.isEmpty()) null else "\"$cleaned\""
        }
    }
}

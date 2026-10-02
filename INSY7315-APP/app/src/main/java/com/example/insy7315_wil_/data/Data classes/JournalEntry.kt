package com.example.insy7315_wil_.data.`Data classes`

data class JournalEntry(
    val entryId: String = "",
    val userId: String,
    val content: String,
    val prompt: String = "",
    val createdAtMillis: Long? = null,
) {
    val wordCount: Int
        get() = JournalMarkup.plainText(content).trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    companion object {
        /** What the onJournalEntryCreated function adds for each new entry (Firebase, n.d.). */
        const val POINTS_PER_ENTRY = 10
    }
}

/* Reference List
Firebase, n.d. Cloud Firestore triggers. [online] Available at: <https://firebase.google.com/docs/functions/firestore-events> [Accessed 21 September 2026].
*/

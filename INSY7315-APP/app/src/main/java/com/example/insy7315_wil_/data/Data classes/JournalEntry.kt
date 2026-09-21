package com.example.insy7315_wil_.data.`Data classes`

data class JournalEntry(
    val entryId: String = "",
    val userId: String,
    val content: String,
    val prompt: String = "",
    val createdAtMillis: Long? = null,
) {
    val wordCount: Int get() = content.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    companion object {
        /** Points the onJournalEntryCreated Cloud Function awards for each new entry (IIE, 2026; Firebase, n.d.). */
        const val POINTS_PER_ENTRY = 10
    }
}

/* Reference List
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Firebase, n.d.. Cloud Firestore triggers. [online] Available at: <https://firebase.google.com/docs/functions/firestore-events> [Accessed 21 September 2026].
*/

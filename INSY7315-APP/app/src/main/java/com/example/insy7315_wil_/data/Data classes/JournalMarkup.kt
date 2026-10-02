package com.example.insy7315_wil_.data.`Data classes`

import android.text.Spanned
import androidx.core.text.HtmlCompat

/** Converts the journal's rich text to a safe, compact HTML representation for Firestore. */
object JournalMarkup {
    fun fromStored(content: String): Spanned =
        HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)

    fun toStored(content: Spanned): String =
        HtmlCompat.toHtml(content, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

    /**
     * The words on their own, with the markup taken out. This does the stripping in plain Kotlin
     * rather than through HtmlCompat, because word counts are also worked out in unit tests, where
     * there is no Android framework to call (Android Developers, 2026).
     */
    fun plainText(content: String): String = content
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</(p|div|li|h[1-6])>"), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
}

/* Reference List
Android Developers, 2026. Local tests. [online] Available at: <https://developer.android.com/training/testing/local-tests> [Accessed 2 October 2026].
*/

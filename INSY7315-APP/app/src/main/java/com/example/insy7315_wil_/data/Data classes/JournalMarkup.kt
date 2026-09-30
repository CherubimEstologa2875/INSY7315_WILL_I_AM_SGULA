package com.example.insy7315_wil_.data.`Data classes`

import android.text.Spanned
import androidx.core.text.HtmlCompat

/** Converts the journal's rich text to a safe, compact HTML representation for Firestore. */
object JournalMarkup {
    fun fromStored(content: String): Spanned =
        HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)

    fun toStored(content: Spanned): String =
        HtmlCompat.toHtml(content, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

    fun plainText(content: String): String = fromStored(content).toString()
}

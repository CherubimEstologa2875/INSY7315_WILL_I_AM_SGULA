package com.example.insy7315_wil_.ui.screens.audio

import com.example.insy7315_wil_.data.`Data classes`.Broadcast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Decides whether a broadcast has gone live yet, and how its date should read on screen.
 * No Android types in here, so the tests can cover it.
 */
// Dates read in whatever locale the member's phone is set to (Oracle, 2026).
object BroadcastSchedule {
    private val dayFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
    private val dayTimeFormat = SimpleDateFormat("EEE d MMM 'at' HH:mm", Locale.getDefault())

    /** A broadcast with no date is treated as published, so older records still show. */
    fun isPublished(broadcast: Broadcast, nowMillis: Long = System.currentTimeMillis()): Boolean =
        (broadcast.publishedAtMillis ?: 0L) <= nowMillis

    fun published(broadcasts: List<Broadcast>, nowMillis: Long = System.currentTimeMillis()): List<Broadcast> =
        broadcasts.filter { isPublished(it, nowMillis) }
            .sortedByDescending { it.publishedAtMillis ?: 0L }

    /** Scheduled recordings, soonest first, so the therapist sees what goes out next. */
    fun upcoming(broadcasts: List<Broadcast>, nowMillis: Long = System.currentTimeMillis()): List<Broadcast> =
        broadcasts.filterNot { isPublished(it, nowMillis) }
            .sortedBy { it.publishedAtMillis ?: 0L }

    fun latestPublished(broadcasts: List<Broadcast>, nowMillis: Long = System.currentTimeMillis()): Broadcast? =
        published(broadcasts, nowMillis).firstOrNull()

    fun nextUpcoming(broadcasts: List<Broadcast>, nowMillis: Long = System.currentTimeMillis()): Broadcast? =
        upcoming(broadcasts, nowMillis).firstOrNull()

    /** "4 October" once live, "Coming Fri 3 Oct at 07:00" while it is still scheduled. */
    fun dateLabel(broadcast: Broadcast, nowMillis: Long = System.currentTimeMillis()): String {
        val millis = broadcast.publishedAtMillis ?: return ""
        return if (isPublished(broadcast, nowMillis)) {
            dayFormat.format(Date(millis))
        } else {
            "Coming ${dayTimeFormat.format(Date(millis))}"
        }
    }
}

/* Reference List
Oracle, 2026. SimpleDateFormat (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html> [Accessed 2 October 2026].
*/

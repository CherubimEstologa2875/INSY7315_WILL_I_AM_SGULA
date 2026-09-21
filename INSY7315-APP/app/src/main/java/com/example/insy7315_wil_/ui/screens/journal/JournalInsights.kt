package com.example.insy7315_wil_.ui.screens.journal

import com.example.insy7315_wil_.data.`Data classes`.JournalEntry
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Pure calculations behind the journal history screen, kept separate so they can be unit tested (IIE, 2026). */
object JournalInsights {
    data class MonthSummary(val entries: Int, val words: Int, val longestStreak: Int)

    // Converts the stored epoch millis to a local calendar day (Oracle, n.d.).
    fun dateOf(entry: JournalEntry, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        entry.createdAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun inMonth(entries: List<JournalEntry>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()) =
        entries.filter { entry -> dateOf(entry, zone)?.let { YearMonth.from(it) == month } == true }

    fun summary(entries: List<JournalEntry>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): MonthSummary {
        val monthEntries = inMonth(entries, month, zone)
        return MonthSummary(
            entries = monthEntries.size,
            words = monthEntries.sumOf { it.wordCount },
            longestStreak = longestStreak(monthEntries.mapNotNull { dateOf(it, zone) }.toSet()),
        )
    }

    /** Longest run of consecutive days that each have at least one entry. */
    fun longestStreak(days: Set<LocalDate>): Int {
        var longest = 0
        for (day in days) {
            if (day.minusDays(1) in days) continue // only count from the start of a run
            var length = 1
            while (day.plusDays(length.toLong()) in days) length++
            longest = maxOf(longest, length)
        }
        return longest
    }
}

/* Reference List
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Oracle, n.d.. YearMonth (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/YearMonth.html> [Accessed 21 September 2026].
*/

package com.example.insy7315_wil_.ui.screens.journal

import com.example.insy7315_wil_.data.`Data classes`.JournalEntry
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The sums behind the journal history screen. Kept out of the fragment so the tests can check them. */
object JournalInsights {
    data class MonthSummary(val entries: Int, val words: Int, val longestStreak: Int)

    // Turns the stored milliseconds into a calendar day in the member's time zone (Oracle, 2026).
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
Oracle, 2026. YearMonth (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/YearMonth.html> [Accessed 21 September 2026].
*/

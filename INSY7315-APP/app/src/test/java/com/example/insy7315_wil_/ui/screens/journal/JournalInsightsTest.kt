package com.example.insy7315_wil_.ui.screens.journal

import com.example.insy7315_wil_.data.`Data classes`.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

// Local JVM unit tests for the journal stats and prompt logic (JUnit, n.d.).
class JournalInsightsTest {
    private val zone = ZoneOffset.UTC

    private fun entry(date: String, content: String = "one two three") = JournalEntry(
        entryId = date,
        userId = "member",
        content = content,
        createdAtMillis = LocalDate.parse(date).atTime(9, 0).toInstant(zone).toEpochMilli(),
    )

    @Test
    fun summaryOnlyCountsTheSelectedMonth() {
        val entries = listOf(entry("2026-09-01"), entry("2026-09-02", "a b"), entry("2026-08-31"))
        val summary = JournalInsights.summary(entries, YearMonth.of(2026, 9), zone)
        assertEquals(2, summary.entries)
        assertEquals(5, summary.words)
        assertEquals(2, summary.longestStreak)
    }

    @Test
    fun longestStreakFindsTheLongestRunOfDays() {
        val days = listOf("2026-09-01", "2026-09-02", "2026-09-04", "2026-09-05", "2026-09-06")
            .map(LocalDate::parse).toSet()
        assertEquals(3, JournalInsights.longestStreak(days))
        assertEquals(0, JournalInsights.longestStreak(emptySet()))
    }

    @Test
    fun wordCountIgnoresExtraWhitespace() {
        assertEquals(4, entry("2026-09-01", "  calm   after\nlunch today ").wordCount)
        assertEquals(0, entry("2026-09-01", "   ").wordCount)
    }

    @Test
    fun promptIsStableForADayAndCyclesOnRequest() {
        val prompts = JournalPrompts(listOf("a", "b", "c"))
        val day = LocalDate.of(2026, 9, 21)
        assertEquals(prompts.forDate(day), prompts.forDate(day))
        assertNotEquals(prompts.forDate(day), prompts.forDate(day.plusDays(1)))
        assertEquals(0, prompts.nextIndex(2))
    }
}

/* Reference List
JUnit, n.d.. JUnit 4 Getting Started. [online] Available at: <https://github.com/junit-team/junit4/wiki/Getting-started> [Accessed 21 September 2026].
Oracle, n.d.. YearMonth (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/YearMonth.html> [Accessed 21 September 2026].
*/

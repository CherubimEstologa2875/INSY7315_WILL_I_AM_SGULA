package com.example.insy7315_wil_.ui.screens.journal

import java.time.LocalDate

/**
 * Picks the daily journal prompt. Everyone sees the same prompt on the same day, and the
 * member can ask for another one if it doesn't fit how the day went.
 */
// floorMod keeps the index positive for any date, so the same day always maps to the same prompt (Oracle, n.d.).
class JournalPrompts(private val prompts: List<String>) {
    init {
        require(prompts.isNotEmpty()) { "At least one journal prompt is required." }
    }

    fun indexFor(date: LocalDate): Int = Math.floorMod(date.toEpochDay(), prompts.size.toLong()).toInt()

    fun forDate(date: LocalDate): String = prompts[indexFor(date)]

    fun nextIndex(currentIndex: Int): Int = (currentIndex + 1) % prompts.size

    operator fun get(index: Int): String = prompts[Math.floorMod(index, prompts.size)]
}

/* Reference List
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Oracle, n.d.. Math (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Math.html> [Accessed 21 September 2026].
*/

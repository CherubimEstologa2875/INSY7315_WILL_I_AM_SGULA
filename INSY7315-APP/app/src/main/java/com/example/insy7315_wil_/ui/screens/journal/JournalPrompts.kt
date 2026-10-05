package com.example.insy7315_wil_.ui.screens.journal

import java.time.LocalDate

/**
 * Chooses the prompt of the day. Everyone gets the same one on a given day, and a member who
 * doesn't connect with it can ask for another.
 */
// floorMod keeps the index positive for any date, so a given day always lands on the same prompt (Oracle, 2026).
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
Oracle, 2026. Math (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Math.html> [Accessed 21 September 2026].
*/

package com.example.insy7315_wil_.data.`Data classes`

import java.time.Instant
import java.time.ZoneId

/**
 * Stage thresholds and the inactivity rule, as documented in docs/database-design.md and
 * matched against stageForPoints and the seven day cutoff in functions/src/index.ts. HomeFragment
 * and SucculentFragment both read these instead of carrying their own copy of the numbers.
 */
object SucculentStages {
    data class Progress(val stageIndex: Int, val nextStage: String, val start: Int, val end: Int)

    fun progressFor(stage: String): Progress = when (stage) {
        "sprout" -> Progress(1, "Growing", 100, 300)
        "growing" -> Progress(2, "Blooming", 300, 600)
        "blooming" -> Progress(3, "maximum growth", 600, 600)
        else -> Progress(0, "Sprout", 0, 100)
    }

    fun isWilted(wilted: Boolean, lastActivityAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val inactiveForSevenDays = lastActivityAtMillis?.let { nowMillis - it >= 7L * 24 * 60 * 60 * 1000 } ?: false
        return wilted || inactiveForSevenDays
    }

    fun currentStreak(streakCount: Int, lastActivityAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Int {
        val lastActivity = lastActivityAtMillis ?: return 0
        val zone = ZoneId.of("Africa/Johannesburg")
        val lastDay = Instant.ofEpochMilli(lastActivity).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return if (lastDay >= today.minusDays(1)) streakCount else 0
    }
}

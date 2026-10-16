package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// WIL-163: the stage ranges and the seven day wilt window have to match docs/database-design.md
// and stageForPoints/the cutoff in functions/src/index.ts
class SucculentStagesTest {

    @Test
    fun seedEndsAtNinetyNine() {
        val progress = SucculentStages.progressFor("seed")
        assertEquals(0, progress.start)
        assertEquals(100, progress.end)
        assertEquals(0, progress.stageIndex)
    }

    @Test
    fun sproutRunsFromOneHundredToTwoNinetyNine() {
        val progress = SucculentStages.progressFor("sprout")
        assertEquals(100, progress.start)
        assertEquals(300, progress.end)
        assertEquals(1, progress.stageIndex)
    }

    @Test
    fun growingRunsFromThreeHundredToFiveNinetyNine() {
        val progress = SucculentStages.progressFor("growing")
        assertEquals(300, progress.start)
        assertEquals(600, progress.end)
        assertEquals(2, progress.stageIndex)
    }

    @Test
    fun bloomingStartsAtSixHundred() {
        val progress = SucculentStages.progressFor("blooming")
        assertEquals(600, progress.start)
        assertEquals(600, progress.end)
        assertEquals(3, progress.stageIndex)
    }

    @Test
    fun notYetSevenDaysIsNotWilted() {
        val lastActivity = 0L
        val sixDaysLater = 6L * 24 * 60 * 60 * 1000
        assertFalse(SucculentStages.isWilted(wilted = false, lastActivityAtMillis = lastActivity, nowMillis = sixDaysLater))
    }

    @Test
    fun exactlySevenDaysIsWilted() {
        val lastActivity = 0L
        val sevenDaysLater = 7L * 24 * 60 * 60 * 1000
        assertTrue(SucculentStages.isWilted(wilted = false, lastActivityAtMillis = lastActivity, nowMillis = sevenDaysLater))
    }

    @Test
    fun serverWiltedFlagWinsEvenIfTheClientClockDisagrees() {
        assertTrue(SucculentStages.isWilted(wilted = true, lastActivityAtMillis = System.currentTimeMillis()))
    }
}

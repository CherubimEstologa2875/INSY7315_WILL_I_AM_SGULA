package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertEquals
import org.junit.Test

// WIL-167: the points shown on screen must match pointsByReason in functions/src/index.ts
class SucculentPointsTest {

    @Test
    fun quizPointsMatchTheServer() {
        assertEquals(15, SucculentPoints.QUIZ)
    }

    @Test
    fun audioPointsMatchTheServer() {
        assertEquals(20, SucculentPoints.AUDIO)
    }
}

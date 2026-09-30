package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertTrue
import org.junit.Test

class MoodFunctionalityTest {
    @Test
    fun acceptsMoodLevelsOneToFiveWithAnOptionalNote() {
        assertTrue(WellnessValidation.mood(1, "") is ValidationResult.Valid)
        assertTrue(WellnessValidation.mood(3, "Feeling okay today") is ValidationResult.Valid)
        assertTrue(WellnessValidation.mood(5, "Great day") is ValidationResult.Valid)
    }

    @Test
    fun rejectsMoodLevelsOutsideTheScale() {
        assertTrue(WellnessValidation.mood(0, "note") is ValidationResult.Invalid)
        assertTrue(WellnessValidation.mood(6, "note") is ValidationResult.Invalid)
    }

    @Test
    fun rejectsMoodNotesLongerThanFiveHundredCharacters() {
        assertTrue(WellnessValidation.mood(4, "a".repeat(501)) is ValidationResult.Invalid)
    }
}

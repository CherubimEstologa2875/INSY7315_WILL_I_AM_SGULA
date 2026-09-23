package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertTrue
import org.junit.Test

class WellnessValidationTest {

    @Test
    fun acceptsARealTitleAndASensibleDuration() {
        val result = WellnessValidation.audio("Managing Anxiety", 750)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun acceptsTheDurationBoundaries() {
        assertTrue(WellnessValidation.audio("Track", 1) is ValidationResult.Valid)
        assertTrue(WellnessValidation.audio("Track", 86_400) is ValidationResult.Valid)
    }

    @Test
    fun rejectsABlankTitle() {
        val result = WellnessValidation.audio("", 750)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun rejectsAZeroOrNegativeDuration() {
        assertTrue(WellnessValidation.audio("Track", 0) is ValidationResult.Invalid)
        assertTrue(WellnessValidation.audio("Track", -5) is ValidationResult.Invalid)
    }

    @Test
    fun rejectsADurationOverADay() {
        val result = WellnessValidation.audio("Track", 86_401)
        assertTrue(result is ValidationResult.Invalid)
    }
}

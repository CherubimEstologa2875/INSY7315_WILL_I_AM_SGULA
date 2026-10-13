package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertTrue
import org.junit.Test

class WellnessValidationTest {

    @Test
    fun acceptsAnAffirmationUpToTheLimit() {
        assertTrue(WellnessValidation.affirmation("Small steps still count.") is ValidationResult.Valid)
        assertTrue(WellnessValidation.affirmation("a".repeat(WellnessValidation.AFFIRMATION_MAX_LENGTH)) is ValidationResult.Valid)
    }

    @Test
    fun rejectsABlankOrTooLongAffirmation() {
        assertTrue(WellnessValidation.affirmation("   ") is ValidationResult.Invalid)
        assertTrue(WellnessValidation.affirmation("a".repeat(WellnessValidation.AFFIRMATION_MAX_LENGTH + 1)) is ValidationResult.Invalid)
    }

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

package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertTrue
import org.junit.Test

class JournalFunctionalityTest {
    @Test
    fun acceptsAReflectionWithAnOptionalPrompt() {
        assertTrue(WellnessValidation.journal("I handled a difficult conversation calmly.", "What went well?")
            is ValidationResult.Valid)
    }

    @Test
    fun rejectsBlankJournalContent() {
        assertTrue(WellnessValidation.journal("   ", "Prompt") is ValidationResult.Invalid)
    }

    @Test
    fun enforcesJournalContentAndPromptLimits() {
        assertTrue(WellnessValidation.journal("a".repeat(10_001), "Prompt") is ValidationResult.Invalid)
        assertTrue(WellnessValidation.journal("Reflection", "p".repeat(501)) is ValidationResult.Invalid)
        assertTrue(WellnessValidation.journal("a".repeat(10_000), "p".repeat(500)) is ValidationResult.Valid)
    }
}

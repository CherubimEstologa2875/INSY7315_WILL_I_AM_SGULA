package com.example.insy7315_wil_.data.`Data classes`

/** Result returned by the shared wellness input validators. */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

object WellnessValidation {
    fun mood(moodLevel: Int, note: String): ValidationResult = when {
        moodLevel !in 1..5 -> ValidationResult.Invalid("Mood must be between 1 and 5.")
        note.length > 500 -> ValidationResult.Invalid("Mood note cannot exceed 500 characters.")
        else -> ValidationResult.Valid
    }

    fun journal(content: String, prompt: String): ValidationResult = when {
        content.isBlank() -> ValidationResult.Invalid("Journal content cannot be empty.")
        content.length > 10_000 -> ValidationResult.Invalid("Journal content cannot exceed 10,000 characters.")
        prompt.length > 500 -> ValidationResult.Invalid("Journal prompt cannot exceed 500 characters.")
        else -> ValidationResult.Valid
    }

    fun audio(title: String, durationSeconds: Int): ValidationResult = when {
        title.isBlank() -> ValidationResult.Invalid("Audio title cannot be empty.")
        durationSeconds !in 1..86_400 -> ValidationResult.Invalid("Audio duration is invalid.")
        else -> ValidationResult.Valid
    }

    fun quizAnswers(answers: Map<String, String>): ValidationResult = when {
        answers.isEmpty() -> ValidationResult.Invalid("At least one quiz answer is required.")
        answers.any { it.key.isBlank() || it.value.isBlank() } ->
            ValidationResult.Invalid("Quiz question IDs and answers cannot be empty.")
        else -> ValidationResult.Valid
    }
}

package com.example.insy7315_wil_.data.`Data classes`

import android.util.Patterns

// validation for the sign in, register and reset screens, same ValidationResult as WellnessValidation
object AuthValidation {

    const val MAX_NAME_LENGTH = 60
    const val MAX_EMAIL_LENGTH = 254
    const val MIN_PASSWORD_LENGTH = 8

    // trim and strip markup and control characters so only plain text gets saved
    fun sanitise(value: String, maxLength: Int): String = value
        .trim()
        .filter { !it.isISOControl() }
        .replace(TAGS, "")
        .replace(REPEATED_SPACES, " ")
        .take(maxLength)

    fun displayName(name: String): ValidationResult = when {
        name.isBlank() -> ValidationResult.Invalid("Enter your name.")
        name.length < 2 -> ValidationResult.Invalid("Enter your full name.")
        name.length > MAX_NAME_LENGTH -> ValidationResult.Invalid("Keep your name under $MAX_NAME_LENGTH characters.")
        !name.all { it.isLetter() || it.isWhitespace() || it == '-' || it == '\'' } ->
            ValidationResult.Invalid("Names can only use letters, spaces, hyphens and apostrophes.")
        else -> ValidationResult.Valid
    }

    fun email(email: String): ValidationResult = when {
        email.isBlank() -> ValidationResult.Invalid("Enter your email address.")
        email.length > MAX_EMAIL_LENGTH -> ValidationResult.Invalid("That email address is too long.")
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult.Invalid("Enter a valid email address.")
        else -> ValidationResult.Valid
    }

    fun newPassword(password: String): ValidationResult = when {
        password.isBlank() -> ValidationResult.Invalid("Enter a password.")
        password.length < MIN_PASSWORD_LENGTH -> ValidationResult.Invalid("Use at least $MIN_PASSWORD_LENGTH characters.")
        password.none(Char::isDigit) -> ValidationResult.Invalid("Include at least one number.")
        password.none(Char::isLetter) -> ValidationResult.Invalid("Include at least one letter.")
        else -> ValidationResult.Valid
    }

    // sign in only checks something was typed, firebase decides if it is correct
    fun existingPassword(password: String): ValidationResult =
        if (password.isBlank()) ValidationResult.Invalid("Enter your password.") else ValidationResult.Valid

    private val TAGS = Regex("<[^>]*>")
    private val REPEATED_SPACES = Regex(" {2,}")
}

// message to show on a field, or null when the value is fine
internal fun ValidationResult.errorMessage(): String? = (this as? ValidationResult.Invalid)?.message

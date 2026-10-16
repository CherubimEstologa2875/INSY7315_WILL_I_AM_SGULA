package com.example.insy7315_wil_.ui.screens.auth

import com.example.insy7315_wil_.data.`Data classes`.AuthValidation
import com.example.insy7315_wil_.data.`Data classes`.errorMessage

// pulled out of RegisterFragment's text watcher so the strength bands can be unit tested
internal object PasswordStrength {

    enum class Band { WEAK, MEDIUM, STRONG }

    fun bandFor(password: String): Band = when {
        password.length >= 12 && password.any(Char::isDigit) && password.any(Char::isLetter) -> Band.STRONG
        AuthValidation.newPassword(password).errorMessage() == null -> Band.MEDIUM
        else -> Band.WEAK
    }
}

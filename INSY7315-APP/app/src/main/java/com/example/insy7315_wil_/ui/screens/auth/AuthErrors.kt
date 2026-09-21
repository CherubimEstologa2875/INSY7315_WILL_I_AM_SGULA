package com.example.insy7315_wil_.ui.screens.auth

import com.google.firebase.auth.FirebaseAuthException

// turns firebase auth errors into messages a member can actually read
internal object AuthErrors {

    fun signIn(error: Throwable?): String = when (codeOf(error)) {
        "ERROR_USER_DISABLED" -> "This account has been deactivated. Please contact the practice."
        "ERROR_TOO_MANY_REQUESTS" -> TOO_MANY_ATTEMPTS
        // same message either way so this cannot be used to find registered emails
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_EMAIL" ->
            "That email address or password is incorrect."
        else -> GENERIC
    }

    fun register(error: Throwable?): String = when (codeOf(error)) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "That email address already has an account."
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
        "ERROR_WEAK_PASSWORD" -> "Choose a stronger password."
        "ERROR_TOO_MANY_REQUESTS" -> TOO_MANY_ATTEMPTS
        else -> GENERIC
    }

    fun passwordReset(error: Throwable?): String =
        if (codeOf(error) == "ERROR_TOO_MANY_REQUESTS") TOO_MANY_ATTEMPTS else GENERIC

    // an unknown address counts as sent so the reset screen cannot be used to find accounts
    fun resetCanBeTreatedAsSent(error: Throwable?): Boolean = codeOf(error) == "ERROR_USER_NOT_FOUND"

    private fun codeOf(error: Throwable?) = (error as? FirebaseAuthException)?.errorCode

    private const val GENERIC = "Something went wrong. Check your connection and try again."
    private const val TOO_MANY_ATTEMPTS = "Too many attempts. Wait a few minutes and try again."
}

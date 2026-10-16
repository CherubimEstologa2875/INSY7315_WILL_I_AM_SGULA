package com.example.insy7315_wil_.ui.screens.auth

import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorsTest {
    @Test
    fun signInDoesNotRevealWhetherAnEmailExists() {
        val notFound = FirebaseAuthException("ERROR_USER_NOT_FOUND", "missing")
        val wrongPassword = FirebaseAuthException("ERROR_WRONG_PASSWORD", "wrong")
        val invalidCredential = FirebaseAuthException("ERROR_INVALID_CREDENTIAL", "bad")
        val invalidEmail = FirebaseAuthException("ERROR_INVALID_EMAIL", "bad email")
        assertEquals(AuthErrors.signIn(notFound), AuthErrors.signIn(wrongPassword))
        assertEquals(AuthErrors.signIn(notFound), AuthErrors.signIn(invalidCredential))
        assertEquals(AuthErrors.signIn(notFound), AuthErrors.signIn(invalidEmail))
    }

    @Test
    fun signInReportsADeactivatedAccountByName() {
        val disabled = FirebaseAuthException("ERROR_USER_DISABLED", "disabled")
        assertEquals(AuthErrors.DEACTIVATED, AuthErrors.signIn(disabled))
    }

    @Test
    fun signInRateLimitsSeparatelyFromTheGenericCase() {
        val tooMany = FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "slow down")
        assertTrue(AuthErrors.signIn(tooMany).contains("Too many attempts"))
        assertNotEquals(AuthErrors.signIn(tooMany), AuthErrors.signIn(null))
    }

    @Test
    fun unrecognisedSignInErrorsFallBackToTheGenericMessage() {
        assertTrue(AuthErrors.signIn(null).contains("Something went wrong"))
        assertTrue(AuthErrors.signIn(RuntimeException("not a firebase error")).contains("Something went wrong"))
    }

    @Test
    fun mapsCommonRegistrationAndRateLimitErrors() {
        assertEquals(
            "That email address already has an account.",
            AuthErrors.register(FirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE", "duplicate")),
        )
        assertEquals(
            "Enter a valid email address.",
            AuthErrors.register(FirebaseAuthException("ERROR_INVALID_EMAIL", "bad email")),
        )
        assertEquals(
            "Choose a stronger password.",
            AuthErrors.register(FirebaseAuthException("ERROR_WEAK_PASSWORD", "weak")),
        )
        assertTrue(AuthErrors.register(FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "slow down"))
            .contains("Too many attempts"))
    }

    @Test
    fun unrecognisedRegistrationErrorsFallBackToTheGenericMessage() {
        assertTrue(AuthErrors.register(null).contains("Something went wrong"))
    }

    @Test
    fun passwordResetRateLimitsLikeEverythingElse() {
        val tooMany = FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "slow down")
        assertTrue(AuthErrors.passwordReset(tooMany).contains("Too many attempts"))
        assertTrue(AuthErrors.passwordReset(null).contains("Something went wrong"))
    }

    @Test
    fun unknownPasswordResetUsersCanBeTreatedAsSent() {
        assertTrue(AuthErrors.resetCanBeTreatedAsSent(FirebaseAuthException("ERROR_USER_NOT_FOUND", "missing")))
        assertFalse(AuthErrors.resetCanBeTreatedAsSent(null))
    }
}

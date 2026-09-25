package com.example.insy7315_wil_.ui.screens.auth

import com.google.firebase.auth.FirebaseAuthException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorsTest {
    @Test
    fun signInDoesNotRevealWhetherAnEmailExists() {
        val notFound = FirebaseAuthException("ERROR_USER_NOT_FOUND", "missing")
        val wrongPassword = FirebaseAuthException("ERROR_WRONG_PASSWORD", "wrong")
        assertEquals(AuthErrors.signIn(notFound), AuthErrors.signIn(wrongPassword))
    }

    @Test
    fun mapsCommonRegistrationAndRateLimitErrors() {
        assertEquals(
            "That email address already has an account.",
            AuthErrors.register(FirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE", "duplicate")),
        )
        assertTrue(AuthErrors.register(FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "slow down"))
            .contains("Too many attempts"))
    }

    @Test
    fun unknownPasswordResetUsersCanBeTreatedAsSent() {
        assertTrue(AuthErrors.resetCanBeTreatedAsSent(FirebaseAuthException("ERROR_USER_NOT_FOUND", "missing")))
        assertFalse(AuthErrors.resetCanBeTreatedAsSent(null))
    }
}

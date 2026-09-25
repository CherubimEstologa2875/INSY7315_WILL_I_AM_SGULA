package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun sanitiseTrimsMarkupControlCharactersAndRepeatedSpaces() {
        assertEquals("A User", AuthValidation.sanitise("  <b>A\u0000  User</b>  ", 60))
    }

    @Test
    fun acceptsValidRegistrationDetails() {
        assertTrue(AuthValidation.displayName("Alex Morgan") is ValidationResult.Valid)
        assertTrue(AuthValidation.email("alex@example.com") is ValidationResult.Valid)
        assertTrue(AuthValidation.newPassword("Secure123") is ValidationResult.Valid)
    }

    @Test
    fun rejectsInvalidRegistrationDetails() {
        assertTrue(AuthValidation.displayName("") is ValidationResult.Invalid)
        assertTrue(AuthValidation.displayName("A1ex") is ValidationResult.Invalid)
        assertTrue(AuthValidation.email("not-an-email") is ValidationResult.Invalid)
        assertTrue(AuthValidation.newPassword("short") is ValidationResult.Invalid)
        assertTrue(AuthValidation.newPassword("password") is ValidationResult.Invalid)
        assertTrue(AuthValidation.newPassword("12345678") is ValidationResult.Invalid)
    }

    @Test
    fun signInPasswordOnlyRequiresInput() {
        assertTrue(AuthValidation.existingPassword("") is ValidationResult.Invalid)
        assertTrue(AuthValidation.existingPassword("anything") is ValidationResult.Valid)
    }
}

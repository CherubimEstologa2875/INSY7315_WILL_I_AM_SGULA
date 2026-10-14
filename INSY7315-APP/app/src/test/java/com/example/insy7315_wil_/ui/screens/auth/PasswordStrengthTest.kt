package com.example.insy7315_wil_.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {

    @Test
    fun blankIsWeak() {
        assertEquals(PasswordStrength.Band.WEAK, PasswordStrength.bandFor(""))
    }

    @Test
    fun tooShortIsWeak() {
        assertEquals(PasswordStrength.Band.WEAK, PasswordStrength.bandFor("abc1"))
    }

    @Test
    fun longButMissingADigitIsWeak() {
        assertEquals(PasswordStrength.Band.WEAK, PasswordStrength.bandFor("noDigitsHereAtAll"))
    }

    @Test
    fun longButMissingALetterIsWeak() {
        assertEquals(PasswordStrength.Band.WEAK, PasswordStrength.bandFor("123456789012"))
    }

    @Test
    fun eightOrMoreWithALetterAndDigitIsMedium() {
        assertEquals(PasswordStrength.Band.MEDIUM, PasswordStrength.bandFor("password1"))
    }

    @Test
    fun twelveOrMoreWithALetterAndDigitIsStrong() {
        assertEquals(PasswordStrength.Band.STRONG, PasswordStrength.bandFor("password1234"))
    }
}

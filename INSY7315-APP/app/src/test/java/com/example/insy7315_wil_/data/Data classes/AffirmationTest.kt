package com.example.insy7315_wil_.data.`Data classes`

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// WIL-168: how an affirmation is shown on the home card
class AffirmationTest {

    @Test
    fun wrapsTheTextInOnePairOfQuotes() {
        assertEquals("\"Small steps still count.\"", Affirmation.forDisplay("Small steps still count."))
    }

    @Test
    fun doesNotDoubleUpQuotesTheAdminTyped() {
        assertEquals("\"I am enough.\"", Affirmation.forDisplay("\"I am enough.\""))
        assertEquals("\"I am enough.\"", Affirmation.forDisplay("  \u201CI am enough.\u201D  "))
    }

    @Test
    fun blankTextShowsNothing() {
        assertNull(Affirmation.forDisplay(null))
        assertNull(Affirmation.forDisplay("   "))
        assertNull(Affirmation.forDisplay("\"\""))
    }
}

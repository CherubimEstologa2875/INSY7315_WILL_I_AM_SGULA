package com.example.insy7315_wil_.ui.screens.journal

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import org.junit.Assert.assertTrue
import org.junit.Test

// WIL-173: mood and journal both route their save failures through here, so this is
// what members actually see when they are offline or when a save needs retrying
class JournalErrorsTest {

    @Test
    fun offlineOrUnreachableAsksToCheckTheConnection() {
        val unavailable = FirebaseFirestoreException("offline", Code.UNAVAILABLE)
        val timedOut = FirebaseFirestoreException("slow network", Code.DEADLINE_EXCEEDED)
        assertTrue(JournalErrors.message(unavailable, "save your entry").contains("internet connection"))
        assertTrue(JournalErrors.message(timedOut, "save your entry").contains("internet connection"))
    }

    @Test
    fun expiredSessionAsksToSignInAgain() {
        val permissionDenied = FirebaseFirestoreException("denied", Code.PERMISSION_DENIED)
        val unauthenticated = FirebaseFirestoreException("signed out", Code.UNAUTHENTICATED)
        assertTrue(JournalErrors.message(permissionDenied, "save your entry").contains("sign in again"))
        assertTrue(JournalErrors.message(unauthenticated, "save your entry").contains("sign in again"))
    }

    @Test
    fun failedPreconditionMeansTheJournalIsStillBeingSetUp() {
        val notReady = FirebaseFirestoreException("not ready", Code.FAILED_PRECONDITION)
        assertTrue(JournalErrors.message(notReady, "save your entry").contains("being set up"))
    }

    @Test
    fun aValidationExceptionShowsItsOwnMessageInsteadOfAGenericOne() {
        val validation = IllegalStateException("Entries are limited to 10000 characters.")
        assertTrue(JournalErrors.message(validation, "save your entry").contains("10000 characters"))
    }

    @Test
    fun anythingElseFallsBackToTheGenericRetryMessage() {
        assertTrue(JournalErrors.message(null, "save your entry").contains("Please try again"))
        assertTrue(JournalErrors.message(RuntimeException("boom"), "save your entry").contains("Please try again"))
    }
}

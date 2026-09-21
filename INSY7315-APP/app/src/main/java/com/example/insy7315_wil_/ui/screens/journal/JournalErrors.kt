package com.example.insy7315_wil_.ui.screens.journal

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code

/** Turns Firestore failures into calm, member-facing messages, keyed on the Firestore error code (Firebase, n.d.). */
internal object JournalErrors {
    const val SIGNED_OUT = "Please sign in again to use your private journal."

    fun message(error: Throwable?, action: String): String {
        val code = (error as? FirebaseFirestoreException)?.code
        return when (code) {
            Code.PERMISSION_DENIED, Code.UNAUTHENTICATED ->
                "We couldn't $action because your session has expired. Please sign in again."
            Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED ->
                "We couldn't $action. Check your internet connection and try again."
            Code.FAILED_PRECONDITION ->
                "Your journal is still being set up. Please try again in a few minutes."
            else -> when (error) {
                is IllegalStateException, is IllegalArgumentException -> error.message ?: "We couldn't $action."
                else -> "Something went wrong and we couldn't $action. Please try again."
            }
        }
    }
}

/* Reference List
Firebase, n.d.. FirebaseFirestoreException.Code. [online] Available at: <https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestoreException.Code> [Accessed 21 September 2026].
*/

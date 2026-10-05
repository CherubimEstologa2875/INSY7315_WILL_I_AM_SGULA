package com.example.insy7315_wil_.ui.screens.journal

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.storage.StorageException

/**
 * Turns a Firebase failure into something calm a member can read, based on the error code the
 * service reports (Firebase, n.d.). The original exception is logged as well, so whoever is
 * debugging sees what actually went wrong and not only the gentle wording (Android Developers, n.d.).
 */
internal object JournalErrors {
    private const val TAG = "SgulaErrors"
    const val SIGNED_OUT = "Please sign in again to use your private journal."

    fun message(error: Throwable?, action: String): String {
        if (error != null) Log.w(TAG, "Could not $action", error)

        (error as? StorageException)?.let { return storageMessage(it, action) }

        return when ((error as? FirebaseFirestoreException)?.code) {
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

    /** Uploads and downloads come back through Cloud Storage, which has its own set of codes (Firebase, n.d.). */
    private fun storageMessage(error: StorageException, action: String): String = when (error.errorCode) {
        StorageException.ERROR_NOT_AUTHENTICATED ->
            "We couldn't $action because you are signed out. Please sign in again."
        StorageException.ERROR_NOT_AUTHORIZED ->
            "We couldn't $action: this account isn't allowed to upload audio. Check the Storage rules in Firebase."
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED, StorageException.ERROR_CANCELED ->
            "We couldn't $action. Check your internet connection and try again."
        StorageException.ERROR_QUOTA_EXCEEDED ->
            "We couldn't $action because the practice's storage is full."
        StorageException.ERROR_BUCKET_NOT_FOUND, StorageException.ERROR_PROJECT_NOT_FOUND ->
            "We couldn't $action because file storage isn't set up for this app yet."
        StorageException.ERROR_OBJECT_NOT_FOUND ->
            "We couldn't $action because that recording is no longer there."
        else -> "Something went wrong and we couldn't $action. Please try again."
    }
}

/* Reference List
Android Developers, n.d. Log. [online] Available at: <https://developer.android.com/reference/android/util/Log> [Accessed 2 October 2026].
Firebase, n.d. FirebaseFirestoreException.Code. [online] Available at: <https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestoreException.Code> [Accessed 21 September 2026].
Firebase, n.d. StorageException. [online] Available at: <https://firebase.google.com/docs/reference/android/com/google/firebase/storage/StorageException> [Accessed 2 October 2026].
*/

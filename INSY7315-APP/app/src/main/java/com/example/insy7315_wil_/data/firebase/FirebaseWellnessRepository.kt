package com.example.insy7315_wil_.data.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import java.util.Date

/** Firebase data-access boundary used by the app screens and future view models. */
class FirebaseWellnessRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun register(email: String, password: String): Task<AuthResult> =
        auth.createUserWithEmailAndPassword(email, password)

    fun signIn(email: String, password: String): Task<AuthResult> =
        auth.signInWithEmailAndPassword(email, password)

    fun sendPasswordReset(email: String): Task<Void> = auth.sendPasswordResetEmail(email)

    fun signOut() = auth.signOut()

    fun currentUserId(): String? = auth.currentUser?.uid

    fun saveUserProfile(profile: UserProfile): Task<Void> =
        firestore.collection("UserProfile").document(profile.userId).set(
            mapOf(
                "userId" to profile.userId,
                "email" to profile.email,
                "displayName" to profile.displayName,
                "role" to profile.role,
                "remindersEnabled" to profile.remindersEnabled,
                "createdAt" to (profile.createdAtMillis?.let(::Date) ?: Date()),
            ),
            SetOptions.merge(),
        )

    fun addMoodEntry(entry: MoodEntry): Task<DocumentReference> {
        requireValid(WellnessValidation.mood(entry.moodLevel, entry.note))
        return firestore.collection("MoodEntry").add(
            mapOf(
                "userId" to requireUserId(entry.userId),
                "moodLevel" to entry.moodLevel,
                "note" to entry.note,
                "createdAt" to Date(),
            ),
        )
    }

    fun addJournalEntry(entry: JournalEntry): Task<DocumentReference> {
        requireValid(WellnessValidation.journal(entry.content, entry.prompt))
        return firestore.collection("JournalEntry").add(
            mapOf(
                "userId" to requireUserId(entry.userId),
                "content" to entry.content,
                "prompt" to entry.prompt,
                "createdAt" to Date(),
            ),
        )
    }

    fun observeMoodEntries(userId: String): Task<QuerySnapshot> =
        firestore.collection("MoodEntry").whereEqualTo("userId", requireUserId(userId)).get()

    fun observeJournalEntries(userId: String): Task<QuerySnapshot> =
        firestore.collection("JournalEntry").whereEqualTo("userId", requireUserId(userId)).get()

    fun saveAudioSession(session: AudioSession): Task<Void> =
        firestore.collection("AudioSession").document(session.sessionId.ifBlank { firestore.collection("AudioSession").document().id })
            .set(
                mapOf(
                    "userId" to requireUserId(session.userId),
                    "audioId" to session.audioId,
                    "completed" to session.completed,
                    "durationSeconds" to session.durationSeconds,
                    "updatedAt" to Date(),
                ),
                SetOptions.merge(),
            )

    fun saveQuizResult(result: QuizResult): Task<DocumentReference> {
        requireValid(WellnessValidation.quizAnswers(result.scores.mapValues { it.value.toString() }))
        return firestore.collection("quizResults").add(
            mapOf(
                "userId" to requireUserId(result.userId),
                "quizId" to result.quizId,
                "recommendedCategoryId" to result.recommendedCategoryId,
                "scores" to result.scores,
                "createdAt" to Date(),
            ),
        )
    }

    private fun requireUserId(userId: String): String {
        check(userId.isNotBlank()) { "A signed-in user ID is required." }
        check(auth.currentUser?.uid == userId) { "The signed-in user must own this record." }
        return userId
    }

    private fun requireValid(result: ValidationResult) {
        if (result is ValidationResult.Invalid) error(result.message)
    }
}

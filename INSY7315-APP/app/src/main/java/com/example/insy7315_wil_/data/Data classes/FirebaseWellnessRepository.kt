package com.example.insy7315_wil_.data.`Data classes`

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import java.util.Date

/** Firebase data-access boundary used by the app screens and future view models. */
class FirebaseWellnessRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeSucculentState(userId: String, onState: (SucculentState) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration =
        firestore.collection(FirestoreCollections.SUCCULENT_STATE).document(requireUserId(userId)).addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            val data = snapshot?.data ?: emptyMap()
            onState(SucculentState(
                userId = userId,
                totalPoints = (data["totalPoints"] as? Number)?.toInt() ?: 0,
                stage = data["stage"] as? String ?: "seed",
                lastActivityAtMillis = (data["lastActivityAt"] as? Timestamp)?.toDate()?.time,
                wilted = data["wilted"] as? Boolean ?: false,
            ))
        }
    fun register(email: String, password: String): Task<AuthResult> =
        auth.createUserWithEmailAndPassword(email, password)

    fun signIn(email: String, password: String): Task<AuthResult> =
        auth.signInWithEmailAndPassword(email, password)

    fun sendPasswordReset(email: String): Task<Void> = auth.sendPasswordResetEmail(email)

    fun signOut() = auth.signOut()

    fun currentUserId(): String? = auth.currentUser?.uid

    fun saveUserProfile(profile: UserProfile): Task<Void> =
        firestore.collection(FirestoreCollections.USER_PROFILE).document(profile.userId).set(
            mapOf(
                "userId" to profile.userId,
                "email" to profile.email,
                "displayName" to profile.displayName,
                "role" to profile.role,
                "remindersEnabled" to profile.remindersEnabled,
                "sharesAnonymousInsights" to profile.sharesAnonymousInsights,
                "activityProgressVisible" to profile.activityProgressVisible,
                "createdAt" to (profile.createdAtMillis?.let(::Date) ?: Date()),
            ),
            SetOptions.merge(),
        )

    fun loadUserProfile(userId: String): Task<DocumentSnapshot> =
        firestore.collection(FirestoreCollections.USER_PROFILE).document(requireUserId(userId)).get()

    // only writes the three switches, the role is left out so a save cannot change it
    fun updateUserSettings(
        userId: String,
        remindersEnabled: Boolean,
        sharesAnonymousInsights: Boolean,
        activityProgressVisible: Boolean,
    ): Task<Void> =
        firestore.collection(FirestoreCollections.USER_PROFILE).document(requireUserId(userId)).set(
            mapOf(
                "remindersEnabled" to remindersEnabled,
                "sharesAnonymousInsights" to sharesAnonymousInsights,
                "activityProgressVisible" to activityProgressVisible,
            ),
            SetOptions.merge(),
        )

    fun toUserProfile(document: DocumentSnapshot): UserProfile = UserProfile(
        userId = document.getString("userId").orEmpty().ifBlank { document.id },
        email = document.getString("email").orEmpty(),
        displayName = document.getString("displayName").orEmpty(),
        role = document.getString("role") ?: UserProfile.ROLE_MEMBER,
        remindersEnabled = document.getBoolean("remindersEnabled") ?: true,
        sharesAnonymousInsights = document.getBoolean("sharesAnonymousInsights") ?: true,
        activityProgressVisible = document.getBoolean("activityProgressVisible") ?: true,
        createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time,
    )

    fun addMoodEntry(entry: MoodEntry): Task<DocumentReference> {
        requireValid(WellnessValidation.mood(entry.moodLevel, entry.note))
        return firestore.collection(FirestoreCollections.MOOD_ENTRY).add(
            mapOf(
                "userId" to requireUserId(entry.userId),
                "moodLevel" to entry.moodLevel,
                "note" to entry.note,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    /**
     * Saves a private journal entry. The owner's uid is stamped on the document and both
     * timestamps come from the Firestore server clock, so the rules in firestore.rules can
     * verify them (Firebase, n.d.). The +10 succulent points are awarded by the onJournalEntryCreated
     * function (Firebase, n.d.).
     */
    fun addJournalEntry(entry: JournalEntry): Task<DocumentReference> {
        requireValid(WellnessValidation.journal(entry.content, entry.prompt))
        return firestore.collection(FirestoreCollections.JOURNAL_ENTRY).add(
            mapOf(
                "userId" to requireUserId(entry.userId),
                "content" to entry.content.trim(),
                "prompt" to entry.prompt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    /**
     * Live list of the signed-in user's own journal entries, newest first. The query is
     * constrained to userId == auth.uid, which is what the security rules require for reads.
     * Needs the JournalEntry (userId ASC, createdAt DESC) index in firestore.indexes.json
     * (Firebase, n.d.).
     */
    fun listenToJournalEntries(
        userId: String,
        onEntries: (List<JournalEntry>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration =
        firestore.collection(FirestoreCollections.JOURNAL_ENTRY)
            .whereEqualTo("userId", requireUserId(userId))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                onEntries(snapshot?.documents.orEmpty().map(::toJournalEntry))
            }

    private fun toJournalEntry(document: DocumentSnapshot): JournalEntry {
        // ESTIMATE gives a local time for entries still waiting on the server timestamp (Firebase, n.d.).
        val behavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return JournalEntry(
            entryId = document.id,
            userId = document.getString("userId").orEmpty(),
            content = document.getString("content").orEmpty(),
            prompt = document.getString("prompt").orEmpty(),
            createdAtMillis = document.getTimestamp("createdAt", behavior)?.toDate()?.time,
        )
    }

    // Sorted here instead of with orderBy so the query doesn't need a composite index
    fun listenToMoodEntries(
        userId: String,
        onEntries: (List<MoodEntry>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration =
        firestore.collection(FirestoreCollections.MOOD_ENTRY)
            .whereEqualTo("userId", requireUserId(userId))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { onError(error); return@addSnapshotListener }
                val entries = snapshot?.documents.orEmpty().map(::toMoodEntry)
                onEntries(entries.sortedByDescending { it.createdAtMillis ?: Long.MAX_VALUE })
            }

    private fun toMoodEntry(document: DocumentSnapshot): MoodEntry {
        val behavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return MoodEntry(
            entryId = document.id,
            userId = document.getString("userId").orEmpty(),
            moodLevel = document.getLong("moodLevel")?.toInt() ?: 3,
            note = document.getString("note").orEmpty(),
            createdAtMillis = document.getTimestamp("createdAt", behavior)?.toDate()?.time,
        )
    }

    fun observeMoodEntries(userId: String): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.MOOD_ENTRY).whereEqualTo("userId", requireUserId(userId)).get()

    fun observeJournalEntries(userId: String): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.JOURNAL_ENTRY).whereEqualTo("userId", requireUserId(userId)).get()

    fun saveAudioSession(session: AudioSession): Task<Void> =
        firestore.collection(FirestoreCollections.AUDIO_SESSION).document(session.sessionId.ifBlank { firestore.collection(FirestoreCollections.AUDIO_SESSION).document().id })
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
        return firestore.collection(FirestoreCollections.QUIZ_RESULTS).add(
            mapOf(
                "userId" to requireUserId(result.userId),
                "quizId" to result.quizId,
                "recommendedCategoryId" to result.recommendedCategoryId,
                "scores" to result.scores,
                "createdAt" to Date(),
            ),
        )
    }

    fun loadAudioContent(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.AUDIO_CONTENT)
            .whereEqualTo("active", true)
            .get()

    fun loadBroadcasts(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.BROADCAST).get()

    fun loadRelaxationTracks(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.RELAXATION_TRACKS).get()

    fun loadMeditationCategories(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.MEDITATION_CATEGORY).get()

    fun loadQuizzes(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.QUIZZES).get()

    fun loadQuizQuestions(quizId: String): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.QUIZ_QUESTIONS)
            .whereEqualTo("quizId", quizId)
            .get()

    private fun requireUserId(userId: String): String {
        check(userId.isNotBlank()) { "A signed-in user ID is required." }
        check(auth.currentUser?.uid == userId) { "The signed-in user must own this record." }
        return userId
    }

    private fun requireValid(result: ValidationResult) {
        if (result is ValidationResult.Invalid) error(result.message)
    }
}

/* Reference List
Firebase, n.d.. Add data to Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/manage-data/add-data> [Accessed 21 September 2026].
Firebase, n.d.. Cloud Firestore triggers. [online] Available at: <https://firebase.google.com/docs/functions/firestore-events> [Accessed 21 September 2026].
Firebase, n.d.. Get realtime updates with Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/listen> [Accessed 21 September 2026].
Firebase, n.d.. Order and limit data with Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/order-limit-data> [Accessed 21 September 2026].
Firebase, n.d.. Manage indexes in Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/indexing> [Accessed 21 September 2026].
Firebase, n.d.. Securely query data. [online] Available at: <https://firebase.google.com/docs/firestore/security/rules-query> [Accessed 21 September 2026].
Firebase, n.d.. DocumentSnapshot.ServerTimestampBehavior. [online] Available at: <https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/DocumentSnapshot.ServerTimestampBehavior> [Accessed 21 September 2026].
*/

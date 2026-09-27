package com.example.insy7315_wil_.data.`Data classes`

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
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
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions


/** Firebase data-access boundary used by the app screens and future view models. */
class FirebaseWellnessRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("africa-south1"),
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

    fun checkAdmin(
        onResult: (Boolean) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(false)
            return
        }

        firestore.collection(FirestoreCollections.ADMIN)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.exists())
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

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
                "active" to profile.active,
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
        active = document.getBoolean("active") ?: true,
        createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time,
    )

    fun loadAllUsers(
        onSuccess: (List<UserProfile>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        firestore.collection(FirestoreCollections.USER_PROFILE)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.map { document ->
                    toUserProfile(document)
                }

                onSuccess(users)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun setUserActive(
        userId: String,
        active: Boolean,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        firestore.collection(FirestoreCollections.USER_PROFILE)
            .document(userId)
            .update("active", active)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }


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

    fun saveAudioSession(session: AudioSession): Task<Void> {
        requireValid(WellnessValidation.audio(session.audioId, session.durationSeconds))
        return firestore.collection(FirestoreCollections.AUDIO_SESSION).document(session.sessionId.ifBlank { firestore.collection(FirestoreCollections.AUDIO_SESSION).document().id })
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
    }

    // The function scores the answers and saves the quizResults document itself
    fun calculateQuizRecommendation(quizId: String, answers: Map<String, Int>): Task<QuizRecommendation> {
        requireValid(WellnessValidation.quizAnswers(answers.mapValues { it.value.toString() }))
        return functions.getHttpsCallable("calculateQuizRecommendation")
            .call(mapOf("quizId" to quizId, "answers" to answers))
            .onSuccessTask { result -> Tasks.forResult(toQuizRecommendation(result.getData() as? Map<*, *>)) }
    }

    private fun toQuizRecommendation(data: Map<*, *>?): QuizRecommendation {
        val audio = data?.get("audio") as? Map<*, *>
        return QuizRecommendation(
            resultId = data?.get("resultId") as? String ?: "",
            categoryId = data?.get("categoryId") as? String ?: "",
            categoryName = data?.get("categoryName") as? String ?: "",
            categoryDescription = data?.get("categoryDescription") as? String ?: "",
            audio = audio?.let {
                AudioContent(
                    audioId = it["audioId"] as? String ?: "",
                    title = it["title"] as? String ?: "",
                    category = it["category"] as? String ?: "",
                    storagePath = it["storagePath"] as? String ?: "",
                    downloadUrl = it["downloadUrl"] as? String ?: "",
                    durationSeconds = (it["durationSeconds"] as? Number)?.toInt() ?: 0,
                )
            },
        )
    }

    fun uploadAudioContent(
        uri: Uri,
        title: String,
        category: String,
        categoryId: String,
        broadcastDate: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val audioRef = firestore
            .collection(FirestoreCollections.AUDIO_CONTENT)
            .document()

        val audioId = audioRef.id

        val storagePath = "audio/$audioId"

        val storageRef = storage.reference.child(storagePath)

        storageRef.putFile(uri)
            .continueWithTask { uploadTask ->
                if (!uploadTask.isSuccessful) {
                    throw uploadTask.exception
                        ?: Exception("Audio upload failed")
                }

                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->

                val audio = mapOf(
                    "audioId" to audioId,
                    "title" to title,
                    "description" to "",
                    "category" to category,
                    "categoryId" to categoryId,
                    "broadcastDate" to broadcastDate,
                    "storagePath" to storagePath,
                    "downloadUrl" to downloadUri.toString(),
                    "durationSeconds" to 0,
                    "active" to true

                )

                audioRef.set(audio)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error)
                    }
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun deleteAudioContent(
        audioId: String,
        storagePath: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val firestoreRef = firestore
            .collection(FirestoreCollections.AUDIO_CONTENT)
            .document(audioId)

        val storageRef = storage.reference.child(storagePath)

        // Delete the Storage file first.
        storageRef.delete()
            .addOnSuccessListener {

                // Then delete the Firestore metadata.
                firestoreRef.delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error)
                    }
            }
            .addOnFailureListener { storageError ->

                // If the Storage file is already gone, still remove
                // the Firestore document.
                firestoreRef.delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { firestoreError ->
                        onError(firestoreError)
                    }
            }
    }
    fun getAudioDownloadUrl(
        storagePath: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        storage.reference
            .child(storagePath)
            .downloadUrl
            .addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun createBroadcast(
        title: String,
        message: String,
        publishedAtMillis: Long?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref =
            firestore.collection(FirestoreCollections.BROADCAST)
                .document()

        val data = mapOf(
            "broadcastId" to ref.id,
            "title" to title,
            "message" to message,
            "publishedAtMillis" to publishedAtMillis
        )

        ref.set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun addAffirmation(
        text: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref =
            firestore.collection(FirestoreCollections.AFFIRMATION)
                .document()

        val data = mapOf(
            "affirmationId" to ref.id,
            "text" to text,
            "category" to "motivation"
        )

        ref.set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
    fun loadAudioSessions(
        userId: String,
        onSuccess: (Int) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        firestore
            .collection(FirestoreCollections.AUDIO_SESSION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.size())
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
    fun loadAudioContent(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.AUDIO_CONTENT)
            .whereEqualTo("active", true)
            .get()

    fun toAudioContent(document: DocumentSnapshot): AudioContent = AudioContent(
        audioId = document.id,
        title = document.getString("title").orEmpty(),
        description = document.getString("description").orEmpty(),
        category = document.getString("category").orEmpty(),
        categoryId = document.getString("categoryId").orEmpty(),
        storagePath = document.getString("storagePath").orEmpty(),
        downloadUrl = document.getString("downloadUrl").orEmpty(),
        durationSeconds = document.getLong("durationSeconds")?.toInt() ?: 0,
        active = document.getBoolean("active") ?: true,
    )

    // newest first matches how the history screen lists past broadcasts
    // field is publishedAt, not publishedAtMillis with FirestoreCollectionSchemas in mind
    fun loadBroadcasts(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.BROADCAST)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .get()

    fun toBroadcast(document: DocumentSnapshot): Broadcast = Broadcast(
        broadcastId = document.id,
        title = document.getString("title").orEmpty(),
        message = document.getString("message").orEmpty(),
        publishedAtMillis = document.getTimestamp("publishedAt")?.toDate()?.time,
    )

    // Written every night by the refreshDailyAffirmation function
    fun loadDailyAffirmation(): Task<DocumentSnapshot> =
        firestore.collection(FirestoreCollections.DAILY_BROADCAST).document("current").get()

    fun loadRelaxationTracks(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.RELAXATION_TRACKS).get()

    fun loadMeditationCategories(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.MEDITATION_CATEGORY).get()

    fun toMeditationCategory(document: DocumentSnapshot): MeditationCategory = MeditationCategory(
        categoryId = document.id,
        name = document.getString("name").orEmpty().ifBlank { document.id },
        description = document.getString("description").orEmpty(),
        recommendedFor = document.getString("recommendedFor").orEmpty(),
    )

    fun loadQuizzes(): Task<QuerySnapshot> =
        firestore.collection(FirestoreCollections.QUIZZES).get()

    fun loadQuizQuestions(
        quizId: String,
        onQuestions: (List<QuizQuestion>) -> Unit,
        onError: (Exception) -> Unit = {},
    ) {
        firestore.collection(FirestoreCollections.QUIZ_QUESTIONS)
            .whereEqualTo("quizId", quizId)
            .get()
            .addOnSuccessListener { snapshot ->
                // Skips half-typed questions from the console
                val questions = snapshot.documents.map(::toQuizQuestion)
                    .filter { it.options.isNotEmpty() && it.options.size == it.categoryIds.size }
                onQuestions(questions.sortedBy { it.order })
            }
            .addOnFailureListener(onError)
    }

    private fun toQuizQuestion(document: DocumentSnapshot): QuizQuestion = QuizQuestion(
        questionId = document.id,
        quizId = document.getString("quizId").orEmpty(),
        questionText = document.getString("questionText").orEmpty(),
        options = (document.get("options") as? List<*>).orEmpty().map { it.toString() },
        categoryIds = (document.get("categoryIds") as? List<*>).orEmpty().map { it.toString() },
        weight = document.getLong("weight")?.toInt() ?: 1,
        order = document.getLong("order")?.toInt() ?: 0,
    )

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

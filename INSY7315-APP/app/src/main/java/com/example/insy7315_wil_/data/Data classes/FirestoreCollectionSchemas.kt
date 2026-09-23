package com.example.insy7315_wil_.data.`Data classes`

/** Field reference for the documents used by the Android data-access layer. */
data class FirestoreCollectionSchema(
    val collection: String,
    val documentIdExample: String,
    val fields: List<String>,
)

object FirestoreCollectionSchemas {
    val all = listOf(
        FirestoreCollectionSchema(FirestoreCollections.ADMIN, "{authUid}", listOf("name", "role")),
        FirestoreCollectionSchema(FirestoreCollections.AFFIRMATION, "{affirmationId}", listOf("text", "category", "dateDisplayed")),
        FirestoreCollectionSchema(FirestoreCollections.AUDIO_CONTENT, "{audioId}", listOf("title", "description", "category", "categoryId", "storagePath", "durationSeconds", "active")),
        FirestoreCollectionSchema(FirestoreCollections.AUDIO_SESSION, "{sessionId}", listOf("userId", "audioId", "completed", "durationSeconds", "pointsAwarded")),
        FirestoreCollectionSchema(FirestoreCollections.BROADCAST, "{broadcastId}", listOf("title", "message", "publishedAt")),
        FirestoreCollectionSchema(FirestoreCollections.JOURNAL_ENTRY, "{entryId}", listOf("userId", "content", "prompt", "createdAt", "updatedAt")),
        FirestoreCollectionSchema(FirestoreCollections.MEDITATION_CATEGORY, "{categoryId}", listOf("name", "description", "recommendedFor")),
        FirestoreCollectionSchema(FirestoreCollections.MOOD_ENTRY, "{entryId}", listOf("userId", "moodLevel", "note", "createdAt")),
        FirestoreCollectionSchema(FirestoreCollections.QUIZ, "{quizId}", listOf("title", "description", "active")),
        FirestoreCollectionSchema(FirestoreCollections.QUIZ_QUESTIONS, "{questionId}", listOf("quizId", "questionText", "options", "categoryIds", "weight", "order")),
        FirestoreCollectionSchema(FirestoreCollections.QUIZ_RESULTS, "{resultId}", listOf("userId", "quizId", "answers", "scores", "recommendedCategoryId", "recommendedAudioId", "createdAt")),
        FirestoreCollectionSchema(FirestoreCollections.QUIZZES, "{quizId}", listOf("title", "description", "active")),
        FirestoreCollectionSchema(FirestoreCollections.RELAXATION_TRACKS, "{trackId}", listOf("title", "categoryId", "storagePath", "durationSeconds")),
        FirestoreCollectionSchema(FirestoreCollections.SUCCULENT_STATE, "{userId}", listOf("userId", "totalPoints", "stage", "lastActivityAt")),
        FirestoreCollectionSchema(FirestoreCollections.USER_PROFILE, "{userId}", listOf("userId", "email", "displayName", "role", "remindersEnabled", "createdAt")),
    )
}

package com.example.insy7315_wil_.data.`Data classes`

/**
 * case-sensitive Firestore collection names for sgula-task2-insy7315.
 * Keep these names aligned with the live Firestore database.
 */
object FirestoreCollections {
    const val ADMIN = "Admin"
    const val AFFIRMATION = "Affirmation"
    const val AUDIO_CONTENT = "AudioContent"
    const val AUDIO_SESSION = "AudioSession"
    const val BROADCAST = "Broadcast"
    const val DAILY_BROADCAST = "DailyBroadcast"
    const val JOURNAL_ENTRY = "JournalEntry"
    const val MEDITATION_CATEGORY = "MeditationCategory"
    const val MOOD_ENTRY = "MoodEntry"
    const val QUIZ = "Quiz"
    const val QUIZ_QUESTIONS = "quizQuestions"
    const val QUIZ_RESULTS = "quizResults"
    const val QUIZZES = "quizzes"
    const val RELAXATION_TRACKS = "relaxationTracks"
    const val SUCCULENT_STATE = "SucculentState"
    const val USER_PROFILE = "UserProfile"

    /** Collections visible in the live Firestore root. */
    val liveCollections = listOf(
        ADMIN,
        AFFIRMATION,
        AUDIO_CONTENT,
        AUDIO_SESSION,
        BROADCAST,
        JOURNAL_ENTRY,
        MEDITATION_CATEGORY,
        MOOD_ENTRY,
        QUIZ,
        SUCCULENT_STATE,
        USER_PROFILE,
        QUIZ_QUESTIONS,
        QUIZ_RESULTS,
        QUIZZES,
        RELAXATION_TRACKS,
    )
}

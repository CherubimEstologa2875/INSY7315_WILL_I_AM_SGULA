package com.example.insy7315_wil_.data.firebase

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String = "member",
    val remindersEnabled: Boolean = true,
    val createdAtMillis: Long? = null,
)

data class MoodEntry(
    val entryId: String = "",
    val userId: String,
    val moodLevel: Int,
    val note: String = "",
    val createdAtMillis: Long? = null,
)

data class JournalEntry(
    val entryId: String = "",
    val userId: String,
    val content: String,
    val prompt: String = "",
    val createdAtMillis: Long? = null,
)

data class SucculentState(
    val userId: String,
    val totalPoints: Int = 0,
    val stage: String = "seed",
    val lastActivityAtMillis: Long? = null,
)

data class AudioContent(
    val audioId: String = "",
    val title: String,
    val description: String = "",
    val category: String = "",
    val storagePath: String = "",
    val durationSeconds: Int = 0,
    val active: Boolean = true,
)

data class DailyBroadcast(
    val broadcastId: String = "",
    val title: String,
    val message: String,
    val publishedAtMillis: Long? = null,
)

data class RelaxationTrack(
    val trackId: String = "",
    val title: String,
    val categoryId: String = "",
    val storagePath: String = "",
    val durationSeconds: Int = 0,
)

data class AudioSession(
    val sessionId: String = "",
    val userId: String,
    val audioId: String,
    val completed: Boolean = false,
    val durationSeconds: Int = 0,
)

data class MeditationCategory(
    val categoryId: String = "",
    val name: String,
    val description: String = "",
    val recommendedFor: String = "",
)

data class Quiz(
    val quizId: String = "",
    val title: String,
    val description: String = "",
    val active: Boolean = true,
)

data class QuizQuestion(
    val questionId: String = "",
    val quizId: String,
    val questionText: String,
    val options: List<String> = emptyList(),
)

data class QuizResult(
    val resultId: String = "",
    val userId: String,
    val quizId: String,
    val recommendedCategoryId: String,
    val scores: Map<String, Int> = emptyMap(),
)

data class Affirmation(
    val affirmationId: String = "",
    val text: String,
    val category: String = "motivation",
)

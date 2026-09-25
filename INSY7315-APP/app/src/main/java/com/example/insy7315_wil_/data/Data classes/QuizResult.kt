package com.example.insy7315_wil_.data.`Data classes`

data class QuizResult(
    val resultId: String = "",
    val userId: String,
    val quizId: String,
    val answers: Map<String, Int> = emptyMap(),
    val scores: Map<String, Int> = emptyMap(),
    val recommendedCategoryId: String,
    val recommendedAudioId: String? = null,
)
package com.example.insy7315_wil_.data.`Data classes`

data class QuizResult(
    val resultId: String = "",
    val userId: String,
    val quizId: String,
    val recommendedCategoryId: String,
    val scores: Map<String, Int> = emptyMap(),
)

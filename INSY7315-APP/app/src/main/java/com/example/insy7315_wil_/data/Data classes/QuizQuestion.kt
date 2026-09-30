package com.example.insy7315_wil_.data.`Data classes`

data class QuizQuestion(
    val questionId: String = "",
    val quizId: String,
    val questionText: String,
    val options: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val weight: Int = 1,
    val order: Int = 0,
)
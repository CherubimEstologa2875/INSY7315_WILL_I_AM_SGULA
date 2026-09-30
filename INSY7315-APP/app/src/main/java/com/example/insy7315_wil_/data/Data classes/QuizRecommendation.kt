package com.example.insy7315_wil_.data.`Data classes`

data class QuizRecommendation(
    val resultId: String,
    val categoryId: String,
    val categoryName: String,
    val categoryDescription: String = "",
    val audio: AudioContent? = null,
)
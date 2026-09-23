package com.example.insy7315_wil_.ui.screens.quiz

internal const val ARG_SELECTED_ANSWER = "selected_answer"
internal const val ARG_RECOMMENDED_CATEGORY = "recommended_category"
internal const val ARG_RECOMMENDED_BROADCAST = "recommended_broadcast"
internal const val ARG_PLAYER_TITLE = "player_title"
internal const val ARG_PLAYER_SUBTITLE = "player_subtitle"
internal const val ARG_POINTS_EARNED = "points_earned"

internal const val QUIZ_POINTS_EARNED = 15
internal const val QUIZ_ID = "dailyWellness"

internal data class WellnessQuizRecommendation(
    val answer: String,
    val category: String,
    val broadcast: String,
    val playerTitle: String,
    val playerSubtitle: String,
)

internal fun recommendationForAnswer(answer: String): WellnessQuizRecommendation {
    return when (answer.lowercase()) {
        "calm" -> WellnessQuizRecommendation(
            answer = answer,
            category = "Calm reset",
            broadcast = "David's soft breath broadcast",
            playerTitle = "Calm reset",
            playerSubtitle = "David's soft breath broadcast",
        )
        "anxious" -> WellnessQuizRecommendation(
            answer = answer,
            category = "Grounding reset",
            broadcast = "David's steady breath broadcast",
            playerTitle = "Grounding reset",
            playerSubtitle = "David's steady breath broadcast",
        )
        "overwhelmed" -> WellnessQuizRecommendation(
            answer = answer,
            category = "Unwind and release",
            broadcast = "David's pressure release broadcast",
            playerTitle = "Unwind and release",
            playerSubtitle = "David's pressure release broadcast",
        )
        "motivated" -> WellnessQuizRecommendation(
            answer = answer,
            category = "Focus and flow",
            broadcast = "David's focus broadcast",
            playerTitle = "Focus and flow",
            playerSubtitle = "David's focus broadcast",
        )
        else -> WellnessQuizRecommendation(
            answer = answer,
            category = "Grounding reset",
            broadcast = "David's steady breath broadcast",
            playerTitle = "Grounding reset",
            playerSubtitle = "David's steady breath broadcast",
        )
    }
}

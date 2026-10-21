package com.example.insy7315_wil_.ui.screens.quiz

import com.example.insy7315_wil_.data.`Data classes`.QuizRecommendation
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_ID
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_STORAGE_PATH
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_URL
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_DURATION
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_SUBTITLE
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_TITLE
import com.example.insy7315_wil_.ui.screens.audio.AudioTime

/**
 * Turns the quiz recommendation into the arguments the result screen and then the player read.
 * It returns a plain map rather than a Bundle so it can be unit tested without the Android
 * framework, the same reason JournalInsights is kept separate.
 */
internal object QuizHandoff {

    fun resultArgs(recommendation: QuizRecommendation): Map<String, String> = buildMap {
        put(ARG_RECOMMENDED_CATEGORY, recommendation.categoryName)
        put(ARG_CATEGORY_DESCRIPTION, recommendation.categoryDescription)

        // A track with nothing to stream would only open the player on an error, so the result
        // screen is left to offer the audio library instead
        val audio = recommendation.audio ?: return@buildMap
        if (audio.audioId.isBlank() || (audio.storagePath.isBlank() && audio.downloadUrl.isBlank())) return@buildMap

        put(ARG_AUDIO_ID, audio.audioId)
        put(ARG_TRACK_TITLE, audio.title.ifBlank { "${recommendation.categoryName} session" })
        // Uploads don't always carry a category label, the matched category reads better than
        // the player's generic "Guided meditation"
        put(ARG_TRACK_SUBTITLE, audio.category.ifBlank { recommendation.categoryName })
        if (audio.storagePath.isNotBlank()) put(ARG_AUDIO_STORAGE_PATH, audio.storagePath)
        if (audio.downloadUrl.isNotBlank()) put(ARG_AUDIO_URL, audio.downloadUrl)
        if (audio.durationSeconds > 0) put(ARG_TRACK_DURATION, AudioTime.formatTrackDuration(audio.durationSeconds))
    }
}

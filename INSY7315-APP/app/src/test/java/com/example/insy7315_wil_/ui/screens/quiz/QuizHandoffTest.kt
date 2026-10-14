package com.example.insy7315_wil_.ui.screens.quiz

import com.example.insy7315_wil_.data.`Data classes`.AudioContent
import com.example.insy7315_wil_.data.`Data classes`.QuizRecommendation
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_ID
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_STORAGE_PATH
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_URL
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_DURATION
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_SUBTITLE
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_TITLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// WIL-167: what the quiz hands over is exactly what PlayerFragment reads
class QuizHandoffTest {

    private val track = AudioContent(
        audioId = "track1",
        title = "Evening wind down",
        category = "Meditation",
        categoryId = "sleep",
        storagePath = "audio/track1",
        downloadUrl = "https://example.com/track1.mp3",
        durationSeconds = 600,
    )

    private fun recommend(audio: AudioContent?) = QuizRecommendation(
        resultId = "result1",
        categoryId = "sleep",
        categoryName = "Sleep",
        categoryDescription = "Slow wind-down sessions to help you rest.",
        audio = audio,
    )

    @Test
    fun handsEveryPlayerFieldOver() {
        val args = QuizHandoff.resultArgs(recommend(track))

        assertEquals("Sleep", args[ARG_RECOMMENDED_CATEGORY])
        assertEquals("Slow wind-down sessions to help you rest.", args[ARG_CATEGORY_DESCRIPTION])
        assertEquals("track1", args[ARG_AUDIO_ID])
        assertEquals("Evening wind down", args[ARG_TRACK_TITLE])
        assertEquals("Meditation", args[ARG_TRACK_SUBTITLE])
        assertEquals("audio/track1", args[ARG_AUDIO_STORAGE_PATH])
        assertEquals("https://example.com/track1.mp3", args[ARG_AUDIO_URL])
        assertEquals("10:00", args[ARG_TRACK_DURATION])
        assertTrue(args.containsKey(ARG_AUDIO_ID))
    }

    @Test
    fun aStoragePathAloneIsEnoughToPlay() {
        val args = QuizHandoff.resultArgs(recommend(track.copy(downloadUrl = "")))

        assertTrue(args.containsKey(ARG_AUDIO_ID))
        assertEquals("audio/track1", args[ARG_AUDIO_STORAGE_PATH])
        assertFalse(args.containsKey(ARG_AUDIO_URL))
    }

    @Test
    fun fallsBackToTheCategoryWhenTheTrackHasNoLabels() {
        val args = QuizHandoff.resultArgs(recommend(track.copy(title = "", category = "")))

        assertEquals("Sleep session", args[ARG_TRACK_TITLE])
        assertEquals("Sleep", args[ARG_TRACK_SUBTITLE])
    }

    @Test
    fun leavesTheDurationForThePlayerWhenItIsUnknown() {
        // Admin uploads save durationSeconds as 0, the player reads the real length once loaded
        val args = QuizHandoff.resultArgs(recommend(track.copy(durationSeconds = 0)))

        assertFalse(args.containsKey(ARG_TRACK_DURATION))
    }

    @Test
    fun noTrackInTheCategoryMeansNoPlayerHandoff() {
        val args = QuizHandoff.resultArgs(recommend(null))

        assertEquals("Sleep", args[ARG_RECOMMENDED_CATEGORY])
        assertFalse(args.containsKey(ARG_AUDIO_ID))
    }

    @Test
    fun aTrackWithNothingToStreamIsTreatedAsNoTrack() {
        val args = QuizHandoff.resultArgs(recommend(track.copy(storagePath = "", downloadUrl = "")))

        assertFalse(args.containsKey(ARG_AUDIO_ID))
    }
}

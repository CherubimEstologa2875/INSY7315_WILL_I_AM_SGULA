package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentPlayerBinding
import com.example.insy7315_wil_.data.`Data classes`.AudioSession
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import com.example.insy7315_wil_.ui.screens.quiz.ARG_PLAYER_SUBTITLE
import com.example.insy7315_wil_.ui.screens.quiz.ARG_PLAYER_TITLE
import com.example.insy7315_wil_.ui.screens.quiz.ARG_POINTS_EARNED
import com.example.insy7315_wil_.ui.screens.quiz.ARG_RECOMMENDED_BROADCAST
import com.example.insy7315_wil_.ui.screens.quiz.ARG_RECOMMENDED_CATEGORY
import com.example.insy7315_wil_.ui.screens.quiz.ARG_SELECTED_ANSWER

internal const val ARG_TRACK_TITLE = "track_title"
internal const val ARG_TRACK_SUBTITLE = "track_subtitle"
internal const val ARG_TRACK_DURATION = "track_duration"

private const val COMPLETION_POINTS = 20

// No 'play' functionality yet, so the bar is driven by a fake clock
private const val TICK_MS = 250L
private const val TICK_SECONDS = 10

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private val sessionId = UUID.randomUUID().toString()

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var totalSeconds = 720
    private var sessionCompleted = false

    private val tick = object : Runnable {
        override fun run() {
            elapsedSeconds += TICK_SECONDS
            if (elapsedSeconds >= totalSeconds) {
                elapsedSeconds = totalSeconds
                showProgress()
                finishTrack()
            } else {
                showProgress()
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)

        val args = arguments
        val category = args?.getString(ARG_RECOMMENDED_CATEGORY).orEmpty()
        val broadcast = args?.getString(ARG_RECOMMENDED_BROADCAST).orEmpty()
        val answer = args?.getString(ARG_SELECTED_ANSWER).orEmpty()

        // the quiz result hands over its recommendation, everywhere else just sends a track
        val fromQuiz = category.isNotBlank() || broadcast.isNotBlank() || answer.isNotBlank()
        binding.playerRecommendationCard.isVisible = fromQuiz

        if (fromQuiz) {
            val points = args?.getInt(ARG_POINTS_EARNED, 15) ?: 15
            binding.playerRecommendationSubtitle.text =
                "Your matched meditation is loaded. You earned +$points points."
            binding.playerCategoryLabel.text =
                if (category.isNotBlank()) "Meditation category: $category" else "Meditation category: Calm reset"
            binding.playerBroadcastLabel.text =
                if (broadcast.isNotBlank()) "Broadcast: $broadcast" else "Broadcast: David's soft breath broadcast"
            binding.playerAnswerLabel.text =
                if (answer.isNotBlank()) "Matched from: $answer" else "Matched from: Calm"
        }

        val quizTitle = args?.getString(ARG_PLAYER_TITLE).orEmpty()
        val quizSubtitle = args?.getString(ARG_PLAYER_SUBTITLE).orEmpty()
        val trackTitle = args?.getString(ARG_TRACK_TITLE).orEmpty()
        val trackSubtitle = args?.getString(ARG_TRACK_SUBTITLE).orEmpty()
        val duration = args?.getString(ARG_TRACK_DURATION).orEmpty()

        val title = listOf(trackTitle, quizTitle).firstOrNull { it.isNotBlank() } ?: "Calm reset"
        val subtitle = listOf(trackSubtitle, quizSubtitle).firstOrNull { it.isNotBlank() }
            ?: "Guided meditation"
        totalSeconds = parseDuration(duration) ?: 720

        binding.playerTrackTitle.text = title
        binding.playerTrackSubtitle.text = subtitle
        binding.playerBar.setTitle(title)
        binding.playerBar.setSubtitle(subtitle)
        binding.playerBar.setTotal(formatTime(totalSeconds))
        showProgress()

        binding.playerBar.onPlayPause = { playing -> if (playing) startTrack() else handler.removeCallbacks(tick) }

        binding.playerSleepTimer.onSelect = { choice ->
            binding.playerSleepTimerNote.text = if (choice == "Off") {
                "Playback stops on its own when the timer runs out."
            } else {
                "Playback will fade out after $choice."
            }
        }
    }

    private fun startTrack() {
        // Reset to 0 after the track ends
        if (elapsedSeconds >= totalSeconds) {
            elapsedSeconds = 0
            binding.playerPointsToast.isVisible = false
            showProgress()
        }
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
    }

    private fun finishTrack() {
        handler.removeCallbacks(tick)
        binding.playerBar.isPlaying = false
        if (sessionCompleted) return
        sessionCompleted = true
        binding.playerPointsToast.text = getString(R.string.sgula_points_toast, COMPLETION_POINTS)
        binding.playerPointsToast.isVisible = true
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        repository.saveAudioSession(AudioSession(
            sessionId = sessionId,
            userId = userId,
            audioId = binding.playerTrackTitle.text.toString(),
            completed = true,
            durationSeconds = totalSeconds,
        ))
    }

    private fun showProgress() {
        binding.playerBar.playbackProgress = elapsedSeconds.toFloat() / totalSeconds
        binding.playerBar.setElapsed(formatTime(elapsedSeconds))
    }

    private fun parseDuration(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val minutes = parts[0].toIntOrNull() ?: return null
        val seconds = parts[1].toIntOrNull() ?: return null
        return minutes * 60 + seconds
    }

    private fun formatTime(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

    override fun onDestroyView() {
        handler.removeCallbacks(tick)
        _binding = null
        super.onDestroyView()
    }
}

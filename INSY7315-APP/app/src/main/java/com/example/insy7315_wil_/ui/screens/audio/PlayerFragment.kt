package com.example.insy7315_wil_.ui.screens.audio

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
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

internal const val ARG_AUDIO_ID = "audio_id"
internal const val ARG_AUDIO_URL = "audio_url"
internal const val ARG_AUDIO_STORAGE_PATH = "audio_storage_path"

private const val COMPLETION_POINTS = 20

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseWellnessRepository()

    private val handler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null

    private var audioId = ""

    private var audioUrl = ""

    private var storagePath = ""

    private var sessionCompleted = false

    private val progressRunnable = object : Runnable {

        override fun run() {

            val player = mediaPlayer ?: return

            if (!player.isPlaying) {
                return
            }

            val duration = player.duration

            val position = player.currentPosition

            if (duration > 0) {

                binding.playerBar.playbackProgress =
                    position.toFloat() / duration.toFloat()

                binding.playerBar.setElapsed(
                    formatTime(position / 1000)
                )

                binding.playerBar.setTotal(
                    formatTime(duration / 1000)
                )
            }

            handler.postDelayed(
                this,
                500L
            )
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPlayerBinding.bind(view)

        val args = arguments

        audioId =
            args?.getString(ARG_AUDIO_ID).orEmpty()

        audioUrl =
            args?.getString(ARG_AUDIO_URL).orEmpty()

        storagePath =
            args?.getString(ARG_AUDIO_STORAGE_PATH).orEmpty()

        val category =
            args?.getString(ARG_RECOMMENDED_CATEGORY).orEmpty()

        val broadcast =
            args?.getString(ARG_RECOMMENDED_BROADCAST).orEmpty()

        val answer =
            args?.getString(ARG_SELECTED_ANSWER).orEmpty()

        val fromQuiz =
            category.isNotBlank() ||
                    broadcast.isNotBlank() ||
                    answer.isNotBlank()

        binding.playerRecommendationCard.isVisible =
            fromQuiz

        if (fromQuiz) {

            val points =
                args?.getInt(
                    ARG_POINTS_EARNED,
                    15
                ) ?: 15

            binding.playerRecommendationSubtitle.text =
                "Your matched meditation is loaded. You earned +$points points."

            binding.playerCategoryLabel.text =
                if (category.isNotBlank()) {
                    "Meditation category: $category"
                } else {
                    "Meditation category: Calm reset"
                }

            binding.playerBroadcastLabel.text =
                if (broadcast.isNotBlank()) {
                    "Broadcast: $broadcast"
                } else {
                    "Broadcast: David's soft breath broadcast"
                }

            binding.playerAnswerLabel.text =
                if (answer.isNotBlank()) {
                    "Matched from: $answer"
                } else {
                    "Matched from: Calm"
                }
        }

        val quizTitle =
            args?.getString(ARG_PLAYER_TITLE).orEmpty()

        val quizSubtitle =
            args?.getString(ARG_PLAYER_SUBTITLE).orEmpty()

        val trackTitle =
            args?.getString(ARG_TRACK_TITLE).orEmpty()

        val trackSubtitle =
            args?.getString(ARG_TRACK_SUBTITLE).orEmpty()

        val duration =
            args?.getString(ARG_TRACK_DURATION).orEmpty()

        val title =
            listOf(
                trackTitle,
                quizTitle
            ).firstOrNull {
                it.isNotBlank()
            } ?: "Calm reset"

        val subtitle =
            listOf(
                trackSubtitle,
                quizSubtitle
            ).firstOrNull {
                it.isNotBlank()
            } ?: "Guided meditation"

        binding.playerTrackTitle.text = title
        binding.playerTrackSubtitle.text = subtitle

        binding.playerBar.setTitle(title)
        binding.playerBar.setSubtitle(subtitle)

        binding.playerBar.setTotal(
            if (duration.isNotBlank()) {
                duration
            } else {
                "0:00"
            }
        )

        binding.playerBar.setElapsed("0:00")
        binding.playerBar.playbackProgress = 0f

        binding.playerBar.onPlayPause = { playing ->

            if (playing) {
                playAudio()
            } else {
                pauseAudio()
            }
        }

        binding.playerBar.onToggleLoop = { looping ->

            mediaPlayer?.isLooping = looping
        }
    }

    private fun playAudio() {

        if (mediaPlayer != null) {

            mediaPlayer?.start()

            binding.playerBar.isPlaying = true

            startProgressUpdates()

            return
        }

        if (audioUrl.isNotBlank()) {
            prepareAndPlay(audioUrl)
            return
        }

        if (storagePath.isNotBlank()) {

            repository.getAudioDownloadUrl(
                storagePath = storagePath,

                onSuccess = { url ->
                    audioUrl = url
                    prepareAndPlay(url)
                },

                onError = { error ->
                    binding.playerBar.isPlaying = false

                    Toast.makeText(
                        requireContext(),
                        "Could not load audio: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            return
        }

        Toast.makeText(
            requireContext(),
            "No audio file was found.",
            Toast.LENGTH_LONG
        ).show()

        binding.playerBar.isPlaying = false
    }

    private fun prepareAndPlay(url: String) {

        try {

            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {

                setDataSource(url)

                setOnPreparedListener { player ->

                    binding.playerBar.setTotal(
                        formatTime(
                            player.duration / 1000
                        )
                    )

                    binding.playerBar.isPlaying = true

                    player.start()

                    startProgressUpdates()
                }

                setOnCompletionListener {

                    binding.playerBar.isPlaying = false

                    handler.removeCallbacks(
                        progressRunnable
                    )

                    finishTrack()
                }

                setOnErrorListener { _, _, _ ->

                    binding.playerBar.isPlaying = false

                    handler.removeCallbacks(
                        progressRunnable
                    )

                    Toast.makeText(
                        requireContext(),
                        "Unable to play this audio file.",
                        Toast.LENGTH_LONG
                    ).show()

                    true
                }

                prepareAsync()
            }

        } catch (error: Exception) {

            binding.playerBar.isPlaying = false

            Toast.makeText(
                requireContext(),
                "Audio error: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun pauseAudio() {

        mediaPlayer?.pause()

        handler.removeCallbacks(
            progressRunnable
        )

        binding.playerBar.isPlaying = false
    }

    private fun startProgressUpdates() {

        handler.removeCallbacks(
            progressRunnable
        )

        handler.post(
            progressRunnable
        )
    }

    private fun finishTrack() {

        if (sessionCompleted) {
            return
        }

        sessionCompleted = true

        binding.playerPointsToast.text =
            getString(
                R.string.sgula_points_toast,
                COMPLETION_POINTS
            )

        binding.playerPointsToast.isVisible = true

        val userId =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: return

        val durationSeconds =
            mediaPlayer
                ?.duration
                ?.div(1000)
                ?: 0

        repository.saveAudioSession(
            AudioSession(
                sessionId = UUID.randomUUID().toString(),
                userId = userId,
                audioId = audioId,
                completed = true,
                durationSeconds = durationSeconds
            )
        )
    }

    private fun formatTime(
        seconds: Int
    ): String {

        val safeSeconds =
            seconds.coerceAtLeast(0)

        val minutes =
            safeSeconds / 60

        val remainingSeconds =
            safeSeconds % 60

        return "%d:%02d".format(
            minutes,
            remainingSeconds
        )
    }

    override fun onDestroyView() {

        handler.removeCallbacks(
            progressRunnable
        )

        mediaPlayer?.release()
        mediaPlayer = null

        _binding = null

        super.onDestroyView()
    }
}
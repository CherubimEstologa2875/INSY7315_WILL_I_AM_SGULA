package com.example.insy7315_wil_.ui.screens.audio

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentPlayerBinding
import com.example.insy7315_wil_.data.`Data classes`.AudioSession
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
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
internal const val ARG_STORAGE_PATH = "storage_path"

private const val COMPLETION_POINTS = 20
private const val PROGRESS_POLL_MS = 250L

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private val sessionId = UUID.randomUUID().toString()

    // falls back to the title when a screen has not been updated to pass a real audioId yet
    private var audioId: String = ""
    private var storagePath: String = ""

    private var exoPlayer: ExoPlayer? = null
    private var hasRetriedAfterError = false
    private var sleepTimer: CountDownTimer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var totalSeconds = 720
    private var sessionCompleted = false

    // Player.Listener has no continuous position callback, so poll the real player while it plays
    private val progressPoll = object : Runnable {
        override fun run() {
            val player = exoPlayer ?: return
            elapsedSeconds = (player.currentPosition / 1000).toInt()
            showProgress()
            if (player.isPlaying) handler.postDelayed(this, PROGRESS_POLL_MS)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            binding.playerBuffering.isVisible = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_READY) {
                val durationMs = exoPlayer?.duration ?: 0L
                if (durationMs > 0) {
                    totalSeconds = (durationMs / 1000).toInt()
                    binding.playerBar.setTotal(AudioTime.formatPlaybackTime(totalSeconds))
                }
            }
            if (playbackState == Player.STATE_ENDED) finishTrack()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.playerBar.isPlaying = isPlaying
            handler.removeCallbacks(progressPoll)
            if (isPlaying) handler.post(progressPoll)
        }

        override fun onPlayerError(error: PlaybackException) {
            // one retry covers a dropped connection, a second failure is shown to the user
            if (!hasRetriedAfterError) {
                hasRetriedAfterError = true
                exoPlayer?.prepare()
                return
            }
            binding.playerBuffering.isVisible = false
            binding.playerError.isVisible = true
            binding.playerError.text = getString(R.string.sgula_playback_error)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)

        //  no ViewModel anywhere in app to carry the player across a config change yet,
        // locking rotation here means playback never has to survive one in the first place
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

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
        totalSeconds = AudioTime.parseDuration(duration) ?: 720

        // the quiz flow does not pass a real audioId yet, fall back to the title so a session
        // still gets recorded rather than silently dropped
        audioId = args?.getString(ARG_AUDIO_ID).orEmpty().ifBlank { title }
        storagePath = args?.getString(ARG_STORAGE_PATH).orEmpty()

        binding.playerTrackTitle.text = title
        binding.playerTrackSubtitle.text = subtitle
        binding.playerBar.setTitle(title)
        binding.playerBar.setSubtitle(subtitle)
        binding.playerBar.setTotal(AudioTime.formatPlaybackTime(totalSeconds))
        showProgress()

        binding.playerBar.onPlayPause = { playing ->
            val player = exoPlayer
            if (playing) player?.play() else player?.pause()
        }

        binding.playerSleepTimer.onSelect = { choice -> applySleepTimer(choice) }

        if (storagePath.isNotBlank()) {
            preparePlayer(storagePath)
        } else {
            binding.playerError.isVisible = true
            binding.playerError.text = getString(R.string.sgula_no_audio_linked)
        }
    }

    private fun preparePlayer(path: String) {
        binding.playerBuffering.isVisible = true
        FirebaseStorage.getInstance().reference.child(path).downloadUrl
            .addOnSuccessListener { uri ->
                if (_binding == null) return@addOnSuccessListener
                val player = ExoPlayer.Builder(requireContext()).build()
                exoPlayer = player
                // ExoPlayer defaults to playing as soon as it is ready, buffer in the
                // background but wait for the user to actually press play
                player.playWhenReady = false
                player.addListener(playerListener)
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.playerBuffering.isVisible = false
                binding.playerError.isVisible = true
                binding.playerError.text = getString(R.string.sgula_playback_error)
            }
    }

    private fun applySleepTimer(choice: String) {
        sleepTimer?.cancel()
        binding.playerSleepTimerNote.text = if (choice == "Off") {
            "Playback stops on its own when the timer runs out."
        } else {
            "Playback will fade out after $choice."
        }
        val durationMs = AudioTime.sleepTimerMillis(choice) ?: return
        sleepTimer = object : CountDownTimer(durationMs, durationMs) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                exoPlayer?.pause()
            }
        }.start()
    }

    private fun finishTrack() {
        binding.playerBar.isPlaying = false
        if (sessionCompleted) return
        sessionCompleted = true
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // the toast only shows once the write is confirmed, otherwise it claims points that
        // were never actually awarded if the save fails
        repository.saveAudioSession(AudioSession(
            sessionId = sessionId,
            userId = userId,
            audioId = audioId,
            completed = true,
            durationSeconds = totalSeconds,
        ))
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                binding.playerPointsToast.text = getString(R.string.sgula_points_toast, COMPLETION_POINTS)
                binding.playerPointsToast.isVisible = true
            }
            .addOnFailureListener { error ->
                Log.w("PlayerFragment", "failed to save the audio session", error)
            }
    }

    private fun showProgress() {
        binding.playerBar.playbackProgress = elapsedSeconds.toFloat() / totalSeconds
        binding.playerBar.setElapsed(AudioTime.formatPlaybackTime(elapsedSeconds))
    }

    override fun onDestroyView() {
        handler.removeCallbacks(progressPoll)
        sleepTimer?.cancel()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        _binding = null
        super.onDestroyView()
    }
}

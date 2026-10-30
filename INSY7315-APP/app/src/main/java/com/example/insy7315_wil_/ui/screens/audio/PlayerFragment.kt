package com.example.insy7315_wil_.ui.screens.audio

import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentPlayerBinding
import com.example.insy7315_wil_.data.`Data classes`.AudioSession
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.SucculentPoints
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

internal const val ARG_TRACK_TITLE = "track_title"
internal const val ARG_TRACK_SUBTITLE = "track_subtitle"
internal const val ARG_TRACK_DURATION = "track_duration"

internal const val ARG_AUDIO_ID = "audio_id"
internal const val ARG_AUDIO_URL = "audio_url"
internal const val ARG_AUDIO_STORAGE_PATH = "audio_storage_path"

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

    // one retry per playback attempt, reset once a track actually starts playing
    private var hasRetriedAfterError = false

    // MediaPlayer throws if start/pause land while prepareAsync is still running, a fast double tap can beat onPrepared
    private var isPreparing = false

    private var sleepTimer: CountDownTimer? = null

    private val progressRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            if (!player.isPlaying) return

            val duration = player.duration
            val position = player.currentPosition

            if (duration > 0) {
                binding.playerBar.playbackProgress = position.toFloat() / duration.toFloat()
                binding.playerBar.setElapsed(AudioTime.formatPlaybackTime(position / 1000))
                binding.playerBar.setTotal(AudioTime.formatPlaybackTime(duration / 1000))
            }

            handler.postDelayed(this, 500L)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)
        binding.playerBack.setOnClickListener { findNavController().navigateUp() }

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        val args = arguments

        audioId = args?.getString(ARG_AUDIO_ID).orEmpty()
        audioUrl = args?.getString(ARG_AUDIO_URL).orEmpty()
        storagePath = args?.getString(ARG_AUDIO_STORAGE_PATH).orEmpty()

        val duration = args?.getString(ARG_TRACK_DURATION).orEmpty()

        // The library, broadcasts and the quiz all hand over the same ARG_TRACK_ keys
        val title = args?.getString(ARG_TRACK_TITLE).orEmpty().ifBlank { "Calm reset" }
        val subtitle = args?.getString(ARG_TRACK_SUBTITLE).orEmpty().ifBlank { "Guided meditation" }

        binding.playerTrackTitle.text = title
        binding.playerTrackSubtitle.text = subtitle
        binding.playerArt.showCategory(subtitle)

        binding.playerBar.setTitle(title)
        binding.playerBar.setSubtitle(subtitle)
        binding.playerBar.setTotal(duration.ifBlank { "0:00" })
        binding.playerBar.setElapsed("0:00")
        binding.playerBar.playbackProgress = 0f

        binding.playerBar.onPlayPause = { playing ->
            if (playing) playAudio() else pauseAudio()
        }

        binding.playerBar.onToggleLoop = { looping ->
            mediaPlayer?.isLooping = looping
        }

        binding.playerSleepTimer.onSelect = { choice -> applySleepTimer(choice) }
    }

    private fun playAudio() {
        if (isPreparing) return

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
            binding.playerBuffering.isVisible = true
            repository.getAudioDownloadUrl(
                storagePath = storagePath,
                onSuccess = { url ->
                    if (_binding == null) return@getAudioDownloadUrl
                    audioUrl = url
                    prepareAndPlay(url)
                },
                onError = { error ->
                    if (_binding == null) return@getAudioDownloadUrl
                    binding.playerBuffering.isVisible = false
                    binding.playerBar.isPlaying = false
                    showError(JournalErrors.message(error, "play this track"))
                }
            )
            return
        }

        binding.playerBar.isPlaying = false
        showError(getString(R.string.sgula_no_audio_linked))
    }

    private fun prepareAndPlay(url: String) {
        isPreparing = true
        binding.playerBuffering.isVisible = true

        try {
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)

                setOnPreparedListener { player ->
                    isPreparing = false
                    if (_binding == null) return@setOnPreparedListener
                    binding.playerBuffering.isVisible = false
                    hasRetriedAfterError = false
                    binding.playerBar.setTotal(AudioTime.formatPlaybackTime(player.duration / 1000))
                    binding.playerBar.isPlaying = true
                    player.start()
                    startProgressUpdates()
                }

                setOnCompletionListener {
                    if (_binding != null) binding.playerBar.isPlaying = false
                    handler.removeCallbacks(progressRunnable)
                    finishTrack()
                }

                setOnErrorListener { _, _, _ ->
                    handler.removeCallbacks(progressRunnable)
                    isPreparing = false
                    if (_binding == null) return@setOnErrorListener true
                    binding.playerBuffering.isVisible = false
                    binding.playerBar.isPlaying = false

                    // one retry covers a dropped connection, a second failure is shown to the user
                    if (!hasRetriedAfterError) {
                        hasRetriedAfterError = true
                        handler.post { prepareAndPlay(url) }
                    } else {
                        showError(JournalErrors.message(null, "play this track"))
                    }
                    true
                }

                prepareAsync()
            }
        } catch (error: Exception) {
            isPreparing = false
            binding.playerBuffering.isVisible = false
            binding.playerBar.isPlaying = false
            showError(JournalErrors.message(error, "play this track"))
        }
    }

    private fun showError(message: String?) {
        binding.playerError.isVisible = message != null
        binding.playerError.text = message
    }

    private fun pauseAudio() {
        if (isPreparing) {
            // A pause tap while buffering cancels the pending intent immediately. The next
            // play tap can start the same preparation again without waiting for a stale callback.
            mediaPlayer?.let { player ->
                try { player.reset() } catch (_: IllegalStateException) { }
                player.release()
            }
            mediaPlayer = null
            isPreparing = false
            binding.playerBuffering.isVisible = false
            binding.playerBar.isPlaying = false
            return
        }
        mediaPlayer?.pause()
        handler.removeCallbacks(progressRunnable)
        binding.playerBar.isPlaying = false
    }

    private fun startProgressUpdates() {
        handler.removeCallbacks(progressRunnable)
        handler.post(progressRunnable)
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
                mediaPlayer?.pause()
                if (_binding == null) return
                handler.removeCallbacks(progressRunnable)
                binding.playerBar.isPlaying = false
            }
        }.start()
    }

    private fun finishTrack() {
        if (sessionCompleted) return
        sessionCompleted = true

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val durationSeconds = mediaPlayer?.duration?.div(1000) ?: 0

        // saveAudioSession can throw if validation fails, same risk mood's save() guards against
        val task = try {
            repository.saveAudioSession(
                AudioSession(
                    sessionId = UUID.randomUUID().toString(),
                    userId = userId,
                    audioId = audioId,
                    completed = true,
                    durationSeconds = durationSeconds
                )
            )
        } catch (error: IllegalStateException) {
            showError(JournalErrors.message(error, "save your listening session"))
            return
        }

        // the toast only shows once the write is confirmed, otherwise it claims points that
        // were never actually awarded if the save fails
        task.addOnSuccessListener {
            if (_binding == null) return@addOnSuccessListener
            binding.playerPointsToast.text = getString(R.string.sgula_points_toast, SucculentPoints.AUDIO)
            binding.playerPointsToast.isVisible = true
        }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                showError(JournalErrors.message(error, "save your listening session"))
            }
    }

    override fun onDestroyView() {
        handler.removeCallbacks(progressRunnable)
        sleepTimer?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        _binding = null
        super.onDestroyView()
    }
}

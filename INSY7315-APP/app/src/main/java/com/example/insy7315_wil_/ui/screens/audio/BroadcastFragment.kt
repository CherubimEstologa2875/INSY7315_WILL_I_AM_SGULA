package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.Broadcast
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.databinding.FragmentBroadcastBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors

/**
 * Today's broadcast: the newest recording whose time has come around, with a note underneath
 * about the one still to come.
 */
class BroadcastFragment : Fragment(R.layout.fragment_broadcast) {

    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var latestBroadcast: Broadcast? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View binding replaces findViewById on this screen (Android Developers, 2026).
        _binding = FragmentBroadcastBinding.bind(view)

        binding.broadcastPlay.setOnClickListener { latestBroadcast?.let(::openPlayer) }

        binding.broadcastHistory.setOnClickListener {
            findNavController().navigate(R.id.action_broadcastFragment_to_broadcastHistoryFragment)
        }

        loadLatestBroadcast()
    }

    // Broadcasts come back newest first from GET /api/broadcasts, which makes splitting live from upcoming easy.
    private fun loadLatestBroadcast() {
        binding.broadcastLoading.isVisible = true
        showError(null)
        repository.fetchBroadcasts(
            onSuccess = { broadcasts ->
                if (_binding != null) {
                    binding.broadcastLoading.isVisible = false
                    showBroadcasts(broadcasts)
                }
            },
            onError = { error ->
                if (_binding != null) {
                    binding.broadcastLoading.isVisible = false
                    showError(JournalErrors.message(error, "load today's broadcast"))
                }
            },
        )
    }

    // Each broadcast sits on a Material card (Material Components for Android, n.d.).
    private fun showBroadcasts(broadcasts: List<Broadcast>) {
        // A recording set for later today stays out of the player until its time arrives.
        latestBroadcast = BroadcastSchedule.latestPublished(broadcasts)
        val upcoming = BroadcastSchedule.nextUpcoming(broadcasts)

        latestBroadcast?.let { broadcast ->
            binding.broadcastDate.text = BroadcastSchedule.dateLabel(broadcast)
            binding.broadcastAuthor.text = broadcast.author.ifBlank { DEFAULT_AUTHOR }
            binding.broadcastTopic.text = broadcast.title
            binding.broadcastDescription.text = broadcast.message
            binding.broadcastDescription.isVisible = broadcast.message.isNotBlank()
            binding.broadcastPlay.isEnabled = broadcast.storagePath.isNotBlank()
        }
        binding.broadcastCard.isVisible = latestBroadcast != null

        upcoming?.let { broadcast ->
            binding.broadcastUpcomingDate.text = BroadcastSchedule.dateLabel(broadcast)
            binding.broadcastUpcomingTopic.text = broadcast.title
            binding.broadcastUpcomingDescription.text = broadcast.message
            binding.broadcastUpcomingDescription.isVisible = broadcast.message.isNotBlank()
        }
        binding.broadcastUpcomingCard.isVisible = upcoming != null

        binding.broadcastEmptyText.isVisible = latestBroadcast == null
        binding.broadcastEmptyText.text = if (upcoming == null) {
            "No broadcast has been recorded yet. Check back soon."
        } else {
            "Nothing has gone live yet. The next recording is on its way."
        }
    }

    private fun showError(message: String?) {
        binding.broadcastErrorText.isVisible = message != null
        binding.broadcastErrorText.text = message
    }

    // The player screen is opened with the broadcast's details as arguments (Android Developers, 2026).
    private fun openPlayer(broadcast: Broadcast) {
        findNavController().navigate(
            R.id.action_broadcastFragment_to_playerFragment,
            Bundle().apply {
                putString(ARG_TRACK_TITLE, broadcast.title)
                putString(ARG_TRACK_SUBTITLE, broadcast.author.ifBlank { DEFAULT_AUTHOR })
                putString(ARG_AUDIO_ID, broadcast.broadcastId)
                // Hand over the path: the player fetches the file from Storage itself.
                putString(ARG_AUDIO_STORAGE_PATH, broadcast.storagePath)
                if (broadcast.durationSeconds > 0) {
                    putString(ARG_TRACK_DURATION, AudioTime.formatPlaybackTime(broadcast.durationSeconds))
                }
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        // used when an older broadcast was saved before the name was recorded
        const val DEFAULT_AUTHOR = "Your therapist"
    }
}

/* Reference List
Android Developers, 2026. Navigate to a destination. [online] Available at: <https://developer.android.com/guide/navigation/use-graph/navigate> [Accessed 2 October 2026].
Android Developers, 2026. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 2 October 2026].
Firebase, n.d. Download files with Cloud Storage on Android. [online] Available at: <https://firebase.google.com/docs/storage/android/download-files> [Accessed 2 October 2026].
Firebase, n.d. Order and limit data with Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/order-limit-data> [Accessed 2 October 2026].
Material Components for Android, n.d. Cards. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md> [Accessed 2 October 2026].
*/

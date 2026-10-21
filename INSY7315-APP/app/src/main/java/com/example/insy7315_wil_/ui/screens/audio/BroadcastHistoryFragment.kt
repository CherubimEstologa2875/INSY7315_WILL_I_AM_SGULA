package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.Broadcast
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.databinding.FragmentBroadcastHistoryBinding
import com.example.insy7315_wil_.databinding.ItemBroadcastBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors

/** Past broadcasts, newest first, with anything still to come listed underneath. */
class BroadcastHistoryFragment : Fragment(R.layout.fragment_broadcast_history) {

    private var _binding: FragmentBroadcastHistoryBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBroadcastHistoryBinding.bind(view)
        binding.broadcastHistoryBack.setOnClickListener { findNavController().navigateUp() }
        loadHistory()
    }

    // One ordered read covers both halves of the list (Firebase, n.d.).
    private fun loadHistory() {
        binding.broadcastHistoryLoading.isVisible = true
        showError(null)
        repository.loadBroadcasts()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.broadcastHistoryLoading.isVisible = false
                showBroadcasts(snapshot.documents.map(repository::toBroadcast))
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                binding.broadcastHistoryLoading.isVisible = false
                showError(JournalErrors.message(error, "load your broadcast history"))
            }
    }

    private fun showError(message: String?) {
        binding.broadcastHistoryErrorText.isVisible = message != null
        binding.broadcastHistoryErrorText.text = message
    }

    private fun showBroadcasts(broadcasts: List<Broadcast>) {
        binding.broadcastHistoryList.removeAllViews()
        val published = BroadcastSchedule.published(broadcasts)
        val upcoming = BroadcastSchedule.upcoming(broadcasts)

        if (published.isEmpty() && upcoming.isEmpty()) {
            showError("No broadcasts have been recorded yet.")
            return
        }

        published.forEach { broadcast -> addRow(broadcast, playable = true) }
        upcoming.forEach { broadcast -> addRow(broadcast, playable = false) }
    }

    // Rows are Material cards, inflated through view binding (Material Components for Android, n.d.; Android Developers, 2026).
    private fun addRow(broadcast: Broadcast, playable: Boolean) {
        val inflater = LayoutInflater.from(requireContext())
        val gap = resources.getDimensionPixelSize(R.dimen.sgula_space_3)
        val item = ItemBroadcastBinding.inflate(inflater, binding.broadcastHistoryList, false)

        item.broadcastItemDate.text = BroadcastSchedule.dateLabel(broadcast)
        item.broadcastItemDuration.isVisible = broadcast.durationSeconds > 0
        if (broadcast.durationSeconds > 0) {
            item.broadcastItemDuration.text = AudioTime.formatTrackDuration(broadcast.durationSeconds)
        }
        item.broadcastItemTopic.text = broadcast.title
        item.broadcastItemDescription.text = broadcast.message

        // A scheduled recording shows its date but can't be opened yet.
        item.root.isClickable = playable
        item.root.alpha = if (playable) 1f else 0.6f
        if (playable) item.root.setOnClickListener { openPlayer(broadcast) }

        val params = item.root.layoutParams as LinearLayout.LayoutParams
        if (binding.broadcastHistoryList.isNotEmpty()) params.topMargin = gap
        binding.broadcastHistoryList.addView(item.root, params)
    }

    private fun openPlayer(broadcast: Broadcast) {
        findNavController().navigate(
            R.id.action_broadcastHistoryFragment_to_playerFragment,
            Bundle().apply {
                putString(ARG_TRACK_TITLE, broadcast.title)
                putString(ARG_TRACK_SUBTITLE, broadcast.author.ifBlank { "Your therapist" })
                putString(ARG_AUDIO_ID, broadcast.broadcastId)
                putString(ARG_AUDIO_STORAGE_PATH, broadcast.storagePath)
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

/* Reference List
Android Developers, 2026. Navigate to a destination. [online] Available at: <https://developer.android.com/guide/navigation/use-graph/navigate> [Accessed 2 October 2026].
Android Developers, 2026. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 2 October 2026].
Firebase, n.d. Order and limit data with Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/order-limit-data> [Accessed 2 October 2026].
Material Components for Android, n.d. Cards. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md> [Accessed 2 October 2026].
*/

package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.Broadcast
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.databinding.FragmentBroadcastHistoryBinding
import com.example.insy7315_wil_.databinding.ItemBroadcastBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("d MMMM", Locale.getDefault())

class BroadcastHistoryFragment : Fragment(R.layout.fragment_broadcast_history) {

    private var _binding: FragmentBroadcastHistoryBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBroadcastHistoryBinding.bind(view)
        loadHistory()
    }

    private fun loadHistory() {
        binding.broadcastHistoryLoading.isVisible = true
        // loadBroadcasts() already orders newest first
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
        val inflater = LayoutInflater.from(requireContext())
        val gap = resources.getDimensionPixelSize(R.dimen.sgula_space_3)

        broadcasts.forEach { broadcast ->
            val item = ItemBroadcastBinding.inflate(inflater, binding.broadcastHistoryList, false)
            item.broadcastItemDate.text = broadcast.publishedAtMillis
                ?.let { historyDateFormat.format(Date(it)) }
                .orEmpty()
            // Broadcast has no duration field so hide the placeholder instead of it just being blank
            item.broadcastItemDuration.isVisible = false
            item.broadcastItemTopic.text = broadcast.title
            item.broadcastItemDescription.text = broadcast.message
            item.root.setOnClickListener { openPlayer(broadcast) }

            val params = item.root.layoutParams as LinearLayout.LayoutParams
            if (binding.broadcastHistoryList.isNotEmpty()) params.topMargin = gap
            binding.broadcastHistoryList.addView(item.root, params)
        }
    }

    private fun openPlayer(broadcast: Broadcast) {
        findNavController().navigate(
            R.id.action_broadcastHistoryFragment_to_playerFragment,
            Bundle().apply {
                putString(ARG_TRACK_TITLE, broadcast.title)
                putString(ARG_AUDIO_ID, broadcast.broadcastId)
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
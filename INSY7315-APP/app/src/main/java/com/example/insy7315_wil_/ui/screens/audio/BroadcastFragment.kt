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

class BroadcastFragment : Fragment(R.layout.fragment_broadcast) {

    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var latestBroadcast: Broadcast? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBroadcastBinding.bind(view)

        binding.broadcastPlay.setOnClickListener { latestBroadcast?.let(::openPlayer) }

        binding.broadcastHistory.setOnClickListener {
            findNavController().navigate(R.id.action_broadcastFragment_to_broadcastHistoryFragment)
        }

        loadLatestBroadcast()
    }

    private fun loadLatestBroadcast() {
        binding.broadcastLoading.isVisible = true
        // loadBroadcasts() already orders newest first, so the first result is today's broadcast
        repository.loadBroadcasts()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.broadcastLoading.isVisible = false
                latestBroadcast = snapshot.documents.firstOrNull()?.let(repository::toBroadcast)
                latestBroadcast?.let { broadcast ->
                    binding.broadcastTopic.text = broadcast.title
                    binding.broadcastDescription.text = broadcast.message
                    binding.broadcastCard.isVisible = true
                }
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                binding.broadcastLoading.isVisible = false
                showError(JournalErrors.message(error, "load today's broadcast"))
            }
    }

    private fun showError(message: String?) {
        binding.broadcastErrorText.isVisible = message != null
        binding.broadcastErrorText.text = message
    }

    private fun openPlayer(broadcast: Broadcast) {
        findNavController().navigate(
            R.id.action_broadcastFragment_to_playerFragment,
            Bundle().apply {
                putString(ARG_TRACK_TITLE, broadcast.title)
                putString(ARG_TRACK_SUBTITLE, "Anat Casey")
                putString(ARG_AUDIO_ID, broadcast.broadcastId)
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
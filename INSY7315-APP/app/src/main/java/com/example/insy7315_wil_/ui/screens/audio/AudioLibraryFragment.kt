package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.AudioContent
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.databinding.FragmentAudioLibraryBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.widget.SgulaTrackItemView

private val categoryLabels = listOf("White noise", "Nature sounds", "Guided meditation")

class AudioLibraryFragment : Fragment(R.layout.fragment_audio_library) {

    private var _binding: FragmentAudioLibraryBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var tracksByTab: List<List<AudioContent>> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAudioLibraryBinding.bind(view)

        binding.audioTabs.onSelect = { index -> showCategory(index) }
        loadTracks()
    }

    private fun loadTracks() {
        repository.loadAudioContent()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                val allTracks = snapshot.documents.map(repository::toAudioContent)
                tracksByTab = AudioCategoryGrouping.groupByTab(allTracks)
                showCategory(binding.audioTabs.selectedIndex)
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                showError(JournalErrors.message(error, "load the audio library"))
            }
    }

    private fun showError(message: String?) {
        binding.audioErrorText.isVisible = message != null
        binding.audioErrorText.text = message
    }

    private fun showCategory(index: Int) {
        binding.audioCategoryLabel.text = categoryLabels[index]
        binding.audioTrackList.removeAllViews()
        if (tracksByTab.isEmpty()) return

        val gap = resources.getDimensionPixelSize(R.dimen.sgula_space_3)

        tracksByTab[index].forEach { track ->
            val item = SgulaTrackItemView(requireContext()).apply {
                setTitle(track.title)
                setDuration(AudioTime.formatTrackDuration(track.durationSeconds))
                isFavourite = false
                setOnClickListener { openPlayer(track) }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            if (binding.audioTrackList.childCount > 0) params.topMargin = gap
            binding.audioTrackList.addView(item, params)
        }
    }

    private fun openPlayer(track: AudioContent) {
        findNavController().navigate(
            R.id.action_audioLibraryFragment_to_playerFragment,
            Bundle().apply {
                putString(ARG_TRACK_TITLE, track.title)
                putString(ARG_TRACK_SUBTITLE, track.category)
                putString(ARG_TRACK_DURATION, AudioTime.formatTrackDuration(track.durationSeconds))
                putString(ARG_AUDIO_ID, track.audioId)
                putString(ARG_STORAGE_PATH, track.storagePath)
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

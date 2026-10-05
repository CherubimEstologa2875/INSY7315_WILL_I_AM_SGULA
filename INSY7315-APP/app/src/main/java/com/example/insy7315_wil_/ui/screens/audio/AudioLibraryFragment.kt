package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentAudioLibraryBinding
import com.example.insy7315_wil_.data.`Data classes`.AudioContent
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.widget.SgulaTrackItemView

/**
 * The audio library, split into the tabs named in R.array.sgula_audio_tabs. Tracks find their
 * tab by keyword rather than an exact match, because the category wording has drifted: the
 * upload form says "Meditation" and older records say "Guided meditation".
 */
class AudioLibraryFragment : Fragment(R.layout.fragment_audio_library) {

    private var _binding: FragmentAudioLibraryBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseWellnessRepository()

    private var tabNames: List<String> = emptyList()
    private var groupedTracks: List<List<AudioContent>> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAudioLibraryBinding.bind(view)

        // Tab labels and the admin upload form read the same array, so the two can't drift
        // apart again (Android Developers, 2026).
        tabNames = resources.getStringArray(R.array.sgula_audio_tabs).toList()

        binding.audioTabs.onSelect = { index -> showCategory(index) }

        loadAudio()
    }

    // One read per visit is enough here; the library changes rarely (Firebase, n.d.).
    private fun loadAudio() {
        binding.audioLoading.isVisible = true
        binding.audioErrorText.isVisible = false
        binding.audioTrackList.removeAllViews()

        repository.loadAudioContent()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.audioLoading.isVisible = false
                val tracks = snapshot.documents.map(repository::toAudioContent)
                groupedTracks = AudioCategoryGrouping.groupByTab(tracks)
                showCategory(binding.audioTabs.selectedIndex)
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                binding.audioLoading.isVisible = false
                binding.audioTrackList.removeAllViews()
                showMessage(JournalErrors.message(error, "load the audio library"))
            }
    }

    private fun showCategory(index: Int) {
        if (index !in tabNames.indices) return

        binding.audioCategoryLabel.text = tabNames[index]
        binding.audioTrackList.removeAllViews()

        val categoryTracks = groupedTracks.getOrNull(index).orEmpty()
        if (categoryTracks.isEmpty()) {
            showMessage("Nothing in ${tabNames[index].lowercase()} yet. The practice adds new audio from the admin screen.")
            return
        }
        binding.audioErrorText.isVisible = false

        val gap = resources.getDimensionPixelSize(R.dimen.sgula_space_3)

        categoryTracks.forEach { audio ->
            val item = SgulaTrackItemView(requireContext()).apply {
                setTitle(audio.title)
                setDuration(
                    if (audio.durationSeconds > 0) {
                        AudioTime.formatTrackDuration(audio.durationSeconds)
                    } else {
                        "Audio"
                    },
                )
                setOnClickListener { openPlayer(audio) }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            if (binding.audioTrackList.childCount > 0) params.topMargin = gap

            binding.audioTrackList.addView(item, params)
        }
    }

    private fun showMessage(message: String) {
        binding.audioErrorText.text = message
        binding.audioErrorText.isVisible = true
    }

    private fun openPlayer(audio: AudioContent) {
        findNavController().navigate(
            R.id.action_audioLibraryFragment_to_playerFragment,
            Bundle().apply {
                putString(ARG_TRACK_TITLE, audio.title)
                putString(ARG_TRACK_SUBTITLE, audio.category)
                if (audio.durationSeconds > 0) {
                    putString(ARG_TRACK_DURATION, AudioTime.formatTrackDuration(audio.durationSeconds))
                }
                putString(ARG_AUDIO_ID, audio.audioId)
                putString(ARG_AUDIO_URL, audio.downloadUrl)
                putString(ARG_AUDIO_STORAGE_PATH, audio.storagePath)
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
Android Developers, 2026. String resources. [online] Available at: <https://developer.android.com/guide/topics/resources/string-resource> [Accessed 2 October 2026].
Android Developers, 2026. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 2 October 2026].
Firebase, n.d. Get data with Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/get-data> [Accessed 2 October 2026].
*/

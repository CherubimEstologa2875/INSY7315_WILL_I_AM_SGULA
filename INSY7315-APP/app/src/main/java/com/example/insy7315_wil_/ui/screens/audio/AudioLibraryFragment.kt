package com.example.insy7315_wil_.ui.screens.audio

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentAudioLibraryBinding
import com.example.insy7315_wil_.data.`Data classes`.AudioContent
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.widget.SgulaTrackItemView

private val categories = listOf(
    "White noise",
    "Nature sounds",
    "Guided meditation"
)

class AudioLibraryFragment : Fragment(R.layout.fragment_audio_library) {

    private var _binding: FragmentAudioLibraryBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseWellnessRepository()

    private var audioContent = emptyList<AudioContent>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAudioLibraryBinding.bind(view)

        binding.audioTabs.onSelect = { index ->
            showCategory(index)
        }

        loadAudio()
    }

    private fun loadAudio() {

        binding.audioLoading.visibility = View.VISIBLE
        binding.audioErrorText.visibility = View.GONE

        repository.loadAudioContent()
            .addOnSuccessListener { snapshot ->

                audioContent = snapshot.documents.map {
                    repository.toAudioContent(it)
                }

                binding.audioLoading.visibility = View.GONE

                if (audioContent.isEmpty()) {

                    binding.audioErrorText.text =
                        "No audio is currently available."

                    binding.audioErrorText.visibility = View.VISIBLE

                    binding.audioTrackList.removeAllViews()

                } else {

                    binding.audioErrorText.visibility = View.GONE

                    showCategory(
                        binding.audioTabs.selectedIndex
                    )
                }
            }
            .addOnFailureListener { error ->

                binding.audioLoading.visibility = View.GONE

                binding.audioTrackList.removeAllViews()

                binding.audioErrorText.text =
                    JournalErrors.message(error, "load the audio library")

                binding.audioErrorText.visibility = View.VISIBLE
            }
    }

    private fun showCategory(index: Int) {

        if (index !in categories.indices) {
            return
        }

        val category = categories[index]

        binding.audioCategoryLabel.text = category

        binding.audioTrackList.removeAllViews()

        val categoryTracks = audioContent.filter { audio ->

            audio.category
                .trim()
                .equals(
                    category.trim(),
                    ignoreCase = true
                )
        }

        if (categoryTracks.isEmpty()) {

            binding.audioErrorText.text =
                "No audio available in this category."

            binding.audioErrorText.visibility = View.VISIBLE

            return
        }

        binding.audioErrorText.visibility = View.GONE

        val gap = resources.getDimensionPixelSize(
            R.dimen.sgula_space_3
        )

        categoryTracks.forEach { audio ->

            val item = SgulaTrackItemView(
                requireContext()
            ).apply {

                setTitle(audio.title)

                setDuration(
                    if (audio.durationSeconds > 0) {
                        formatDuration(
                            audio.durationSeconds
                        )
                    } else {
                        "Audio"
                    }
                )

                setOnClickListener {
                    openPlayer(audio)
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            if (binding.audioTrackList.childCount > 0) {
                params.topMargin = gap
            }

            binding.audioTrackList.addView(
                item,
                params
            )
        }
    }

    private fun openPlayer(audio: AudioContent) {

        findNavController().navigate(
            R.id.action_audioLibraryFragment_to_playerFragment,
            Bundle().apply {

                putString(
                    ARG_TRACK_TITLE,
                    audio.title
                )

                putString(
                    ARG_TRACK_SUBTITLE,
                    audio.category
                )

                putString(
                    ARG_TRACK_DURATION,
                    formatDuration(
                        audio.durationSeconds
                    )
                )

                putString(
                    ARG_AUDIO_ID,
                    audio.audioId
                )

                putString(
                    ARG_AUDIO_URL,
                    audio.downloadUrl
                )

                putString(
                    ARG_AUDIO_STORAGE_PATH,
                    audio.storagePath
                )
            }
        )
    }

    private fun formatDuration(
        seconds: Int
    ): String {

        if (seconds <= 0) {
            return "0:00"
        }

        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return "%d:%02d".format(
            minutes,
            remainingSeconds
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
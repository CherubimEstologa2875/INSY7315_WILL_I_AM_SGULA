package com.example.insy7315_wil_.ui.screens.quiz

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentQuizResultBinding
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_ID
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_STORAGE_PATH
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_DURATION
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_SUBTITLE
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_TITLE

class QuizResultFragment : Fragment(R.layout.fragment_quiz_result) {

    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizResultBinding.bind(view)

        val args = requireArguments()
        val category = args.getString(ARG_RECOMMENDED_CATEGORY).orEmpty()
        val description = args.getString(ARG_CATEGORY_DESCRIPTION).orEmpty()
        val trackTitle = args.getString(ARG_TRACK_TITLE).orEmpty()

        binding.quizResultCategory.text = category
        binding.quizResultDescription.text = description

        if (trackTitle.isBlank()) {
            binding.quizResultTrack.text = "There's no track in this category yet. Have a look through the audio library instead."
            binding.quizOpenPlayerButton.text = "Browse the audio library"
            binding.quizOpenPlayerButton.setOnClickListener {
                findNavController().navigate(R.id.action_quizResultFragment_to_audioLibraryFragment)
            }
            return
        }

        binding.quizResultTrack.text = "Suggested track: $trackTitle"
        binding.quizOpenPlayerButton.text = "Play $trackTitle"
        binding.quizOpenPlayerButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_quizResultFragment_to_playerFragment,
                Bundle().apply {
                    putString(ARG_TRACK_TITLE, trackTitle)
                    putString(ARG_TRACK_SUBTITLE, args.getString(ARG_TRACK_SUBTITLE))
                    putString(ARG_AUDIO_ID, args.getString(ARG_AUDIO_ID))
                    putString(ARG_AUDIO_STORAGE_PATH, args.getString(ARG_AUDIO_STORAGE_PATH))
                    putString(ARG_TRACK_DURATION, args.getString(ARG_TRACK_DURATION))
                    putString(ARG_RECOMMENDED_CATEGORY, category)
                    putString(ARG_CATEGORY_DESCRIPTION, description)
                    putInt(ARG_POINTS_EARNED, QUIZ_POINTS_EARNED)
                },
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
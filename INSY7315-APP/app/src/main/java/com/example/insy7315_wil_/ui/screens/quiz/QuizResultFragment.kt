package com.example.insy7315_wil_.ui.screens.quiz

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.SucculentPoints
import com.example.insy7315_wil_.databinding.FragmentQuizResultBinding
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_ID
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
        // The result only exists once calculateQuizRecommendation saved it, and saving it is what
        // earns the quiz points, so by this screen they have been awarded
        binding.quizResultPoints.text = getString(R.string.sgula_points_toast, SucculentPoints.QUIZ)

        if (args.getString(ARG_AUDIO_ID).isNullOrBlank()) {
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
            // Passes on a copy of everything the quiz handed over rather than picking keys out
            // one by one, so a field added to the hand-off later can't be dropped on the way
            findNavController().navigate(R.id.action_quizResultFragment_to_playerFragment, Bundle(args))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
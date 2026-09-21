package com.example.insy7315_wil_.ui.screens.quiz

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentQuizQuestionBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.QuizResult
import com.google.firebase.auth.FirebaseAuth

class QuizQuestionFragment : Fragment(R.layout.fragment_quiz_question) {

    private var _binding: FragmentQuizQuestionBinding? = null
    private val binding get() = _binding!!

    private var selectedAnswer: String? = null
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizQuestionBinding.bind(view)

        val answers = resources.getStringArray(R.array.sgula_wellness_quiz_answers)

        binding.quizOptions.onSelect = { index ->
            selectedAnswer = answers[index]
            binding.quizSelectedHint.text = "Selected: ${answers[index]}"
            binding.quizContinueButton.isEnabled = true
        }

        binding.quizContinueButton.setOnClickListener {
            val answer = selectedAnswer ?: answers.first()
            val recommendation = recommendationForAnswer(answer)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            binding.quizContinueButton.isEnabled = false
            repository.saveQuizResult(QuizResult(
                userId = userId,
                quizId = "wellness-check-in",
                recommendedCategoryId = recommendation.category,
                scores = mapOf(recommendation.answer to 1),
            )).addOnSuccessListener {
                findNavController().navigate(
                    R.id.action_quizQuestionFragment_to_quizResultFragment,
                    bundleOf(
                        ARG_SELECTED_ANSWER to recommendation.answer,
                        ARG_RECOMMENDED_CATEGORY to recommendation.category,
                        ARG_RECOMMENDED_BROADCAST to recommendation.broadcast,
                        ARG_PLAYER_TITLE to recommendation.playerTitle,
                        ARG_PLAYER_SUBTITLE to recommendation.playerSubtitle,
                        ARG_POINTS_EARNED to QUIZ_POINTS_EARNED,
                    ),
                )
            }.addOnFailureListener { binding.quizContinueButton.isEnabled = true }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

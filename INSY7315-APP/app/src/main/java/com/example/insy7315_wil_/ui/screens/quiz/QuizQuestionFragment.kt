package com.example.insy7315_wil_.ui.screens.quiz

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentQuizQuestionBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.QuizQuestion
import com.example.insy7315_wil_.data.`Data classes`.QuizResult
import com.google.firebase.auth.FirebaseAuth

class QuizQuestionFragment : Fragment(R.layout.fragment_quiz_question) {

    private var _binding: FragmentQuizQuestionBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseWellnessRepository()
    private var questions: List<QuizQuestion> = emptyList()
    private var current = 0
    private val answers = mutableMapOf<String, Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizQuestionBinding.bind(view)

        binding.quizOptions.onSelect = { index ->
            answers[questions[current].questionId] = index
            binding.quizContinueButton.isEnabled = true
        }
        binding.quizBackButton.setOnClickListener { showQuestion(current - 1) }
        binding.quizContinueButton.setOnClickListener {
            if (current < questions.lastIndex) showQuestion(current + 1) else finishQuiz()
        }

        repository.loadQuizQuestions(QUIZ_ID, onQuestions = { loaded ->
            if (_binding == null || loaded.isEmpty()) return@loadQuizQuestions
            questions = loaded
            showQuestion(0)
        })
    }

    private fun showQuestion(index: Int) {
        current = index
        val question = questions[index]
        binding.quizStepLabel.text = "Step ${index + 1} of ${questions.size}"
        binding.quizProgress.progress = (index + 1) * 100 / questions.size
        binding.quizQuestionText.text = question.questionText
        binding.quizOptions.setOptions(question.options, answers[question.questionId])
        binding.quizBackButton.isVisible = index > 0
        binding.quizContinueButton.isEnabled = question.questionId in answers
        binding.quizContinueButton.text = if (index == questions.lastIndex) "See result" else "Next"
    }

    private fun finishQuiz() {
        val first = questions.first()
        val recommendation = recommendationForAnswer(first.options[answers.getValue(first.questionId)])
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.quizContinueButton.isEnabled = false
        repository.saveQuizResult(QuizResult(
            userId = userId,
            quizId = QUIZ_ID,
            recommendedCategoryId = recommendation.category,
            scores = answers,
        )).addOnSuccessListener {
            findNavController().navigate(
                R.id.action_quizQuestionFragment_to_quizResultFragment,
                Bundle().apply {
                    putString(ARG_SELECTED_ANSWER, recommendation.answer)
                    putString(ARG_RECOMMENDED_CATEGORY, recommendation.category)
                    putString(ARG_RECOMMENDED_BROADCAST, recommendation.broadcast)
                    putString(ARG_PLAYER_TITLE, recommendation.playerTitle)
                    putString(ARG_PLAYER_SUBTITLE, recommendation.playerSubtitle)
                    putInt(ARG_POINTS_EARNED, QUIZ_POINTS_EARNED)
                },
            )
        }.addOnFailureListener { _binding?.quizContinueButton?.isEnabled = true }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
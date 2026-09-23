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
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_ID
import com.example.insy7315_wil_.ui.screens.audio.ARG_STORAGE_PATH
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_DURATION
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_SUBTITLE
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_TITLE
import com.example.insy7315_wil_.ui.screens.audio.AudioTime

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
        binding.quizContinueButton.isEnabled = false
        repository.calculateQuizRecommendation(QUIZ_ID, answers).addOnSuccessListener { recommendation ->
            if (_binding == null) return@addOnSuccessListener
            findNavController().navigate(
                R.id.action_quizQuestionFragment_to_quizResultFragment,
                Bundle().apply {
                    putString(ARG_RECOMMENDED_CATEGORY, recommendation.categoryName)
                    putString(ARG_CATEGORY_DESCRIPTION, recommendation.categoryDescription)
                    recommendation.audio?.let { audio ->
                        putString(ARG_AUDIO_ID, audio.audioId)
                        putString(ARG_TRACK_TITLE, audio.title)
                        putString(ARG_TRACK_SUBTITLE, audio.category)
                        putString(ARG_STORAGE_PATH, audio.storagePath)
                        if (audio.durationSeconds > 0) {
                            putString(ARG_TRACK_DURATION, AudioTime.formatTrackDuration(audio.durationSeconds))
                        }
                    }
                },
            )
        }.addOnFailureListener { _binding?.quizContinueButton?.isEnabled = true }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
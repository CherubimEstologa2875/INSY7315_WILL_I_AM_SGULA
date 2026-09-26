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
import com.example.insy7315_wil_.ui.screens.audio.ARG_AUDIO_STORAGE_PATH
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_DURATION
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_SUBTITLE
import com.example.insy7315_wil_.ui.screens.audio.ARG_TRACK_TITLE
import com.example.insy7315_wil_.ui.screens.audio.AudioTime
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.google.firebase.functions.FirebaseFunctionsException

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
            when {
                questions.isEmpty() -> loadQuestions()
                current < questions.lastIndex -> showQuestion(current + 1)
                else -> finishQuiz()
            }
        }

        loadQuestions()
    }

    private fun loadQuestions() {
        showError(null)
        binding.quizStepLabel.text = "Loading questions..."
        binding.quizContinueButton.isEnabled = false
        repository.loadQuizQuestions(QUIZ_ID, onQuestions = { loaded ->
            if (_binding == null) return@loadQuizQuestions
            if (loaded.isEmpty()) {
                showLoadFailed("There are no quiz questions yet. Please try again later.")
                return@loadQuizQuestions
            }
            questions = loaded
            showQuestion(0)
        }, onError = { error ->
            if (_binding != null) showLoadFailed(JournalErrors.message(error, "load the quiz"))
        })
    }

    // The continue button doubles as the retry while there are no questions
    private fun showLoadFailed(message: String) {
        binding.quizStepLabel.text = "Couldn't load the quiz"
        binding.quizContinueButton.text = "Try again"
        binding.quizContinueButton.isEnabled = true
        showError(message)
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
        setSubmitting(true)
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
                        putString(ARG_AUDIO_STORAGE_PATH, audio.storagePath)
                        if (audio.durationSeconds > 0) {
                            putString(ARG_TRACK_DURATION, AudioTime.formatTrackDuration(audio.durationSeconds))
                        }
                    }
                },
            )
        }.addOnFailureListener { error ->
            if (_binding == null) return@addOnFailureListener
            setSubmitting(false)
            showError(resultErrorMessage(error))
        }
    }

    private fun setSubmitting(submitting: Boolean) {
        binding.quizContinueButton.isEnabled = !submitting
        binding.quizContinueButton.text = if (submitting) "Loading..." else "See result"
        binding.quizBackButton.isEnabled = !submitting
        if (submitting) showError(null)
    }

    // JournalErrors only reads Firestore codes, the function throws its own exception type
    private fun resultErrorMessage(error: Exception): String = when ((error as? FirebaseFunctionsException)?.code) {
        FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
            "We couldn't get your result. Check your internet connection and try again."
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Your session has expired. Please sign in again."
        else -> "Something went wrong getting your result. Please try again."
    }

    private fun showError(message: String?) {
        binding.quizErrorText.isVisible = message != null
        binding.quizErrorText.text = message
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
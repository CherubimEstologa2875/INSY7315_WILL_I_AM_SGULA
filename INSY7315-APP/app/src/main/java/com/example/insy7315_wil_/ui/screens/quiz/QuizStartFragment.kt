package com.example.insy7315_wil_.ui.screens.quiz

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.databinding.FragmentQuizStartBinding
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent

class QuizStartFragment : Fragment(R.layout.fragment_quiz_start) {

    private var _binding: FragmentQuizStartBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (redirectGuestFromMemberContent()) return
        _binding = FragmentQuizStartBinding.bind(view)

        binding.quizStartPoints.text = "+$QUIZ_POINTS_EARNED points"

        binding.quizStartButton.setOnClickListener {
            findNavController().navigate(R.id.action_quizStartFragment_to_quizQuestionFragment)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

package com.example.insy7315_wil_.ui.screens.mood

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.databinding.FragmentMoodLogBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.MoodEntry
import com.google.firebase.auth.FirebaseAuth
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent

class MoodLogFragment : Fragment(R.layout.fragment_mood_log) {
    private var _binding: FragmentMoodLogBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var selectedMood = 3
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMoodLogBinding.bind(view)
        if (redirectGuestFromMemberContent()) return
        // the scale is 0-indexed, mood levels are 1 to 5
        binding.moodScale.selectedIndex = selectedMood - 1
        binding.moodScale.onSelect = { index -> selectedMood = index + 1 }
        binding.moodHistoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_moodLogFragment_to_moodHistoryFragment)
        }
        binding.moodSaveButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            binding.moodSaveButton.isEnabled = false
            repository.addMoodEntry(MoodEntry(userId = userId, moodLevel = selectedMood, note = binding.moodNote.text.trim()))
                .addOnCompleteListener { binding.moodSaveButton.isEnabled = true }
        }
    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

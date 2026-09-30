package com.example.insy7315_wil_.ui.screens.mood

import android.os.Bundle
import android.text.InputFilter
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.databinding.FragmentMoodLogBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.MoodEntry
import com.google.firebase.auth.FirebaseAuth
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent
import com.example.insy7315_wil_.ui.widget.SgulaModal

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
        binding.moodNote.editText.filters = arrayOf(InputFilter.LengthFilter(500))
        binding.moodSaveButton.setOnClickListener { save() }
    }

    private fun save() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showError("Please sign in again to log your mood.")
            return
        }
        setSaving(true)
        val task = try {
            repository.addMoodEntry(MoodEntry(userId = userId, moodLevel = selectedMood, note = binding.moodNote.text.trim()))
        } catch (error: IllegalStateException) {
            setSaving(false)
            showError(JournalErrors.message(error, "save your mood"))
            return
        }
        task.addOnSuccessListener {
            if (_binding == null) return@addOnSuccessListener
            setSaving(false)
            binding.moodNote.text = ""
            SgulaModal.show(
                context = requireContext(),
                title = "Mood saved",
                body = "Your check-in is saved and your succulent earned +5 points.",
                confirmText = "View history",
                onConfirm = {
                    if (_binding != null) {
                        findNavController().navigate(R.id.action_moodLogFragment_to_moodHistoryFragment)
                    }
                },
                dismissText = "Done",
                icon = "✓",
            )
        }.addOnFailureListener { error ->
            if (_binding == null) return@addOnFailureListener
            setSaving(false)
            showError(JournalErrors.message(error, "save your mood"))
        }
    }

    private fun setSaving(saving: Boolean) {
        binding.moodSaveButton.isEnabled = !saving
        binding.moodSaveButton.text = if (saving) "Saving..." else "Save mood"
        if (saving) showError(null)
    }

    private fun showError(message: String?) {
        binding.moodErrorText.isVisible = message != null
        binding.moodErrorText.text = message
    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

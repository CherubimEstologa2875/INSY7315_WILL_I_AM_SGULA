package com.example.insy7315_wil_.ui.screens.journal

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.databinding.FragmentJournalEditorBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.JournalEntry
import com.google.firebase.auth.FirebaseAuth
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent

class JournalEditorFragment : Fragment(R.layout.fragment_journal_editor) {
    private var _binding: FragmentJournalEditorBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJournalEditorBinding.bind(view)
        if (redirectGuestFromMemberContent()) return
        binding.journalSaveButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val content = binding.journalEntry.text.trim()
            if (content.isBlank()) { binding.journalEntry.error = "Write a reflection before saving."; return@setOnClickListener }
            binding.journalSaveButton.isEnabled = false
            repository.addJournalEntry(JournalEntry(userId = userId, content = content, prompt = "What made you feel calm today?"))
                .addOnCompleteListener { binding.journalSaveButton.isEnabled = true }
        }
    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

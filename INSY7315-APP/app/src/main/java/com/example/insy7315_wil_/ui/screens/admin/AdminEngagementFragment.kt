package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.example.insy7315_wil_.databinding.FragmentAdminEngagementBinding
import com.example.insy7315_wil_.databinding.ItemAdminEngagementBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent

// Sessions come from AudioSession, journal entries are never read here
class AdminEngagementFragment : Fragment(R.layout.fragment_admin_engagement) {
    private var _binding: FragmentAdminEngagementBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminEngagementBinding.bind(view)

        repository.checkAdmin(
            onResult = { isAdmin ->
                if (_binding == null) return@checkAdmin
                if (isAdmin) loadEngagement() else redirectNonAdminFromAdminContent()
            },
            onError = { if (_binding != null) redirectNonAdminFromAdminContent() },
        )
    }

    private fun loadEngagement() {
        binding.adminEngagementLoading.isVisible = true
        binding.adminEngagementEmpty.isVisible = false

        repository.loadAllUsers(
            onSuccess = { users ->
                if (_binding == null) return@loadAllUsers
                binding.adminEngagementLoading.isVisible = false
                binding.adminEngagementContainer.removeAllViews()
                val clients = users
                    .filter { it.role != UserProfile.ROLE_ADMIN }
                    .sortedBy { nameOf(it).lowercase() }
                if (clients.isEmpty()) {
                    binding.adminEngagementEmpty.text = "No clients have signed up yet."
                    binding.adminEngagementEmpty.isVisible = true
                    return@loadAllUsers
                }
                clients.forEach(::addRow)
            },
            onError = { error ->
                if (_binding == null) return@loadAllUsers
                binding.adminEngagementLoading.isVisible = false
                binding.adminEngagementEmpty.text = JournalErrors.message(error, "load engagement")
                binding.adminEngagementEmpty.isVisible = true
            },
        )
    }

    // Rows go in by name straight away and each session count fills in when it arrives
    private fun addRow(user: UserProfile) {
        val row = ItemAdminEngagementBinding.inflate(layoutInflater, binding.adminEngagementContainer, false)
        row.adminEngagementDivider.isVisible = binding.adminEngagementContainer.childCount > 0
        row.adminEngagementName.text = nameOf(user)
        row.adminEngagementStreak.text = "—"
        row.adminEngagementSessions.text = "…"
        binding.adminEngagementContainer.addView(row.root)

        repository.loadAudioSessions(
            userId = user.userId,
            onSuccess = { count -> if (_binding != null) row.adminEngagementSessions.text = count.toString() },
            onError = { if (_binding != null) row.adminEngagementSessions.text = "—" },
        )
    }

    private fun nameOf(user: UserProfile) = user.displayName.ifBlank { user.email }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
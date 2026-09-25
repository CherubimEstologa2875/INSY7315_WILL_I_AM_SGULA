package com.example.insy7315_wil_.ui.screens.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.SessionManager
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.example.insy7315_wil_.databinding.FragmentSettingsBinding

// the switches are saved to firestore so they follow the account to any device
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        val session = SessionManager(requireContext())

        if (session.isLoggedIn) {
            // show the cached profile first, then correct it from firestore
            show(session.cachedProfile)
            loadProfile(session)
        } else {
            showGuest()
        }

        binding.settingsAdminUpload.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminUploadFragment)
        }
        binding.settingsAdminAffirmations.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminAffirmationsFragment)
        }
        binding.settingsAdminAccounts.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminAccountsFragment)
        }
        binding.settingsAdminEngagement.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminEngagementFragment)
        }

        binding.settingsLogout.setOnClickListener {
            repository.signOut()
            session.clear()
            findNavController().navigate(R.id.action_settingsFragment_to_launchFragment)
        }
    }

    private fun loadProfile(session: SessionManager) {
        val userId = repository.currentUserId() ?: return
        repository.loadUserProfile(userId)
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                if (!document.exists()) {
                    // registration made the account but not the document, so write it now
                    repository.saveUserProfile(session.cachedProfile.copy(userId = userId))
                    return@addOnSuccessListener
                }
                val profile = repository.toUserProfile(document)
                session.cache(profile)
                show(profile)
            }
    }

    private fun show(profile: UserProfile) {
        binding.settingsName.text = profile.displayName.ifBlank { "Sgula member" }
        binding.settingsEmail.text = profile.email
        binding.settingsAdminSection.isVisible = profile.isAdmin
        bindSwitches(profile)
    }

    private fun showGuest() {
        binding.settingsName.text = "Browsing as a guest"
        binding.settingsEmail.text = "Create an account to save your progress"
        binding.settingsAdminSection.isVisible = false
        binding.settingsAnonymousInsights.isVisible = false
        binding.settingsReminders.isVisible = false
        binding.settingsProgress.isVisible = false
        binding.settingsLogout.text = "Leave guest mode"
    }

    // take the listeners off first so setting the switches does not count as a change
    private fun bindSwitches(profile: UserProfile) {
        binding.settingsAnonymousInsights.setOnCheckedChangeListener(null)
        binding.settingsReminders.setOnCheckedChangeListener(null)
        binding.settingsProgress.setOnCheckedChangeListener(null)

        binding.settingsAnonymousInsights.isChecked = profile.sharesAnonymousInsights
        binding.settingsReminders.isChecked = profile.remindersEnabled
        binding.settingsProgress.isChecked = profile.activityProgressVisible

        val session = SessionManager(requireContext())
        binding.settingsAnonymousInsights.setOnCheckedChangeListener { _, checked ->
            session.sharesAnonymousInsights = checked
            saveSwitches(session)
        }
        binding.settingsReminders.setOnCheckedChangeListener { _, checked ->
            session.remindersEnabled = checked
            saveSwitches(session)
        }
        binding.settingsProgress.setOnCheckedChangeListener { _, checked ->
            session.activityProgressVisible = checked
            saveSwitches(session)
        }
    }

    private fun saveSwitches(session: SessionManager) {
        val userId = repository.currentUserId() ?: return
        repository.updateUserSettings(
            userId = userId,
            remindersEnabled = session.remindersEnabled,
            sharesAnonymousInsights = session.sharesAnonymousInsights,
            activityProgressVisible = session.activityProgressVisible,
        )
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

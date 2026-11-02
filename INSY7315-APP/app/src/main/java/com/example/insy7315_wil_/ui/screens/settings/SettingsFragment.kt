package com.example.insy7315_wil_.ui.screens.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.SessionManager
import com.example.insy7315_wil_.data.`Data classes`.AuthValidation
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.example.insy7315_wil_.data.`Data classes`.errorMessage
import com.example.insy7315_wil_.databinding.FragmentSettingsBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors

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
        binding.settingsAdminBroadcasts.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminBroadcastsFragment)
        }
        binding.settingsAdminAccounts.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminAccountsFragment)
        }
        binding.settingsAdminEngagement.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_adminEngagementFragment)
        }

        binding.settingsSaveName.setOnClickListener { saveName(session) }

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
        binding.settingsDisplayName.text = profile.displayName
        binding.settingsAdminSection.isVisible = profile.isAdmin
    }

    private fun showGuest() {
        binding.settingsName.text = "Browsing as a guest"
        binding.settingsEmail.text = "Create an account to save your progress"
        binding.settingsAdminSection.isVisible = false
        binding.settingsProfileSection.isVisible = false
        binding.settingsLogout.text = "Leave guest mode"
    }

    private fun saveName(session: SessionManager) {
        val userId = repository.currentUserId() ?: return
        val name = AuthValidation.sanitise(binding.settingsDisplayName.text, AuthValidation.MAX_NAME_LENGTH)
        binding.settingsDisplayName.error = AuthValidation.displayName(name).errorMessage()
        if (binding.settingsDisplayName.error != null) return

        binding.settingsSaveName.isEnabled = false
        repository.updateDisplayName(userId, name)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                binding.settingsSaveName.isEnabled = true
                session.cache(session.cachedProfile.copy(displayName = name))
                show(session.cachedProfile)
                Toast.makeText(requireContext(), "Name saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                binding.settingsSaveName.isEnabled = true
                binding.settingsDisplayName.error = JournalErrors.message(error, "save your name")
            }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

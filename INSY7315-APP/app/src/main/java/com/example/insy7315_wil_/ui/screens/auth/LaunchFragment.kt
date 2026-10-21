package com.example.insy7315_wil_.ui.screens.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.SessionManager
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository

class LaunchFragment : Fragment(R.layout.fragment_launch) {
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        if (session.isGuest) {
            findNavController().navigate(R.id.action_launchFragment_to_guestLandingFragment)
            return
        }

        val login = view.findViewById<View>(R.id.launch_login)
        val guest = view.findViewById<View>(R.id.launch_guest)
        login.setOnClickListener {
            findNavController().navigate(R.id.action_launchFragment_to_loginFragment)
        }
        guest.setOnClickListener {
            session.continueAsGuest()
            findNavController().navigate(R.id.action_launchFragment_to_guestLandingFragment)
        }

        if (session.isLoggedIn) {
            login.isVisible = false
            guest.isVisible = false
            checkStillActive(session, login, guest)
        }
    }
    
    private fun checkStillActive(session: SessionManager, login: View, guest: View) {
        val userId = repository.currentUserId() ?: return
        repository.loadUserProfile(userId)
            .addOnSuccessListener { document ->
                if (view == null) return@addOnSuccessListener
                if (document.exists() && !repository.toUserProfile(document).active) {
                    repository.signOut()
                    session.clear()
                    login.isVisible = true
                    guest.isVisible = true
                    Toast.makeText(requireContext(), AuthErrors.DEACTIVATED, Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                findNavController().navigate(R.id.action_launchFragment_to_homeFragment)
            }
            .addOnFailureListener {
                // most likely offline, and a member shouldn't be locked out for that
                if (view == null) return@addOnFailureListener
                findNavController().navigate(R.id.action_launchFragment_to_homeFragment)
            }
    }
}

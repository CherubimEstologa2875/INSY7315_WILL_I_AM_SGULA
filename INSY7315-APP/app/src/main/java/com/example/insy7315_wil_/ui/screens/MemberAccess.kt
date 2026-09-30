package com.example.insy7315_wil_.ui.screens

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.SessionManager

fun Fragment.redirectGuestFromMemberContent(): Boolean {
    if (!SessionManager(requireContext()).isGuest) return false
    findNavController().navigate(R.id.guestLandingFragment)
    return true
}

// the admin screens can still be reached by id, so check the role here as well
fun Fragment.redirectNonAdminFromAdminContent(): Boolean {
    val session = SessionManager(requireContext())
    if (session.isLoggedIn && session.isAdmin) return false
    findNavController().navigate(if (session.isLoggedIn) R.id.homeFragment else R.id.launchFragment)
    return true
}

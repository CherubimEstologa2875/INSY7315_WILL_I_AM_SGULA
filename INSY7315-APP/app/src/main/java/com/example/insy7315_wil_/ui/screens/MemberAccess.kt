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

package com.example.insy7315_wil_.ui.screens.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.SessionManager
import com.example.insy7315_wil_.databinding.FragmentGuestLandingBinding

class GuestLandingFragment : Fragment(R.layout.fragment_guest_landing) {
    private var _binding: FragmentGuestLandingBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGuestLandingBinding.bind(view)
        binding.guestCreateAccount.setOnClickListener { findNavController().navigate(R.id.action_guestLandingFragment_to_registerFragment) }
        binding.guestBrowseAudio.setOnClickListener {
            SessionManager(requireContext()).continueAsGuest()
            findNavController().navigate(R.id.action_guestLandingFragment_to_audioLibraryFragment)
        }
        binding.guestBroadcast.setOnClickListener {
            SessionManager(requireContext()).continueAsGuest()
            findNavController().navigate(R.id.action_guestLandingFragment_to_broadcastFragment)
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

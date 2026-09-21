package com.example.insy7315_wil_.ui.screens.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.AuthValidation
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.errorMessage
import com.example.insy7315_wil_.databinding.FragmentPasswordResetBinding

// sends a firebase reset email, an unknown address gets the same confirmation
class PasswordResetFragment : Fragment(R.layout.fragment_password_reset) {
    private var _binding: FragmentPasswordResetBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var sending = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPasswordResetBinding.bind(view)

        binding.passwordResetSend.setOnClickListener { send() }
        binding.passwordResetLogin.setOnClickListener {
            findNavController().navigate(R.id.action_passwordResetFragment_to_loginFragment)
        }
    }

    private fun send() {
        if (sending) return
        val email = AuthValidation.sanitise(binding.passwordResetEmail.text, AuthValidation.MAX_EMAIL_LENGTH).lowercase()
        binding.passwordResetEmail.error = AuthValidation.email(email).errorMessage()
        showError(null)
        if (binding.passwordResetEmail.error != null) return

        setSending(true)
        repository.sendPasswordReset(email)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                showConfirmation()
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                if (AuthErrors.resetCanBeTreatedAsSent(error)) {
                    showConfirmation()
                    return@addOnFailureListener
                }
                setSending(false)
                showError(AuthErrors.passwordReset(error))
            }
    }

    private fun showConfirmation() {
        binding.passwordResetForm.isVisible = false
        binding.passwordResetConfirmation.isVisible = true
    }

    private fun setSending(value: Boolean) {
        sending = value
        binding.passwordResetSend.isEnabled = !value
        binding.passwordResetSend.text = if (value) "Sending..." else "Send reset link"
    }

    private fun showError(message: String?) {
        binding.passwordResetError.isVisible = !message.isNullOrBlank()
        binding.passwordResetError.text = message
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

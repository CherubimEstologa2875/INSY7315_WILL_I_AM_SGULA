package com.example.insy7315_wil_.ui.screens.auth

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.SessionManager
import com.example.insy7315_wil_.data.`Data classes`.AuthValidation
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.example.insy7315_wil_.data.`Data classes`.errorMessage
import com.example.insy7315_wil_.databinding.FragmentRegisterBinding
import com.example.insy7315_wil_.ui.widget.SgulaPasswordStrengthView

// creates the firebase account and its UserProfile document, always as a member
class RegisterFragment : Fragment(R.layout.fragment_register) {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var registering = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.registerPassword.doOnTextChanged { password ->
            binding.registerPasswordStrength.strength = when {
                password.length >= 12 && password.any(Char::isDigit) && password.any(Char::isLetter) ->
                    SgulaPasswordStrengthView.Strength.STRONG
                AuthValidation.newPassword(password).errorMessage() == null -> SgulaPasswordStrengthView.Strength.MEDIUM
                else -> SgulaPasswordStrengthView.Strength.WEAK
            }
        }
        binding.registerButton.setOnClickListener { register() }
        binding.registerLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun register() {
        if (registering) return
        val name = AuthValidation.sanitise(binding.registerName.text, AuthValidation.MAX_NAME_LENGTH)
        val email = AuthValidation.sanitise(binding.registerEmail.text, AuthValidation.MAX_EMAIL_LENGTH).lowercase()
        val password = binding.registerPassword.text
        binding.registerName.error = AuthValidation.displayName(name).errorMessage()
        binding.registerEmail.error = AuthValidation.email(email).errorMessage()
        binding.registerPassword.error = AuthValidation.newPassword(password).errorMessage()
        showError(null)
        if (binding.registerName.error != null ||
            binding.registerEmail.error != null ||
            binding.registerPassword.error != null
        ) {
            return
        }

        setRegistering(true)
        repository.register(email, password)
            .addOnSuccessListener { credential ->
                if (_binding == null) return@addOnSuccessListener
                val userId = credential.user?.uid
                if (userId == null) {
                    setRegistering(false)
                    showError(AuthErrors.register(null))
                    return@addOnSuccessListener
                }
                saveProfile(UserProfile(userId = userId, email = email, displayName = name))
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                setRegistering(false)
                showError(AuthErrors.register(error))
            }
    }

    private fun saveProfile(profile: UserProfile) {
        val session = SessionManager(requireContext())
        repository.saveUserProfile(profile)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                session.cache(profile)
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                // the account exists now, so let them in and settings writes the profile again
                session.cache(profile)
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
    }

    private fun setRegistering(value: Boolean) {
        registering = value
        binding.registerButton.isEnabled = !value
        binding.registerButton.text = if (value) "Creating account..." else "Create account"
    }

    private fun showError(message: String?) {
        binding.registerError.isVisible = !message.isNullOrBlank()
        binding.registerError.text = message
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

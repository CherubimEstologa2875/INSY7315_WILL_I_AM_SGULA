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
import com.example.insy7315_wil_.data.`Data classes`.errorMessage
import com.example.insy7315_wil_.databinding.FragmentLoginBinding

// signs in with firebase and caches the profile so the next screen knows the role
class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var signingIn = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        binding.loginButton.setOnClickListener { signIn() }
        binding.loginForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_passwordResetFragment)
        }
        binding.loginGuestButton.setOnClickListener {
            SessionManager(requireContext()).continueAsGuest()
            findNavController().navigate(R.id.action_loginFragment_to_guestLandingFragment)
        }
        binding.loginRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun signIn() {
        if (signingIn) return
        val email = AuthValidation.sanitise(binding.loginEmail.text, AuthValidation.MAX_EMAIL_LENGTH).lowercase()
        val password = binding.loginPassword.text
        binding.loginEmail.error = AuthValidation.email(email).errorMessage()
        binding.loginPassword.error = AuthValidation.existingPassword(password).errorMessage()
        showError(null)
        if (binding.loginEmail.error != null || binding.loginPassword.error != null) return

        setSigningIn(true)
        repository.signIn(email, password)
            .addOnSuccessListener { credential ->
                if (_binding == null) return@addOnSuccessListener
                val userId = credential.user?.uid
                if (userId == null) {
                    setSigningIn(false)
                    showError(AuthErrors.signIn(null))
                    return@addOnSuccessListener
                }
                cacheProfileThenContinue(userId, email)
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                setSigningIn(false)
                showError(AuthErrors.signIn(error))
            }
    }

    // read the profile before home opens so the admin section shows for the right people
    private fun cacheProfileThenContinue(userId: String, email: String) {
        val session = SessionManager(requireContext())
        repository.loadUserProfile(userId)
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                session.cache(
                    if (document.exists()) repository.toUserProfile(document)
                    else session.cachedProfile.copy(userId = userId, email = email),
                )
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                // the sign in worked, so a failed profile read should not block the member
                session.cache(session.cachedProfile.copy(userId = userId, email = email))
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
    }

    private fun setSigningIn(value: Boolean) {
        signingIn = value
        binding.loginButton.isEnabled = !value
        binding.loginButton.text = if (value) "Signing in..." else "Log in"
        binding.loginGuestButton.isEnabled = !value
    }

    private fun showError(message: String?) {
        binding.loginError.isVisible = !message.isNullOrBlank()
        binding.loginError.text = message
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

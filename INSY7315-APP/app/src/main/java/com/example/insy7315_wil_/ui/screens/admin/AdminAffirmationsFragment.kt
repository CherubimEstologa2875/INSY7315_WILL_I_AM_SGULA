package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.fragment.app.Fragment

import com.google.android.material.button.MaterialButton

import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent
import com.example.insy7315_wil_.ui.widget.SgulaTextFieldView
class AdminAffirmationsFragment : Fragment(R.layout.fragment_admin_affirmations) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = FirebaseWellnessRepository()

        repository.checkAdmin(
            onResult = { isAdmin ->
                if (!isAdmin) {
                    redirectNonAdminFromAdminContent()
                } else {
                    setupAffirmationScreen(view)
                }
            },
            onError = {
                redirectNonAdminFromAdminContent()
            }
        )
    }
    private fun setupAffirmationScreen(view: View) {
        val affirmationField =
            view.findViewById<SgulaTextFieldView>(
                R.id.admin_affirmation_text
            )

        val addButton =
            view.findViewById<MaterialButton>(
                R.id.admin_affirmation_button
            )

        val repository = FirebaseWellnessRepository()

        addButton.setOnClickListener {

            if (affirmationField.text.isBlank()) {
                affirmationField.error = "Affirmation is required"
                return@setOnClickListener
            }

            addButton.isEnabled = false

            repository.addAffirmation(
                text = affirmationField.text.trim(),
                onSuccess = {

                    addButton.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Affirmation added",
                        Toast.LENGTH_SHORT
                    ).show()

                    affirmationField.text = ""
                },
                onError = { error ->

                    addButton.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Couldn't add affirmation: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.net.Uri
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

import com.google.android.material.button.MaterialButton

import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.MeditationCategory
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent
import com.example.insy7315_wil_.ui.widget.SgulaDropdownFieldView
import com.example.insy7315_wil_.ui.widget.SgulaTextFieldView
class AdminUploadFragment : Fragment(R.layout.fragment_admin_upload) {

    private var selectedAudioUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = FirebaseWellnessRepository()

        repository.checkAdmin(
            onResult = { isAdmin ->
                if (!isAdmin) {
                    redirectNonAdminFromAdminContent()
                } else {
                    setupUploadScreen(view)
                }
            },
            onError = {
                redirectNonAdminFromAdminContent()
            }
        )
    }
    private val audioPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            selectedAudioUri = uri

            view?.findViewById<TextView>(
                R.id.admin_upload_file_name
            )?.text =
                uri?.lastPathSegment ?: "No file selected"
        }
    private fun setupUploadScreen(view: View) {
        val chooseButton =
            view.findViewById<MaterialButton>(
                R.id.admin_upload_choose_file
            )

        chooseButton.setOnClickListener {
            audioPicker.launch("audio/*")
        }
        val titleField =
            view.findViewById<SgulaTextFieldView>(
                R.id.admin_upload_title
            )

        val categoryField =
            view.findViewById<SgulaDropdownFieldView>(
                R.id.admin_upload_category
            )

        val wellnessField =
            view.findViewById<SgulaDropdownFieldView>(
                R.id.admin_upload_wellness_category
            )

        val repository = FirebaseWellnessRepository()
        var categories: List<MeditationCategory> = emptyList()

        wellnessField.onSelect = { wellnessField.error = null }

        repository.loadMeditationCategories()
            .addOnSuccessListener { snapshot ->
                categories = snapshot.documents
                    .map(repository::toMeditationCategory)
                    .sortedBy { it.name }
                wellnessField.setOptions(categories.map { it.name })
            }
            .addOnFailureListener {
                wellnessField.error = "Couldn't load the categories"
            }

        val uploadButton =
            view.findViewById<MaterialButton>(
                R.id.admin_upload_button
            )

        uploadButton.setOnClickListener {

            val uri = selectedAudioUri

            if (uri == null) {
                Toast.makeText(
                    requireContext(),
                    "Please choose an audio file",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (titleField.text.isBlank()) {
                titleField.error = "Title is required"
                return@setOnClickListener
            }

            if (categoryField.selection.isNullOrBlank()) {
                categoryField.error = "Category is required"
                return@setOnClickListener
            }

            val wellnessCategory =
                categories.firstOrNull { it.name == wellnessField.selection }

            if (wellnessCategory == null) {
                wellnessField.error = "Wellness category is required"
                return@setOnClickListener
            }

            uploadButton.isEnabled = false

            repository.uploadAudioContent(
                uri = uri,
                title = titleField.text.trim(),
                category = categoryField.selection.orEmpty(),
                categoryId = wellnessCategory.categoryId,
                onSuccess = {

                    uploadButton.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Audio uploaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    titleField.text = ""
                    selectedAudioUri = null

                    view.findViewById<TextView>(
                        R.id.admin_upload_file_name
                    ).text = "No file selected"
                },
                onError = { error ->

                    uploadButton.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Upload failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}

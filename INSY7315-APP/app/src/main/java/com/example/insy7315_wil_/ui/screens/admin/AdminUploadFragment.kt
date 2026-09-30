package com.example.insy7315_wil_.ui.screens.admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.MeditationCategory
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent
import com.example.insy7315_wil_.ui.widget.SgulaDropdownFieldView
import com.example.insy7315_wil_.ui.widget.SgulaTextFieldView

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminUploadFragment : Fragment(R.layout.fragment_admin_upload) {

    private var selectedAudioUri: Uri? = null

    private val repository by lazy {
        FirebaseWellnessRepository()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        repository.checkAdmin(
            onResult = { isAdmin ->

                if (!isAdmin) {
                    redirectNonAdminFromAdminContent()
                } else {
                    setupUploadScreen(view)
                    loadUploadedAudio(view)
                }
            },

            onError = {
                redirectNonAdminFromAdminContent()
            }
        )
    }

    // AUDIO FILE PICKER

    private val audioPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                selectedAudioUri = uri

                view?.findViewById<TextView>(
                    R.id.admin_upload_file_name
                )?.text = getFileName(uri)

            } else {

                selectedAudioUri = null

                view?.findViewById<TextView>(
                    R.id.admin_upload_file_name
                )?.text = "No file selected"
            }
        }

    private fun getFileName(uri: Uri): String {

        var fileName = "Selected audio"

        requireContext()
            .contentResolver
            .query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val nameIndex =
                        cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                    if (nameIndex >= 0) {
                        fileName =
                            cursor.getString(nameIndex)
                    }
                }
            }

        return fileName
    }

    // SETUP ADMIN SCREEN

    private fun setupUploadScreen(view: View) {

        // --------------------------------------------------------
        // CHOOSE FILE
        // --------------------------------------------------------

        val chooseButton =
            view.findViewById<MaterialButton>(
                R.id.admin_upload_choose_file
            )

        chooseButton.setOnClickListener {
            audioPicker.launch("audio/*")
        }

        // --------------------------------------------------------
        // TITLE
        // --------------------------------------------------------

        val titleField =
            view.findViewById<SgulaTextFieldView>(
                R.id.admin_upload_title
            )

        // --------------------------------------------------------
        // MAIN CATEGORY
        // --------------------------------------------------------

        val categoryField =
            view.findViewById<SgulaDropdownFieldView>(
                R.id.admin_upload_category
            )

        categoryField.onSelect = {
            categoryField.error = null
        }

        // --------------------------------------------------------
        // WELLNESS CATEGORY
        // --------------------------------------------------------

        val wellnessField =
            view.findViewById<SgulaDropdownFieldView>(
                R.id.admin_upload_wellness_category
            )

        wellnessField.onSelect = {
            wellnessField.error = null
        }

        // --------------------------------------------------------
        // BROADCAST DATE
        // --------------------------------------------------------

        val broadcastDateField =
            view.findViewById<TextInputEditText>(
                R.id.admin_upload_broadcast_date
            )

        // --------------------------------------------------------
        // LOAD WELLNESS CATEGORIES
        // --------------------------------------------------------

        var wellnessCategories: List<MeditationCategory> =
            emptyList()

        repository.loadMeditationCategories()
            .addOnSuccessListener { snapshot ->

                wellnessCategories =
                    snapshot.documents
                        .map(repository::toMeditationCategory)
                        .sortedBy { it.name }

                if (wellnessCategories.isEmpty()) {

                    wellnessField.setOptions(
                        listOf("No categories available")
                    )

                } else {

                    wellnessField.setOptions(
                        wellnessCategories.map {
                            it.name
                        }
                    )
                }
            }
            .addOnFailureListener {

                wellnessField.error =
                    "Couldn't load the categories"
            }

        // --------------------------------------------------------
        // DATE PICKER
        // --------------------------------------------------------

        broadcastDateField.setOnClickListener {

            val calendar =
                Calendar.getInstance()

            val datePicker =
                DatePickerDialog(
                    requireContext(),

                    { _, year, month, dayOfMonth ->

                        val selectedDate =
                            Calendar.getInstance().apply {

                                set(
                                    Calendar.YEAR,
                                    year
                                )

                                set(
                                    Calendar.MONTH,
                                    month
                                )

                                set(
                                    Calendar.DAY_OF_MONTH,
                                    dayOfMonth
                                )

                                set(
                                    Calendar.HOUR_OF_DAY,
                                    0
                                )

                                set(
                                    Calendar.MINUTE,
                                    0
                                )

                                set(
                                    Calendar.SECOND,
                                    0
                                )

                                set(
                                    Calendar.MILLISECOND,
                                    0
                                )
                            }

                        val dateFormat =
                            SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                            )

                        broadcastDateField.setText(
                            dateFormat.format(
                                selectedDate.time
                            )
                        )
                    },

                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

            datePicker.show()
        }

        // --------------------------------------------------------
        // UPLOAD BUTTON
        // --------------------------------------------------------

        val uploadButton =
            view.findViewById<MaterialButton>(
                R.id.admin_upload_button
            )

        uploadButton.setOnClickListener {

            // ----------------------------------------------------
            // AUDIO FILE VALIDATION
            // ----------------------------------------------------

            val uri = selectedAudioUri

            if (uri == null) {

                Toast.makeText(
                    requireContext(),
                    "Please choose an audio file",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ----------------------------------------------------
            // TITLE VALIDATION
            // ----------------------------------------------------

            val title =
                titleField.text.trim()

            if (title.isBlank()) {

                titleField.error =
                    "Title is required"

                return@setOnClickListener
            }

            // ----------------------------------------------------
            // MAIN CATEGORY VALIDATION
            // ----------------------------------------------------

            val selectedMainCategory =
                categoryField.selection
                    ?.trim()
                    .orEmpty()

            if (selectedMainCategory.isBlank()) {

                categoryField.error =
                    "Category is required"

                return@setOnClickListener
            }

            // ----------------------------------------------------
            // WELLNESS CATEGORY VALIDATION
            // ----------------------------------------------------

            val selectedWellnessCategory =
                wellnessCategories.firstOrNull {
                    it.name == wellnessField.selection
                }

            if (selectedWellnessCategory == null) {

                wellnessField.error =
                    "Wellness category is required"

                return@setOnClickListener
            }

            // ----------------------------------------------------
            // DATE VALIDATION
            // ----------------------------------------------------

            val broadcastDate =
                broadcastDateField.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (broadcastDate.isBlank()) {

                broadcastDateField.error =
                    "Broadcast date is required"

                return@setOnClickListener
            }

            // ----------------------------------------------------
            // DISABLE BUTTON
            // ----------------------------------------------------

            uploadButton.isEnabled = false
            uploadButton.text = "Uploading..."

            // ----------------------------------------------------
            // UPLOAD
            // ----------------------------------------------------

            repository.uploadAudioContent(

                uri = uri,

                title = title,

                category = selectedMainCategory,

                categoryId =
                    selectedWellnessCategory.categoryId,

                broadcastDate = broadcastDate,

                onSuccess = {

                    if (!isAdded) return@uploadAudioContent

                    uploadButton.isEnabled = true
                    uploadButton.text = "Upload audio"

                    Toast.makeText(
                        requireContext(),
                        "Audio uploaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // --------------------------------------------
                    // CLEAR FORM
                    // --------------------------------------------

                    titleField.text = ""

                    selectedAudioUri = null

                    broadcastDateField.setText("")

                    view.findViewById<TextView>(
                        R.id.admin_upload_file_name
                    ).text = "No file selected"

                    // --------------------------------------------
                    // REFRESH UPLOADED AUDIO
                    // --------------------------------------------

                    loadUploadedAudio(view)
                },

                onError = { error ->

                    if (!isAdded) return@uploadAudioContent

                    uploadButton.isEnabled = true
                    uploadButton.text = "Upload audio"

                    Toast.makeText(
                        requireContext(),
                        "Upload failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    // LOAD UPLOADED AUDIO

    private fun loadUploadedAudio(view: View) {

        val audioList =
            view.findViewById<LinearLayout>(
                R.id.admin_audio_list
            )

        val loading =
            view.findViewById<View>(
                R.id.admin_audio_loading
            )

        val empty =
            view.findViewById<TextView>(
                R.id.admin_audio_empty
            )

        loading.visibility = View.VISIBLE
        empty.visibility = View.GONE

        audioList.removeAllViews()

        repository.loadAudioContent()

            .addOnSuccessListener { snapshot ->

                if (!isAdded) return@addOnSuccessListener

                loading.visibility = View.GONE

                if (snapshot.isEmpty) {

                    empty.text =
                        "No audio has been uploaded yet."

                    empty.visibility = View.VISIBLE

                    return@addOnSuccessListener
                }

                snapshot.documents.forEach { document ->

                    val audio =
                        repository.toAudioContent(
                            document
                        )

                    val row =
                        LinearLayout(
                            requireContext()
                        ).apply {

                            orientation =
                                LinearLayout.HORIZONTAL

                            gravity =
                                Gravity.CENTER_VERTICAL

                            setPadding(
                                0,
                                12,
                                0,
                                12
                            )
                        }

   
                    // INFORMATION CONTAINER
                    val info =
                        LinearLayout(
                            requireContext()
                        ).apply {

                            orientation =
                                LinearLayout.VERTICAL

                            layoutParams =
                                LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    1f
                                )
                        }

   
                    // TITLE
                    val titleText =
                        TextView(
                            requireContext()
                        ).apply {

                            text =
                                if (audio.title.isBlank()) {
                                    "Untitled audio"
                                } else {
                                    audio.title
                                }

                            textSize = 16f
                        }

   
                    // CATEGORY
                    val categoryText =
                        TextView(
                            requireContext()
                        ).apply {

                            text =
                                if (audio.category.isBlank()) {
                                    "No category"
                                } else {
                                    audio.category
                                }

                            textSize = 13f
                        }

   
                    // ADD TEXT
                    info.addView(titleText)
                    info.addView(categoryText)

   
                    // DELETE BUTTON
                    val deleteButton =
                        MaterialButton(
                            requireContext()
                        ).apply {

                            text = "Delete"

                            setOnClickListener {

                                confirmDeleteAudio(
                                    view = view,
                                    audioId =
                                        audio.audioId,
                                    title =
                                        audio.title,
                                    storagePath =
                                        audio.storagePath
                                )
                            }
                        }

                    row.addView(info)
                    row.addView(deleteButton)

                    audioList.addView(row)
                }
            }

            .addOnFailureListener { error ->

                if (!isAdded) return@addOnFailureListener

                loading.visibility = View.GONE

                empty.text =
                    "Could not load uploaded audio."

                empty.visibility = View.VISIBLE

                Toast.makeText(
                    requireContext(),
                    "Could not load audio: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // DELETE CONFIRMATION

    private fun confirmDeleteAudio(
        view: View,
        audioId: String,
        title: String,
        storagePath: String
    ) {

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Delete audio?")

            .setMessage(
                "Are you sure you want to permanently delete \"$title\"?"
            )

            .setNegativeButton(
                "Cancel",
                null
            )

            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteAudio(
                    view = view,
                    audioId = audioId,
                    title = title,
                    storagePath = storagePath
                )
            }

            .show()
    }

    // DELETE AUDIO

    private fun deleteAudio(
        view: View,
        audioId: String,
        title: String,
        storagePath: String
    ) {

        Toast.makeText(
            requireContext(),
            "Deleting $title...",
            Toast.LENGTH_SHORT
        ).show()

        repository.deleteAudioContent(

            audioId = audioId,

            storagePath = storagePath,

            onSuccess = {

                if (!isAdded) return@deleteAudioContent

                Toast.makeText(
                    requireContext(),
                    "Audio deleted",
                    Toast.LENGTH_SHORT
                ).show()

                loadUploadedAudio(view)
            },

            onError = { error ->

                if (!isAdded) return@deleteAudioContent

                Toast.makeText(
                    requireContext(),
                    "Delete failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // CLEANUP

    override fun onDestroyView() {
        super.onDestroyView()
        selectedAudioUri = null
    }
}
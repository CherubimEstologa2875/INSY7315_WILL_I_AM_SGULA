package com.example.insy7315_wil_.ui.screens.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.Broadcast
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.SessionManager
import com.example.insy7315_wil_.databinding.FragmentAdminBroadcastsBinding
import com.example.insy7315_wil_.databinding.ItemAdminBroadcastBinding
import com.example.insy7315_wil_.ui.screens.audio.BroadcastSchedule
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent
import com.example.insy7315_wil_.ui.widget.SgulaModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The practice records a broadcast, writes a line about it and picks when it should reach
 * clients. The recording itself goes to Cloud Storage; only the text and the timing are
 * kept in Firestore.
 */
class AdminBroadcastsFragment : Fragment(R.layout.fragment_admin_broadcasts) {

    private var _binding: FragmentAdminBroadcastsBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()

    private var selectedRecording: Uri? = null
    private var uploading = false
    private val scheduledFor: Calendar = Calendar.getInstance()

    // The picker hands back a content:// uri that the upload can read straight from.
    // GetContent gives us whichever file the therapist picked on her phone (Android Developers, 2026).
    private val recordingPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (_binding == null) return@registerForActivityResult
        selectedRecording = uri
        binding.adminBroadcastFileName.text = uri?.let(::fileNameOf) ?: "No recording selected"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminBroadcastsBinding.bind(view)

        // This screen can still be reached by id, so check the role here as well.
        repository.checkAdmin(
            onResult = { isAdmin ->
                if (_binding == null) return@checkAdmin
                if (isAdmin) setupScreen() else redirectNonAdminFromAdminContent()
            },
            onError = { redirectNonAdminFromAdminContent() },
        )
    }

    private fun setupScreen() {
        scheduledFor.set(Calendar.SECOND, 0)
        scheduledFor.set(Calendar.MILLISECOND, 0)
        showSchedule()

        // Start with the signed-in admin's own name; the therapist can type a different one.
        if (binding.adminBroadcastAuthor.text.isBlank()) {
            binding.adminBroadcastAuthor.text = SessionManager(requireContext()).displayName
        }

        binding.adminBroadcastChooseFile.setOnClickListener { recordingPicker.launch("audio/*") }
        binding.adminBroadcastPickDate.setOnClickListener { pickDate() }
        binding.adminBroadcastPickTime.setOnClickListener { pickTime() }
        binding.adminBroadcastUpload.setOnClickListener { confirmUpload() }
        loadBroadcasts()
    }

    private fun fileNameOf(uri: Uri): String {
        var name = "Selected recording"
        requireContext().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
            }
        return name
    }

    // The date and time pickers decide when the recording goes live (Android Developers, 2026; Oracle, 2026).
    private fun pickDate() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                scheduledFor.set(Calendar.YEAR, year)
                scheduledFor.set(Calendar.MONTH, month)
                scheduledFor.set(Calendar.DAY_OF_MONTH, day)
                showSchedule()
            },
            scheduledFor.get(Calendar.YEAR),
            scheduledFor.get(Calendar.MONTH),
            scheduledFor.get(Calendar.DAY_OF_MONTH),
        ).apply { datePicker.minDate = System.currentTimeMillis() - DAY_MILLIS }.show()
    }

    private fun pickTime() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                scheduledFor.set(Calendar.HOUR_OF_DAY, hour)
                scheduledFor.set(Calendar.MINUTE, minute)
                showSchedule()
            },
            scheduledFor.get(Calendar.HOUR_OF_DAY),
            scheduledFor.get(Calendar.MINUTE),
            true,
        ).show()
    }

    private fun showSchedule() {
        binding.adminBroadcastScheduleValue.text = SCHEDULE_FORMAT.format(scheduledFor.time)
        binding.adminBroadcastScheduleNote.text = if (scheduledFor.timeInMillis <= System.currentTimeMillis()) {
            "This goes live as soon as it finishes uploading."
        } else {
            "Clients see this listed as coming up until then."
        }
    }

    private fun confirmUpload() {
        if (uploading) return
        val recording = selectedRecording
        val title = binding.adminBroadcastTitle.text.trim()
        val message = binding.adminBroadcastMessage.text.trim()
        val author = binding.adminBroadcastAuthor.text.trim()

        if (recording == null) {
            showStatus("Choose a recording first.", isError = true)
            return
        }
        binding.adminBroadcastTitle.error = if (title.isBlank()) "A title is required" else null
        if (title.isBlank()) return

        SgulaModal.show(
            context = requireContext(),
            title = "Schedule this broadcast?",
            body = "\"$title\" will reach clients on ${SCHEDULE_FORMAT.format(scheduledFor.time)}.",
            confirmText = "Schedule it",
            onConfirm = { upload(recording, title, message, author) },
            dismissText = "Not yet",
            icon = "?",
        )
    }

    // Upload to Cloud Storage, reporting progress as it goes so the screen isn't silent (Firebase, n.d.).
    private fun upload(recording: Uri, title: String, message: String, author: String) {
        setUploading(true)
        repository.uploadBroadcast(
            uri = recording,
            title = title,
            message = message,
            author = author,
            publishAtMillis = scheduledFor.timeInMillis,
            onProgress = { percent ->
                if (_binding != null) showStatus("Uploading $percent%")
            },
            onSuccess = {
                if (_binding == null) return@uploadBroadcast
                setUploading(false)
                showStatus("Broadcast scheduled for ${SCHEDULE_FORMAT.format(scheduledFor.time)}.")
                selectedRecording = null
                binding.adminBroadcastFileName.text = "No recording selected"
                binding.adminBroadcastTitle.text = ""
                binding.adminBroadcastMessage.text = ""
                loadBroadcasts()
            },
            onError = { error ->
                if (_binding == null) return@uploadBroadcast
                setUploading(false)
                showStatus(JournalErrors.message(error, "schedule this broadcast"), isError = true)
            },
        )
    }

    private fun setUploading(value: Boolean) {
        uploading = value
        binding.adminBroadcastUpload.isEnabled = !value
        binding.adminBroadcastUpload.text = if (value) "Uploading..." else "Schedule broadcast"
        binding.adminBroadcastChooseFile.isEnabled = !value
        binding.adminBroadcastProgress.isVisible = value
        binding.adminBroadcastProgress.isIndeterminate = false
        if (value) binding.adminBroadcastProgress.progress = 0
    }

    private fun showStatus(message: String?, isError: Boolean = false) {
        binding.adminBroadcastStatus.isVisible = !message.isNullOrBlank()
        binding.adminBroadcastStatus.text = message
        binding.adminBroadcastStatus.setTextColor(
            ContextCompat.getColor(requireContext(), if (isError) R.color.sgula_danger else R.color.sgula_ink_700),
        )
        val percent = message?.substringAfter("Uploading ", "")?.removeSuffix("%")?.toIntOrNull()
        if (percent != null) binding.adminBroadcastProgress.progress = percent
    }

    private fun loadBroadcasts() {
        binding.adminBroadcastListLoading.isVisible = true
        binding.adminBroadcastListEmpty.isVisible = false
        binding.adminBroadcastList.removeAllViews()

        repository.loadBroadcasts()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                binding.adminBroadcastListLoading.isVisible = false
                val broadcasts = snapshot.documents.map(repository::toBroadcast)
                // What goes out next sits at the top, then everything already published.
                val ordered = BroadcastSchedule.upcoming(broadcasts) + BroadcastSchedule.published(broadcasts)
                if (ordered.isEmpty()) {
                    binding.adminBroadcastListEmpty.text = "No broadcasts have been scheduled yet."
                    binding.adminBroadcastListEmpty.isVisible = true
                    return@addOnSuccessListener
                }
                ordered.forEach(::addRow)
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                binding.adminBroadcastListLoading.isVisible = false
                binding.adminBroadcastListEmpty.text = JournalErrors.message(error, "load the broadcasts")
                binding.adminBroadcastListEmpty.isVisible = true
            }
    }

    // One Material card per broadcast, with its status and a way to remove it (Material Components for Android, n.d.).
    private fun addRow(broadcast: Broadcast) {
        val item = ItemAdminBroadcastBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding.adminBroadcastList,
            false,
        )
        val published = BroadcastSchedule.isPublished(broadcast)
        item.adminBroadcastItemDate.text = BroadcastSchedule.dateLabel(broadcast)
        item.adminBroadcastItemStatus.text = if (published) "Published" else "Scheduled"
        item.adminBroadcastItemTitle.text = broadcast.title
        item.adminBroadcastItemMessage.text = broadcast.message
        item.adminBroadcastItemMessage.isVisible = broadcast.message.isNotBlank()
        item.adminBroadcastItemDelete.setOnClickListener { confirmDelete(broadcast) }
        binding.adminBroadcastList.addView(item.root)
    }

    private fun confirmDelete(broadcast: Broadcast) {
        SgulaModal.show(
            context = requireContext(),
            title = "Delete this broadcast?",
            body = "\"${broadcast.title}\" and its recording will be removed for everyone. This can't be undone.",
            confirmText = "Delete",
            onConfirm = {
                repository.deleteBroadcast(
                    broadcastId = broadcast.broadcastId,
                    storagePath = broadcast.storagePath,
                    onSuccess = {
                        if (_binding == null) return@deleteBroadcast
                        showStatus("Broadcast deleted.")
                        loadBroadcasts()
                    },
                    onError = { error ->
                        if (_binding == null) return@deleteBroadcast
                        showStatus(JournalErrors.message(error, "delete this broadcast"), isError = true)
                    },
                )
            },
            dismissText = "Cancel",
            destructive = true,
            icon = "!",
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        val SCHEDULE_FORMAT: SimpleDateFormat = SimpleDateFormat("EEE d MMM 'at' HH:mm", Locale.getDefault())
    }
}

/* Reference List
Android Developers, 2026. Add pickers to your app. [online] Available at: <https://developer.android.com/develop/ui/views/components/pickers> [Accessed 2 October 2026].
Android Developers, 2026. Get a result from an activity. [online] Available at: <https://developer.android.com/training/basics/intents/result> [Accessed 2 October 2026].
Android Developers, 2026. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 2 October 2026].
Firebase, 2026. Delete files with Cloud Storage on Android. [online] Available at: <https://firebase.google.com/docs/storage/android/delete-files> [Accessed 2 October 2026].
Firebase, n.d. Upload files with Cloud Storage on Android. [online] Available at: <https://firebase.google.com/docs/storage/android/upload-files> [Accessed 2 October 2026].
Material Components for Android, n.d. Cards. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md> [Accessed 2 October 2026].
Material Components for Android, n.d. Progress indicators. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/ProgressIndicator.md> [Accessed 2 October 2026].
Oracle, 2026. Calendar (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Calendar.html> [Accessed 2 October 2026].
*/

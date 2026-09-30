package com.example.insy7315_wil_.ui.screens.journal

import android.os.Bundle
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.text.style.QuoteSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.InputFilter
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.JournalEntry
import com.example.insy7315_wil_.data.`Data classes`.JournalMarkup
import com.example.insy7315_wil_.data.`Data classes`.ValidationResult
import com.example.insy7315_wil_.data.`Data classes`.WellnessValidation
import com.example.insy7315_wil_.databinding.FragmentJournalEditorBinding
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent
import com.example.insy7315_wil_.ui.widget.SgulaModal
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

/**
 * Private journal editor: shows the daily prompt, confirms before saving, stores the entry in
 * Firestore under the signed-in member's uid and reports the +10 succulent points (IIE, 2026).
 */
class JournalEditorFragment : Fragment(R.layout.fragment_journal_editor) {
    private var _binding: FragmentJournalEditorBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private lateinit var prompts: JournalPrompts
    private var promptIndex = 0
    private var saving = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View binding replaces findViewById for the journal widgets (Android Developers, n.d.).
        _binding = FragmentJournalEditorBinding.bind(view)
        if (redirectGuestFromMemberContent()) return

        prompts = JournalPrompts(resources.getStringArray(R.array.sgula_journal_prompts).toList())
        val todayIndex = prompts.indexFor(LocalDate.now())
        // Keep the chosen prompt across rotation (Android Developers, n.d.).
        promptIndex = savedInstanceState?.getInt(KEY_PROMPT_INDEX, todayIndex) ?: todayIndex
        showPrompt()

        // Cap the entry at the same 10,000 characters the security rules allow (Android Developers, n.d.).
        binding.journalEntry.editText.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        binding.journalEntry.doOnTextChanged { text -> onDraftChanged(text) }
        onDraftChanged(binding.journalEntry.text)

        binding.journalBoldButton.setOnClickListener { toggleStyle(Typeface.BOLD) }
        binding.journalItalicButton.setOnClickListener { toggleStyle(Typeface.ITALIC) }
        binding.journalListButton.setOnClickListener { toggleParagraphStyle(bullet = true) }
        binding.journalQuoteButton.setOnClickListener { toggleParagraphStyle(bullet = false) }

        binding.journalNewPromptButton.setOnClickListener {
            promptIndex = prompts.nextIndex(promptIndex)
            showPrompt()
        }
        binding.journalHistoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_journalEditorFragment_to_journalHistoryFragment)
        }
        binding.journalSaveButton.setOnClickListener { confirmSave() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PROMPT_INDEX, promptIndex)
    }

    private fun showPrompt() {
        val isToday = promptIndex == prompts.indexFor(LocalDate.now())
        binding.journalPromptTitle.text = if (isToday) "Today's prompt" else "Another prompt"
        binding.journalPromptText.text = prompts[promptIndex]
    }

    private fun onDraftChanged(text: CharSequence) {
        val plainText = text.toString()
        binding.journalCharCount.text = "${plainText.length} / $MAX_LENGTH"
        binding.journalPreviewCard.isVisible = plainText.isNotBlank()
        binding.journalPreviewText.text = text
        if (plainText.isNotBlank()) {
            binding.journalEntry.error = null
            binding.journalSavedCard.isVisible = false
        }
    }

    /** Validates the draft, then asks the member to confirm before anything is written. */
    private fun confirmSave() {
        if (saving) return
        val plainContent = binding.journalEntry.text.trim()
        val validation = WellnessValidation.journal(plainContent, prompts[promptIndex])
        if (validation is ValidationResult.Invalid) {
            binding.journalEntry.error = if (plainContent.isBlank()) "Write a reflection before saving." else validation.message
            return
        }
        // Saving needs a Firebase user; the uid is what the security rules check (Firebase, n.d.).
        if (FirebaseAuth.getInstance().currentUser == null) {
            showStatus(JournalErrors.SIGNED_OUT, isError = true)
            return
        }
        SgulaModal.show(
            context = requireContext(),
            title = "Save this entry?",
            body = "Your entry will be stored privately in your journal. Only you can read it. " +
                "Saving adds +${JournalEntry.POINTS_PER_ENTRY} points to your succulent.",
            confirmText = "Save entry",
            onConfirm = { save(JournalMarkup.toStored(binding.journalEntry.editText.text)) },
            dismissText = "Keep writing",
            icon = "?",
        )
    }

    private fun save(content: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showStatus(JournalErrors.SIGNED_OUT, isError = true)
            return
        }
        setSaving(true)
        val prompt = prompts[promptIndex]
        val task = try {
            repository.addJournalEntry(JournalEntry(userId = userId, content = content, prompt = prompt))
        } catch (error: IllegalStateException) {
            setSaving(false)
            showStatus(JournalErrors.message(error, "save your entry"), isError = true)
            return
        }
        // Firestore returns a Task, so success and failure are handled separately (Firebase, n.d.).
        task.addOnSuccessListener {
            if (_binding == null) return@addOnSuccessListener
            setSaving(false)
            binding.journalEntry.text = ""
            binding.journalSavedCard.isVisible = true
            showStatus(null)
            SgulaModal.show(
                context = requireContext(),
                title = "Entry saved",
                body = "Your reflection is safely stored and your succulent earned " +
                    "+${JournalEntry.POINTS_PER_ENTRY} points.",
                confirmText = "View history",
                onConfirm = {
                    if (_binding != null) {
                        findNavController().navigate(R.id.action_journalEditorFragment_to_journalHistoryFragment)
                    }
                },
                dismissText = "Done",
                icon = "✓",
            )
        }.addOnFailureListener { error ->
            if (_binding == null) return@addOnFailureListener
            setSaving(false)
            // The draft stays in the editor so nothing the member wrote is lost.
            showStatus(JournalErrors.message(error, "save your entry"), isError = true)
        }
    }

    private fun toggleStyle(style: Int) {
        val editor = binding.journalEntry.editText
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        if (start == end) {
            showStatus("Select some text first to apply formatting.", isError = true)
            return
        }
        val editable = editor.text as? SpannableStringBuilder ?: SpannableStringBuilder(editor.text)
        val existing = editable.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == style && editable.getSpanStart(it) <= start && editable.getSpanEnd(it) >= end }
        if (existing.isEmpty()) {
            editable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            existing.forEach(editable::removeSpan)
        }
        editor.text = editable
        editor.setSelection(start.coerceAtMost(editable.length), end.coerceAtMost(editable.length))
        showStatus(null)
    }

    private fun toggleParagraphStyle(bullet: Boolean) {
        val editor = binding.journalEntry.editText
        val editable = editor.text as? SpannableStringBuilder ?: SpannableStringBuilder(editor.text)
        val cursor = editor.selectionStart.coerceAtLeast(0)
        val lineStart = editable.toString().lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)) + 1
        val nextBreak = editable.indexOf("\n", cursor)
        val lineEnd = if (nextBreak == -1) editable.length else nextBreak
        val existing = if (bullet) {
            editable.getSpans(lineStart, lineEnd, BulletSpan::class.java).toList()
        } else {
            editable.getSpans(lineStart, lineEnd, QuoteSpan::class.java).toList()
        }
        if (existing.isEmpty()) {
            if (bullet) editable.setSpan(BulletSpan(24), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            else editable.setSpan(QuoteSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            existing.forEach(editable::removeSpan)
        }
        editor.text = editable
        editor.setSelection(cursor.coerceAtMost(editable.length))
        showStatus(null)
    }

    private fun setSaving(value: Boolean) {
        saving = value
        binding.journalSaveButton.isEnabled = !value
        binding.journalSaveButton.text = if (value) "Saving..." else "Save entry"
        binding.journalNewPromptButton.isEnabled = !value
        binding.journalEntry.editText.isEnabled = !value
        binding.journalSaveProgress.isVisible = value
        if (value) showStatus("Saving your entry...")
    }

    private fun showStatus(message: String?, isError: Boolean = false) {
        binding.journalStatusText.isVisible = !message.isNullOrBlank()
        binding.journalStatusText.text = message
        binding.journalStatusText.setTextColor(
            ContextCompat.getColor(requireContext(), if (isError) R.color.sgula_danger else R.color.sgula_ink_700),
        )
    }

    // Clear the binding so late Firestore callbacks never touch a destroyed view (Android Developers, n.d.).
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val MAX_LENGTH = 10_000
        const val KEY_PROMPT_INDEX = "journal_prompt_index"
    }
}

/* Reference List
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Android Developers, n.d.. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 21 September 2026].
Android Developers, n.d.. Save UI states. [online] Available at: <https://developer.android.com/topic/libraries/architecture/saving-states> [Accessed 21 September 2026].
Android Developers, n.d.. InputFilter.LengthFilter. [online] Available at: <https://developer.android.com/reference/android/text/InputFilter.LengthFilter> [Accessed 21 September 2026].
Firebase, n.d.. Manage Users in Firebase (Android). [online] Available at: <https://firebase.google.com/docs/auth/android/manage-users> [Accessed 21 September 2026].
Firebase, n.d.. Add data to Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/manage-data/add-data> [Accessed 21 September 2026].
Android Developers, n.d.. Fragment lifecycle. [online] Available at: <https://developer.android.com/guide/fragments/lifecycle> [Accessed 21 September 2026].
Material Components for Android, n.d.. Progress indicators. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/ProgressIndicator.md> [Accessed 21 September 2026].
*/

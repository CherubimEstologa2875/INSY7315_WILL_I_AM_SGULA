package com.example.insy7315_wil_.ui.screens.journal

import android.os.Bundle
import android.content.res.ColorStateList
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.Editable
import android.text.TextWatcher
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
 * Where a member writes their journal entry. It offers the prompt of the day, checks before
 * saving anything, stores the entry against that member's own account and tells them the
 * succulent picked up its points.
 */
class JournalEditorFragment : Fragment(R.layout.fragment_journal_editor) {
    private var _binding: FragmentJournalEditorBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private lateinit var prompts: JournalPrompts
    private var promptIndex = 0
    private var saving = false
    private var boldForNextText = false
    private var italicForNextText = false
    private var bulletForNextParagraph = false
    private var quoteForNextParagraph = false
    private var selectedBold = false
    private var selectedItalic = false
    private var selectedBullet = false
    private var selectedQuote = false
    private var editStart = 0
    private var editBeforeLength = 0
    private var editBeforeTextLength = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View binding hands us the widgets without findViewById (Android Developers, 2026).
        _binding = FragmentJournalEditorBinding.bind(view)
        if (redirectGuestFromMemberContent()) return

        prompts = JournalPrompts(resources.getStringArray(R.array.sgula_journal_prompts).toList())
        val todayIndex = prompts.indexFor(LocalDate.now())
        // Hold on to the chosen prompt so a rotation doesn't swap it out mid-sentence (Android Developers, 2026).
        promptIndex = savedInstanceState?.getInt(KEY_PROMPT_INDEX, todayIndex) ?: todayIndex
        showPrompt()

        // Stop typing at the same 10,000 characters the security rules allow, so a long entry
        // fails here rather than after the member has finished writing (Android Developers, n.d.).
        binding.journalEntry.editText.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        binding.journalEntry.doOnTextChanged { text -> onDraftChanged(text) }
        binding.journalEntry.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
                editStart = start
                editBeforeLength = count
                editBeforeTextLength = text?.length ?: 0
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(editable: Editable?) {
                if (editable == null) return
                val insertedLength = editable.length - (editBeforeTextLength - editBeforeLength)
                if (insertedLength > 0) {
                    selectedBold = false
                    selectedItalic = false
                    selectedBullet = false
                    selectedQuote = false
                    applyTypingFormatting(editable, editStart, editStart + insertedLength)
                }
            }
        })
        onDraftChanged(binding.journalEntry.text)
        updateFormattingButtons()

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

    private fun onDraftChanged(text: String) {
        val plainText = text
        binding.journalCharCount.text = "${plainText.length} / $MAX_LENGTH"
        binding.journalPreviewCard.isVisible = plainText.isNotBlank()
        binding.journalPreviewText.text = plainText
        if (plainText.isNotBlank()) {
            binding.journalEntry.error = null
            binding.journalSavedCard.isVisible = false
        }
    }

    /** Checks the draft over, then asks before anything is written. */
    private fun confirmSave() {
        if (saving) return
        val plainContent = binding.journalEntry.text.trim()
        val validation = WellnessValidation.journal(plainContent, prompts[promptIndex])
        if (validation is ValidationResult.Invalid) {
            binding.journalEntry.error = if (plainContent.isBlank()) "Write a reflection before saving." else validation.message
            return
        }
        // Saving needs a signed-in user: their uid is what the rules check against (Firebase, n.d.).
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
        // Firestore answers with a Task, so success and failure arrive separately (Firebase, n.d.).
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
            // Leave the draft in the editor: nothing anyone wrote should disappear on a failed save.
            showStatus(JournalErrors.message(error, "save your entry"), isError = true)
        }
    }

    private fun toggleStyle(style: Int) {
        val editor = binding.journalEntry.editText
        val start = editor.selectionStart.coerceAtLeast(0)
        val end = editor.selectionEnd.coerceAtLeast(0)
        if (start == end) {
            if (style == Typeface.BOLD) {
                boldForNextText = !boldForNextText
                selectedBold = false
            }
            if (style == Typeface.ITALIC) {
                italicForNextText = !italicForNextText
                selectedItalic = false
            }
            editor.requestFocus()
            updateFormattingButtons()
            showStatus(null)
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
        if (style == Typeface.BOLD) {
            boldForNextText = false
            selectedBold = existing.isEmpty()
        }
        if (style == Typeface.ITALIC) {
            italicForNextText = false
            selectedItalic = existing.isEmpty()
        }
        editor.text = editable
        editor.setSelection(start.coerceAtMost(editable.length), end.coerceAtMost(editable.length))
        updateFormattingButtons()
        showStatus(null)
    }

    private fun toggleParagraphStyle(bullet: Boolean) {
        val editor = binding.journalEntry.editText
        val editable = editor.text as? SpannableStringBuilder ?: SpannableStringBuilder(editor.text)
        val cursor = editor.selectionStart.coerceAtLeast(0)
        if (editor.selectionStart == editor.selectionEnd) {
            val lineStart = editable.toString().lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)) + 1
            val nextBreak = editable.indexOf("\n", cursor)
            val lineEnd = if (nextBreak == -1) editable.length else nextBreak
            val queryEnd = maxOf(lineStart + 1, lineEnd).coerceAtMost(editable.length)
            val bulletSpans = editable.getSpans(lineStart, queryEnd, BulletSpan::class.java).toList()
            val quoteSpans = editable.getSpans(lineStart, queryEnd, QuoteSpan::class.java).toList()

            if (bullet) {
                selectedBullet = false
                selectedQuote = false
                if (bulletSpans.isNotEmpty() || bulletForNextParagraph) {
                    bulletSpans.forEach(editable::removeSpan)
                    bulletForNextParagraph = false
                } else {
                    quoteSpans.forEach(editable::removeSpan)
                    bulletForNextParagraph = true
                }
                quoteForNextParagraph = false
            } else {
                selectedQuote = false
                selectedBullet = false
                if (quoteSpans.isNotEmpty() || quoteForNextParagraph) {
                    quoteSpans.forEach(editable::removeSpan)
                    quoteForNextParagraph = false
                } else {
                    bulletSpans.forEach(editable::removeSpan)
                    quoteForNextParagraph = true
                }
                bulletForNextParagraph = false
            }
            editor.requestFocus()
            updateFormattingButtons()
            showStatus(null)
            return
        }
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
        if (bullet) {
            bulletForNextParagraph = false
            selectedBullet = existing.isEmpty()
            quoteForNextParagraph = false
            selectedQuote = false
        } else {
            quoteForNextParagraph = false
            selectedQuote = existing.isEmpty()
            bulletForNextParagraph = false
            selectedBullet = false
        }
        editor.text = editable
        editor.setSelection(cursor.coerceAtMost(editable.length))
        updateFormattingButtons()
        showStatus(null)
    }

    /** Applies the selected toolbar options to newly typed text, even when no text was selected. */
    private fun applyTypingFormatting(editable: Editable, start: Int, end: Int) {
        val safeStart = start.coerceIn(0, editable.length)
        val safeEnd = end.coerceIn(safeStart, editable.length)
        if (safeStart == safeEnd) return

        if (boldForNextText) {
            editable.setSpan(StyleSpan(Typeface.BOLD), safeStart, safeEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        if (italicForNextText) {
            editable.setSpan(StyleSpan(Typeface.ITALIC), safeStart, safeEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        if (bulletForNextParagraph || quoteForNextParagraph) {
            val lineStart = editable.toString().lastIndexOf('\n', (safeStart - 1).coerceAtLeast(0)) + 1
            val lineEnd = editable.indexOf("\n", safeStart).let { if (it == -1) editable.length else it }
            val queryEnd = maxOf(lineStart + 1, lineEnd).coerceAtMost(editable.length)
            if (bulletForNextParagraph) {
                val existing = editable.getSpans(lineStart, queryEnd, BulletSpan::class.java).toList()
                existing.drop(1).forEach(editable::removeSpan)
                val span = existing.firstOrNull() ?: BulletSpan(24)
                editable.setSpan(span, lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            } else {
                val existing = editable.getSpans(lineStart, queryEnd, QuoteSpan::class.java).toList()
                existing.drop(1).forEach(editable::removeSpan)
                val span = existing.firstOrNull() ?: QuoteSpan()
                editable.setSpan(span, lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
            }
        }
    }

    private fun updateFormattingButtons() {
        binding.journalBoldButton.isActivated = boldForNextText || selectedBold
        binding.journalItalicButton.isActivated = italicForNextText || selectedItalic
        binding.journalListButton.isActivated = bulletForNextParagraph || selectedBullet
        binding.journalQuoteButton.isActivated = quoteForNextParagraph || selectedQuote

        val selectedBackground = ContextCompat.getColor(requireContext(), R.color.sgula_primary_soft)
        val normalBackground = ContextCompat.getColor(requireContext(), R.color.sgula_surface)
        val selectedText = ContextCompat.getColor(requireContext(), R.color.sgula_plum_800)
        val normalText = ContextCompat.getColor(requireContext(), R.color.sgula_ink_700)
        val background = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()),
            intArrayOf(selectedBackground, normalBackground),
        )
        val text = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()),
            intArrayOf(selectedText, normalText),
        )
        listOf(
            binding.journalBoldButton,
            binding.journalItalicButton,
            binding.journalListButton,
            binding.journalQuoteButton,
        ).forEach {
            it.backgroundTintList = background
            it.setTextColor(text)
        }
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

    // Clear the binding so a late Firestore callback can't reach a view that is already gone
    // (Android Developers, 2026).
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
Android Developers, 2026. Fragment lifecycle. [online] Available at: <https://developer.android.com/guide/fragments/lifecycle> [Accessed 21 September 2026].
Android Developers, 2026. Saving state with fragments. [online] Available at: <https://developer.android.com/guide/fragments/saving-state> [Accessed 21 September 2026].
Android Developers, 2026. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 21 September 2026].
Android Developers, n.d. InputFilter.LengthFilter. [online] Available at: <https://developer.android.com/reference/android/text/InputFilter.LengthFilter> [Accessed 21 September 2026].
Firebase, n.d. Add data to Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/manage-data/add-data> [Accessed 21 September 2026].
Firebase, n.d. Manage Users in Firebase. [online] Available at: <https://firebase.google.com/docs/auth/android/manage-users> [Accessed 21 September 2026].
*/

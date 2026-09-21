package com.example.insy7315_wil_.ui.screens.journal

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.JournalEntry
import com.example.insy7315_wil_.databinding.FragmentJournalHistoryBinding
import com.example.insy7315_wil_.databinding.ItemJournalEntryBinding
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The member's own journal history. Entries are read live from Firestore with a query limited
 * to the signed-in uid; the security rules reject any other reader, including admins (IIE, 2026).
 */
class JournalHistoryFragment : Fragment(R.layout.fragment_journal_history) {
    private var _binding: FragmentJournalHistoryBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var registration: ListenerRegistration? = null
    private var entries: List<JournalEntry> = emptyList()
    private var month: YearMonth = YearMonth.now()
    private var openEntryId: String? = null
    private val zone: ZoneId get() = ZoneId.systemDefault()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentJournalHistoryBinding.bind(view)
        if (redirectGuestFromMemberContent()) return

        savedInstanceState?.getString(KEY_MONTH)?.let { month = YearMonth.parse(it) }
        openEntryId = savedInstanceState?.getString(KEY_OPEN_ENTRY)

        binding.journalMonthPrevious.setOnClickListener { month = month.minusMonths(1); render() }
        binding.journalMonthNext.setOnClickListener { month = month.plusMonths(1); render() }
        binding.journalReadClose.setOnClickListener { openEntryId = null; renderReadMode() }
        binding.journalBackToEditor.setOnClickListener { goToEditor() }
        render()
    }

    // Listen only while the screen is visible and detach in onStop (Android Developers, n.d.; Firebase, n.d.).
    override fun onStart() {
        super.onStart()
        if (_binding != null) startListening()
    }

    override fun onStop() {
        registration?.remove()
        registration = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MONTH, month.toString())
        openEntryId?.let { outState.putString(KEY_OPEN_ENTRY, it) }
    }

    private fun startListening() {
        registration?.remove()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showState("Sign in to see your journal", JournalErrors.SIGNED_OUT, action = null)
            return
        }
        showLoading(true)
        registration = try {
            repository.listenToJournalEntries(
                userId = userId,
                onEntries = { loaded ->
                    if (_binding == null) return@listenToJournalEntries
                    entries = loaded
                    showLoading(false)
                    render()
                },
                onError = { error ->
                    if (_binding == null) return@listenToJournalEntries
                    showLoading(false)
                    showState(
                        "We couldn't load your journal",
                        JournalErrors.message(error, "load your entries"),
                        action = "Try again" to { startListening() },
                    )
                },
            )
        } catch (error: IllegalStateException) {
            showLoading(false)
            showState("Sign in to see your journal", JournalErrors.message(error, "load your entries"), action = null)
            null
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.journalHistoryProgress.isVisible = loading
        if (loading) binding.journalStateCard.isVisible = false
    }

    private fun showState(title: String, body: String, action: Pair<String, () -> Unit>?) {
        binding.journalHistoryProgress.isVisible = false
        binding.journalStateCard.isVisible = true
        binding.journalStateTitle.text = title
        binding.journalStateBody.text = body
        binding.journalStateAction.isVisible = action != null
        action?.let { (label, onClick) ->
            binding.journalStateAction.text = label
            binding.journalStateAction.setOnClickListener { onClick() }
        }
    }

    /** Redraws the month snapshot, calendar, entry list and read card from [entries]. */
    private fun render() {
        val summary = JournalInsights.summary(entries, month, zone)
        binding.journalMonthLabel.text =
            "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
        binding.journalMonthNext.isEnabled = month < YearMonth.now()
        binding.journalStatEntries.text = summary.entries.toString()
        binding.journalStatWords.text = summary.words.toString()
        binding.journalStatStreak.text = summary.longestStreak.toString()
        renderCalendar()
        renderList()
        renderReadMode()
    }

    // Builds a Monday-first month grid from YearMonth (Oracle, n.d.).
    private fun renderCalendar() {
        val container = binding.journalCalendar
        container.removeAllViews()
        val context = requireContext()
        val daysWithEntries = JournalInsights.inMonth(entries, month, zone)
            .mapNotNull { JournalInsights.dateOf(it, zone) }
            .toSet()

        val header = weekRow()
        DayOfWeek.entries.forEach { day ->
            header.addView(TextView(context).apply {
                setTextAppearance(R.style.Widget_Sgula_Text_TableHeader)
                gravity = Gravity.CENTER
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }, cellParams(LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        container.addView(header)

        val cellHeight = resources.getDimensionPixelSize(R.dimen.sgula_min_touch_target)
        val leadingBlanks = month.atDay(1).dayOfWeek.value - 1 // Monday first
        val totalCells = leadingBlanks + month.lengthOfMonth()
        var row: LinearLayout? = null
        for (cell in 0 until ((totalCells + 6) / 7) * 7) {
            if (cell % 7 == 0) row = weekRow().also(container::addView)
            val dayNumber = cell - leadingBlanks + 1
            val view = TextView(context)
            if (dayNumber in 1..month.lengthOfMonth()) {
                val date = month.atDay(dayNumber)
                val hasEntry = date in daysWithEntries
                view.apply {
                    text = dayNumber.toString()
                    gravity = Gravity.CENTER
                    setTextAppearance(R.style.Widget_Sgula_Text_BodySmall)
                    setBackgroundResource(R.drawable.sgula_mood_item_bg)
                    isSelected = hasEntry
                    contentDescription = if (hasEntry) "$date, has a journal entry" else "$date, no entry"
                    if (hasEntry) setOnClickListener { openLatestEntryOn(date) }
                }
            } else {
                view.visibility = View.INVISIBLE
            }
            row?.addView(view, cellParams(cellHeight))
        }
    }

    private fun weekRow() = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }

    private fun cellParams(height: Int) = LinearLayout.LayoutParams(0, height, 1f).apply {
        val margin = resources.getDimensionPixelSize(R.dimen.sgula_space_1)
        setMargins(margin, margin, margin, margin)
    }

    private fun renderList() {
        val list = binding.journalEntriesList
        list.removeAllViews()
        val monthEntries = JournalInsights.inMonth(entries, month, zone)

        if (registration == null && entries.isEmpty()) return // loading or signed-out state is showing
        if (!binding.journalHistoryProgress.isVisible) {
            when {
                entries.isEmpty() -> showState(
                    "No entries yet",
                    "Your first reflection will appear here once you save it. Each entry adds +${JournalEntry.POINTS_PER_ENTRY} points to your succulent.",
                    action = "Write an entry" to { goToEditor() },
                )
                monthEntries.isEmpty() -> showState(
                    "Nothing written this month",
                    "Use the arrows above to look at another month.",
                    action = null,
                )
                else -> binding.journalStateCard.isVisible = false
            }
        }

        val today = LocalDate.now(zone)
        monthEntries.forEach { entry ->
            val item = ItemJournalEntryBinding.inflate(layoutInflater, list, false)
            val date = JournalInsights.dateOf(entry, zone)
            item.journalItemDate.text = when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                null -> "Just now"
                else -> date.format(DAY_FORMAT)
            }
            item.journalItemBadge.isVisible = entry.entryId == openEntryId
            item.journalItemPreview.text = entry.content
            item.journalItemMeta.text = listOfNotNull(
                timeOf(entry),
                "${entry.wordCount} ${if (entry.wordCount == 1) "word" else "words"}",
            ).joinToString(" · ")
            item.root.setOnClickListener { openEntry(entry.entryId) }
            list.addView(item.root)
        }
    }

    /** Returns to the editor already on the back stack instead of stacking a second copy (Android Developers, n.d.). */
    private fun goToEditor() {
        val navController = findNavController()
        if (!navController.popBackStack(R.id.journalEditorFragment, false)) {
            navController.navigate(R.id.action_journalHistoryFragment_to_journalEditorFragment)
        }
    }

    private fun openLatestEntryOn(date: LocalDate) {
        entries.firstOrNull { JournalInsights.dateOf(it, zone) == date }?.let { openEntry(it.entryId) }
    }

    private fun openEntry(entryId: String) {
        openEntryId = entryId
        renderList()
        renderReadMode()
        binding.journalHistoryScroll.post {
            _binding?.let { it.journalHistoryScroll.smoothScrollTo(0, it.journalReadCard.top) }
        }
    }

    /** Read mode: the full, unedited entry the member selected. */
    private fun renderReadMode() {
        val entry = entries.firstOrNull { it.entryId == openEntryId }
        binding.journalReadCard.isVisible = entry != null
        if (entry == null) return
        val date = JournalInsights.dateOf(entry, zone)
        binding.journalReadDate.text = listOfNotNull(date?.format(FULL_DAY_FORMAT), timeOf(entry)).joinToString(" · ")
        binding.journalReadPrompt.isVisible = entry.prompt.isNotBlank()
        binding.journalReadPrompt.text = "Prompt: ${entry.prompt}"
        binding.journalReadContent.text = entry.content
    }

    private fun timeOf(entry: JournalEntry): String? = entry.createdAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).format(TIME_FORMAT)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // Date and time patterns for the list and read mode (Oracle, n.d.).
    private companion object {
        const val KEY_MONTH = "journal_history_month"
        const val KEY_OPEN_ENTRY = "journal_history_open_entry"
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
        val FULL_DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

/* Reference List
IIE, 2026. INSY7315 Work Integrated Learning Module Manual 2026. The Independent Institute of Education (Pty) Ltd.
Android Developers, n.d.. View binding. [online] Available at: <https://developer.android.com/topic/libraries/view-binding> [Accessed 21 September 2026].
Android Developers, n.d.. Fragment lifecycle. [online] Available at: <https://developer.android.com/guide/fragments/lifecycle> [Accessed 21 September 2026].
Firebase, n.d.. Get realtime updates with Cloud Firestore. [online] Available at: <https://firebase.google.com/docs/firestore/query-data/listen> [Accessed 21 September 2026].
Firebase, n.d.. Manage Users in Firebase (Android). [online] Available at: <https://firebase.google.com/docs/auth/android/manage-users> [Accessed 21 September 2026].
Android Developers, n.d.. Navigate to a destination. [online] Available at: <https://developer.android.com/guide/navigation/use-graph/navigate> [Accessed 21 September 2026].
Android Developers, n.d.. Save UI states. [online] Available at: <https://developer.android.com/topic/libraries/architecture/saving-states> [Accessed 21 September 2026].
Oracle, n.d.. YearMonth (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/YearMonth.html> [Accessed 21 September 2026].
Oracle, n.d.. DateTimeFormatter (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html> [Accessed 21 September 2026].
Material Components for Android, n.d.. Cards. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/Card.md> [Accessed 21 September 2026].
Material Components for Android, n.d.. Progress indicators. [online] Available at: <https://github.com/material-components/material-components-android/blob/master/docs/components/ProgressIndicator.md> [Accessed 21 September 2026].
*/

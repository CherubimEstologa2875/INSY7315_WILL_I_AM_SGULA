package com.example.insy7315_wil_.ui.screens.mood

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.MoodEntry
import com.example.insy7315_wil_.databinding.FragmentMoodHistoryBinding
import com.example.insy7315_wil_.databinding.ItemMoodEntryBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class MoodHistoryFragment : Fragment(R.layout.fragment_mood_history) {
    private var _binding: FragmentMoodHistoryBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var registration: ListenerRegistration? = null
    private var entries: List<MoodEntry> = emptyList()
    private var month: YearMonth = YearMonth.now()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMoodHistoryBinding.bind(view)
        if (redirectGuestFromMemberContent()) return
        savedInstanceState?.getString(KEY_MONTH)?.let { month = YearMonth.parse(it) }
        binding.moodMonthPrevious.setOnClickListener { month = month.minusMonths(1); render() }
        binding.moodMonthNext.setOnClickListener { month = month.plusMonths(1); render() }
        binding.moodBackToLog.setOnClickListener { goToLog() }
    }

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
    }

    private fun startListening() {
        registration?.remove()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showState("Sign in to see your moods", "Please sign in again to see your mood history.")
            return
        }
        binding.moodHistoryProgress.isVisible = true
        binding.moodStateCard.isVisible = false
        registration = try {
            repository.listenToMoodEntries(
                userId = userId,
                onEntries = { loaded ->
                    if (_binding == null) return@listenToMoodEntries
                    binding.moodHistoryProgress.isVisible = false
                    entries = loaded
                    render()
                },
                onError = { error ->
                    if (_binding == null) return@listenToMoodEntries
                    showState(
                        "We couldn't load your moods",
                        JournalErrors.message(error, "load your mood history"),
                        action = "Try again" to { startListening() },
                    )
                },
            )
        } catch (error: IllegalStateException) {
            showState("Sign in to see your moods", JournalErrors.message(error, "load your mood history"))
            null
        }
    }

    private fun render() {
        binding.moodEntriesList.removeAllViews()
        binding.moodMonthBar.isVisible = entries.isNotEmpty()
        if (entries.isEmpty()) {
            binding.moodSummaryCard.isVisible = false
            showState(
                "No check-ins yet",
                "Your first check-in will show up here once you save it.",
                action = "Log a mood" to { goToLog() },
            )
            return
        }

        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        binding.moodMonthLabel.text = "$monthName ${month.year}"
        binding.moodMonthNext.isEnabled = month < YearMonth.now()

        val monthEntries = entries.filter { monthOf(it) == month }
        binding.moodSummaryCard.isVisible = monthEntries.isNotEmpty()
        if (monthEntries.isEmpty()) {
            showState("Nothing logged in $monthName", "Use the arrows to look at another month.")
            return
        }
        binding.moodStateCard.isVisible = false
        val labels = resources.getStringArray(R.array.sgula_mood_labels)
        val tones = resources.obtainTypedArray(R.array.sgula_mood_tones)
        val today = LocalDate.now()

        val average = monthEntries.map { it.moodLevel }.average()
        val averageText = String.format(Locale.US, "%.1f", average)
        val averageIndex = (average.roundToInt() - 1).coerceIn(labels.indices)
        binding.moodAverageRing.progress = (average / 5).toFloat()
        binding.moodAverageRing.progressColor = tones.getColor(averageIndex, 0)
        binding.moodAverageRing.setValueLabel(averageText)
        binding.moodAverageRing.setCaption("out of 5")
        binding.moodAverageLabel.text = labels[averageIndex]
        binding.moodAverageDot.backgroundTintList = ColorStateList.valueOf(tones.getColor(averageIndex, 0))
        binding.moodCheckIns.text = if (monthEntries.size == 1) "1 check-in" else "${monthEntries.size} check-ins"

        monthEntries.forEach { entry ->
            val item = ItemMoodEntryBinding.inflate(layoutInflater, binding.moodEntriesList, false)
            val index = (entry.moodLevel - 1).coerceIn(labels.indices)
            item.moodItemLabel.text = labels[index]
            item.moodItemDot.backgroundTintList = ColorStateList.valueOf(tones.getColor(index, 0))
            item.moodItemDate.text = entry.createdAtMillis?.let { millis ->
                val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                val day = when (time.toLocalDate()) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    else -> time.format(DAY_FORMAT)
                }
                "$day · ${time.format(TIME_FORMAT)}"
            } ?: "Just now"
            item.moodItemNote.isVisible = entry.note.isNotBlank()
            item.moodItemNote.text = entry.note
            binding.moodEntriesList.addView(item.root)
        }
        tones.recycle()
    }

    // Entries still waiting on the server timestamp have no time yet, so they count as today
    private fun dateOf(entry: MoodEntry): LocalDate =
        entry.createdAtMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            ?: LocalDate.now()

    private fun monthOf(entry: MoodEntry): YearMonth = YearMonth.from(dateOf(entry))

    private fun showState(title: String, body: String, action: Pair<String, () -> Unit>? = null) {
        binding.moodHistoryProgress.isVisible = false
        binding.moodStateCard.isVisible = true
        binding.moodStateTitle.text = title
        binding.moodStateBody.text = body
        binding.moodStateAction.isVisible = action != null
        action?.let { (label, onClick) ->
            binding.moodStateAction.text = label
            binding.moodStateAction.setOnClickListener { onClick() }
        }
    }

    // Goes back to the log already on the back stack instead of opening a second copy
    private fun goToLog() {
        val navController = findNavController()
        if (!navController.popBackStack(R.id.moodLogFragment, false)) {
            navController.navigate(R.id.action_moodHistoryFragment_to_moodLogFragment)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val KEY_MONTH = "mood_history_month"
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
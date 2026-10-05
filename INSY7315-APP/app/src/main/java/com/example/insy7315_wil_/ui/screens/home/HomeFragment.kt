package com.example.insy7315_wil_.ui.screens.home

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.SucculentState
import com.example.insy7315_wil_.ui.widget.AffirmationCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val repository = FirebaseWellnessRepository()
    private var succulentListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        view.findViewById<View>(R.id.home_plant_card)?.setOnClickListener {
            navController.navigate(R.id.action_homeFragment_to_succulentFragment)
        }
        view.findViewById<View>(R.id.home_tile_mood)?.setOnClickListener {
            navController.navigate(R.id.action_homeFragment_to_moodLogFragment)
        }
        view.findViewById<View>(R.id.home_tile_journal)?.setOnClickListener {
            navController.navigate(R.id.action_homeFragment_to_journalEditorFragment)
        }
        view.findViewById<View>(R.id.home_tile_audio)?.setOnClickListener {
            navController.navigate(R.id.action_homeFragment_to_audioLibraryFragment)
        }
        view.findViewById<View>(R.id.home_tile_quiz)?.setOnClickListener {
            navController.navigate(R.id.action_homeFragment_to_quizStartFragment)
        }
        view.findViewById<View>(R.id.home_play_broadcast)?.setOnClickListener {
            navController.navigate(R.id.action_homeFragment_to_broadcastFragment)
        }

        // Keeps the line from the layout if today's affirmation hasn't been written yet
        repository.loadDailyAffirmation().addOnSuccessListener { document ->
            val text = document.getString("text")
            if (!text.isNullOrBlank()) {
                view.findViewById<AffirmationCardView>(R.id.home_affirmation)?.setAffirmation("\"$text\"")
            }
        }

        val greeting = greetingForNow()
        view.findViewById<TextView>(R.id.home_greeting)?.text = greeting

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Keeps the greeting without a name if the profile hasn't loaded yet
        repository.loadUserProfile(userId).addOnSuccessListener { document ->
            val profile = repository.toUserProfile(document)
            if (profile.displayName.isNotBlank()) {
                view.findViewById<TextView>(R.id.home_greeting)?.text = "$greeting, ${profile.displayName}"
            }
        }

        succulentListener = repository.observeSucculentState(userId, { state -> renderSucculent(view, state) })
    }

    // Uses the phone's clock
    private fun greetingForNow(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    private fun renderSucculent(view: View, state: SucculentState) {
        val stage = state.stage.lowercase()
        val inactiveForSevenDays = state.lastActivityAtMillis?.let {
            System.currentTimeMillis() - it >= 7L * 24 * 60 * 60 * 1000
        } ?: false
        val isWilted = state.wilted || inactiveForSevenDays
        val (nextStage, start, end) = when (stage) {
            "seed" -> Triple("Sprout", 0, 100)
            "sprout" -> Triple("Growing", 100, 300)
            "growing" -> Triple("Blooming", 300, 600)
            else -> Triple("maximum growth", 600, 600)
        }
        val percent = if (end == start) 100 else (((state.totalPoints - start).coerceIn(0, end - start) * 100) / (end - start))

        view.findViewById<ImageView>(R.id.home_plant_icon)?.setImageResource(
            if (isWilted) R.drawable.ic_plant_wilted else when (stage) {
                "sprout" -> R.drawable.ic_plant_sprout
                "growing" -> R.drawable.ic_plant_growing
                "blooming" -> R.drawable.ic_plant_blooming
                else -> R.drawable.ic_plant_seed
            }
        )
        view.findViewById<TextView>(R.id.home_plant_stage_name)?.text =
            if (isWilted) "Wilted" else stage.replaceFirstChar { it.uppercase() }
        view.findViewById<TextView>(R.id.home_plant_points_caption)?.text = when {
            isWilted -> "${state.totalPoints} pts, new activity will revive it"
            end == start -> "${state.totalPoints} pts, fully grown"
            else -> "${state.totalPoints} pts, ${end - state.totalPoints} more to reach $nextStage"
        }
        view.findViewById<ProgressBar>(R.id.home_plant_progress)?.progress = percent
    }

    override fun onDestroyView() {
        succulentListener?.remove()
        succulentListener = null
        super.onDestroyView()
    }
}

package com.example.insy7315_wil_.ui.screens.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.ui.widget.AffirmationCardView

class HomeFragment : Fragment(R.layout.fragment_home) {

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
        FirebaseWellnessRepository().loadDailyAffirmation().addOnSuccessListener { document ->
            val text = document.getString("text")
            if (!text.isNullOrBlank()) {
                view.findViewById<AffirmationCardView>(R.id.home_affirmation)?.setAffirmation("\"$text\"")
            }
        }
    }
}
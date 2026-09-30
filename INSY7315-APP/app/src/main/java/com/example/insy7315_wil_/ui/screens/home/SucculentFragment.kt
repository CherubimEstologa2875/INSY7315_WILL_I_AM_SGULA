package com.example.insy7315_wil_.ui.screens.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.databinding.FragmentSucculentBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.SucculentState
import com.google.firebase.auth.FirebaseAuth
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent

class SucculentFragment : Fragment(R.layout.fragment_succulent) {
    private var _binding: FragmentSucculentBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()
    private var stateListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSucculentBinding.bind(view)
        if (redirectGuestFromMemberContent()) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        stateListener = repository.observeSucculentState(userId, ::renderState)
    }

    private fun renderState(state: SucculentState) {
        val stage = state.stage.lowercase()
        val inactiveForSevenDays = state.lastActivityAtMillis?.let {
            System.currentTimeMillis() - it >= 7L * 24 * 60 * 60 * 1000
        } ?: false
        val isWilted = state.wilted || inactiveForSevenDays
        val stageIndex = when (stage) { "sprout" -> 1; "growing" -> 2; "blooming" -> 3; else -> 0 }
        val (nextStage, start, end) = when (stage) {
            "seed" -> Triple("Sprout", 0, 100)
            "sprout" -> Triple("Growing", 100, 300)
            "growing" -> Triple("Blooming", 300, 600)
            else -> Triple("maximum growth", 600, 600)
        }
        val percent = if (end == start) 100 else (((state.totalPoints - start).coerceIn(0, end - start) * 100) / (end - start))
        val displayStage = if (isWilted) "Wilted" else stage.replaceFirstChar { it.uppercase() }
        binding.succulentHero!!.setImageResource(if (isWilted) R.drawable.ic_plant_wilted else when (stage) {
            "sprout" -> R.drawable.ic_plant_sprout
            "growing" -> R.drawable.ic_plant_growing
            "blooming" -> R.drawable.ic_plant_blooming
            else -> R.drawable.ic_plant_seed
        })
        binding.succulentStageName!!.text = displayStage
        binding.succulentPointsSummary!!.text = if (isWilted) {
            "${state.totalPoints} pts · New activity will revive your succulent"
        } else if (end == start) {
            "${state.totalPoints} pts · Fully grown"
        } else {
            "${state.totalPoints} pts, ${end - state.totalPoints} more to reach $nextStage"
        }
        binding.succulentStatusBadge!!.text = if (isWilted) "Needs attention" else "Watered with your activity"
        binding.succulentStatusBadge!!.setTextColor(requireContext().getColor(if (isWilted) R.color.sgula_danger else R.color.sgula_plant_600))
        binding.succulentGrowthBar!!.progress = percent
        binding.succulentStageTracker!!.stageIndex = stageIndex
        binding.succulentStageTracker!!.wilted = isWilted
        binding.succulentSubtitle!!.text = if (isWilted) "Your succulent wilted after 7 days without activity." else "Every wellbeing action helps it grow."
    }

    override fun onDestroyView() {
        stateListener?.remove()
        stateListener = null
        _binding = null
        super.onDestroyView()
    }
}

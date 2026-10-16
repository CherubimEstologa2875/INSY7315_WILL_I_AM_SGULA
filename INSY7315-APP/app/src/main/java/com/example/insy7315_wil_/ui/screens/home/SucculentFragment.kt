package com.example.insy7315_wil_.ui.screens.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.databinding.FragmentSucculentBinding
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.SucculentStages
import com.example.insy7315_wil_.data.`Data classes`.SucculentState
import com.google.firebase.auth.FirebaseAuth
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.redirectGuestFromMemberContent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

        repository.loadUserProfile(userId).addOnSuccessListener { document ->
            if (_binding == null) return@addOnSuccessListener
            val createdAtMillis = repository.toUserProfile(document).createdAtMillis ?: return@addOnSuccessListener
            val since = Instant.ofEpochMilli(createdAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            binding.succulentSince!!.text = "Growing with you since ${SINCE_FORMAT.format(since)}."
        }
    }

    private fun renderState(state: SucculentState) {
        val stage = state.stage.lowercase()
        val isWilted = SucculentStages.isWilted(state.wilted, state.lastActivityAtMillis)
        val (stageIndex, nextStage, start, end) = SucculentStages.progressFor(stage)
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

    private companion object {
        val SINCE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
    }
}

package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent

class AdminEngagementFragment :
    Fragment(R.layout.fragment_admin_engagement) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val repository = FirebaseWellnessRepository()

        repository.checkAdmin(
            onResult = { isAdmin: Boolean ->
                if (!isAdmin) {
                    redirectNonAdminFromAdminContent()
                } else {
                    loadEngagement(view)
                }
            },
            onError = {
                redirectNonAdminFromAdminContent()
            }
        )
    }

    private fun loadEngagement(view: View) {

        val repository = FirebaseWellnessRepository()

        val container =
            view.findViewById<LinearLayout>(
                R.id.admin_engagement_container
            )

        repository.loadAllUsers(
            onSuccess = { users: List<UserProfile> ->

                container.removeAllViews()

                users.forEach { user: UserProfile ->

                    loadUserEngagement(
                        user = user,
                        container = container,
                        repository = repository
                    )
                }
            },
            onError = {

                Toast.makeText(
                    requireContext(),
                    "Unable to load engagement",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun loadUserEngagement(
        user: UserProfile,
        container: LinearLayout,
        repository: FirebaseWellnessRepository
    ) {

        repository.loadAudioSessions(
            userId = user.userId,

            onSuccess = { sessionCount ->

                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16, 20, 16, 20)
                }

                val name = TextView(requireContext()).apply {
                    text = user.displayName.ifBlank {
                        user.email
                    }
                }

                val points = TextView(requireContext()).apply {
                    text = "—"
                }

                val sessions = TextView(requireContext()).apply {
                    text = sessionCount.toString()
                }

                row.addView(
                    name,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.9f
                    )
                )

                row.addView(
                    points,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.1f
                    )
                )

                row.addView(
                    sessions,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.1f
                    )
                )

                container.addView(row)
            },

            onError = {

                Toast.makeText(
                    requireContext(),
                    "Unable to load sessions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}
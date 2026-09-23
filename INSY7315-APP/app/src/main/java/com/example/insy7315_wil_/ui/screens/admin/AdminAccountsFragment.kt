package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.google.android.material.button.MaterialButton
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
class AdminAccountsFragment : Fragment(R.layout.fragment_admin_accounts) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = FirebaseWellnessRepository()

        repository.checkAdmin(
            onResult = { isAdmin ->
                if (!isAdmin) {
                    redirectNonAdminFromAdminContent()
                } else {
                    loadUsers(view)
                }
            },
            onError = {
                redirectNonAdminFromAdminContent()
            }
        )
    }

    private fun loadUsers(view: View) {

        val repository = FirebaseWellnessRepository()

        val container =
            view.findViewById<LinearLayout>(
                R.id.admin_accounts_container
            )

        repository.loadAllUsers(
            onSuccess = { users ->

                container.removeAllViews()

                users.forEach { user ->

                    addUserRow(
                        container,
                        user,
                        repository
                    )
                }
            },
            onError = {
                Toast.makeText(
                    requireContext(),
                    "Unable to load users",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun addUserRow(
        container: LinearLayout,
        user: UserProfile,
        repository: FirebaseWellnessRepository
    ) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 20, 16, 20)
        }

        val name = TextView(requireContext()).apply {
            text = user.displayName.ifBlank { user.email }
        }

        val status = TextView(requireContext()).apply {
            text = if (user.active) "Active" else "Deactivated"
        }

        val button = MaterialButton(requireContext()).apply {
            text = if (user.active) "Deactivate" else "Activate"
        }

        row.addView(
            name,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            )
        )

        row.addView(
            status,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.2f
            )
        )

        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.4f
            )
        )

        button.setOnClickListener {

            repository.setUserActive(
                user.userId,
                !user.active,
                onSuccess = {
                    loadUsers(requireView())
                },
                onError = {
                    Toast.makeText(
                        requireContext(),
                        "Unable to update account",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        container.addView(row)
    }
}

package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.data.`Data classes`.FirebaseWellnessRepository
import com.example.insy7315_wil_.data.`Data classes`.UserProfile
import com.example.insy7315_wil_.databinding.FragmentAdminAccountsBinding
import com.example.insy7315_wil_.databinding.ItemAdminAccountBinding
import com.example.insy7315_wil_.ui.screens.journal.JournalErrors
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent
import com.example.insy7315_wil_.ui.widget.SgulaModal

// Deactivating keeps the account and its data, it only stops the client logging in
class AdminAccountsFragment : Fragment(R.layout.fragment_admin_accounts) {
    private var _binding: FragmentAdminAccountsBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseWellnessRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAdminAccountsBinding.bind(view)

        repository.checkAdmin(
            onResult = { isAdmin ->
                if (_binding == null) return@checkAdmin
                if (isAdmin) loadUsers() else redirectNonAdminFromAdminContent()
            },
            onError = { if (_binding != null) redirectNonAdminFromAdminContent() },
        )
    }

    private fun loadUsers() {
        binding.adminAccountsLoading.isVisible = true
        binding.adminAccountsEmpty.isVisible = false

        repository.loadAllUsers(
            onSuccess = { users ->
                if (_binding == null) return@loadAllUsers
                binding.adminAccountsLoading.isVisible = false
                binding.adminAccountsContainer.removeAllViews()
                if (users.isEmpty()) {
                    showEmpty("No accounts yet.")
                    return@loadAllUsers
                }
                users.sortedBy { nameOf(it).lowercase() }.forEach(::addRow)
            },
            onError = { error ->
                if (_binding == null) return@loadAllUsers
                binding.adminAccountsLoading.isVisible = false
                showEmpty(JournalErrors.message(error, "load the accounts"))
            },
        )
    }

    private fun addRow(user: UserProfile) {
        val row = ItemAdminAccountBinding.inflate(layoutInflater, binding.adminAccountsContainer, false)
        row.adminAccountDivider.isVisible = binding.adminAccountsContainer.childCount > 0
        row.adminAccountName.text = nameOf(user)
        row.adminAccountActive.isVisible = user.active
        row.adminAccountDeactivated.isVisible = !user.active

        // An admin deactivating themselves would lock the practice out of this screen
        val isSelf = user.userId == repository.currentUserId()
        row.adminAccountToggle.text = if (isSelf) "You" else if (user.active) "Deactivate" else "Activate"
        row.adminAccountToggle.isEnabled = !isSelf
        row.adminAccountToggle.setOnClickListener { confirmToggle(user, row) }
        binding.adminAccountsContainer.addView(row.root)
    }

    private fun confirmToggle(user: UserProfile, row: ItemAdminAccountBinding) {
        if (!user.active) {
            setActive(user, active = true, row = row)
            return
        }
        SgulaModal.show(
            context = requireContext(),
            title = "Deactivate this account?",
            body = "${nameOf(user)} won't be able to log in until you activate the account again. Their data is kept.",
            confirmText = "Deactivate",
            onConfirm = { setActive(user, active = false, row = row) },
            dismissText = "Cancel",
            destructive = true,
            icon = "!",
        )
    }

    private fun setActive(user: UserProfile, active: Boolean, row: ItemAdminAccountBinding) {
        row.adminAccountToggle.isEnabled = false
        repository.setUserActive(
            user.userId,
            active,
            onSuccess = { if (_binding != null) loadUsers() },
            onError = { error ->
                if (_binding == null) return@setUserActive
                row.adminAccountToggle.isEnabled = true
                Toast.makeText(requireContext(), JournalErrors.message(error, "update this account"), Toast.LENGTH_LONG).show()
            },
        )
    }

    private fun showEmpty(message: String) {
        binding.adminAccountsEmpty.text = message
        binding.adminAccountsEmpty.isVisible = true
    }

    private fun nameOf(user: UserProfile) = user.displayName.ifBlank { user.email }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
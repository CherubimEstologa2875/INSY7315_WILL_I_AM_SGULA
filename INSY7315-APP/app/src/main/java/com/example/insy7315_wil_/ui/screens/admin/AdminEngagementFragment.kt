package com.example.insy7315_wil_.ui.screens.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.screens.redirectNonAdminFromAdminContent

class AdminEngagementFragment : Fragment(R.layout.fragment_admin_engagement) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        redirectNonAdminFromAdminContent()
    }
}

package com.example.insy7315_wil_

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.insy7315_wil_.ui.widget.NavItem
import com.example.insy7315_wil_.ui.widget.SgulaTabBarView
import com.example.insy7315_wil_.ui.widget.SgulaTopNavView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.sgula_screen)

        val navController = (supportFragmentManager
            .findFragmentById(R.id.sgula_nav_host) as NavHostFragment).navController

        val items = Tabs.map { NavItem(it.route, it.label) }

        // only one of these exists, the w1024dp layout swaps the tab bar for the top nav
        val tabBar = findViewById<SgulaTabBarView>(R.id.sgula_tab_bar)
        val topNav = findViewById<SgulaTopNavView>(R.id.sgula_top_nav)

        tabBar?.apply {
            setItems(items)
            onNavigate = { navController.switchTab(it) }
        }
        topNav?.apply {
            setBrand(getString(R.string.sgula_brand))
            setItems(items)
            onNavigate = { navController.switchTab(it) }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val route = TabForDestination[destination.id]
            tabBar?.isVisible = route != null
            topNav?.isVisible = route != null
            if (route != null) {
                tabBar?.selectedRoute = route
                topNav?.selectedRoute = route
            }
        }
    }

    private fun NavController.switchTab(route: String) {
        val destination = Tabs.firstOrNull { it.route == route }?.destination ?: return
        if (currentDestination?.id == destination) return
        navigate(
            destination,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(R.id.homeFragment) { saveState = true }
            },
        )
    }

    private class Tab(val route: String, val label: String, val destination: Int)

    private companion object {
        val Tabs = listOf(
            Tab("home", "Home", R.id.homeFragment),
            Tab("mood", "Mood", R.id.moodLogFragment),
            Tab("journal", "Journal", R.id.journalEditorFragment),
            Tab("audio", "Audio", R.id.audioLibraryFragment),
            Tab("profile", "Profile", R.id.settingsFragment),
        )

        // a detail screen keeps its parent tab lit, anything missing here hides the bar entirely
        val TabForDestination = mapOf(
            R.id.homeFragment to "home",
            R.id.succulentFragment to "home",
            R.id.quizStartFragment to "home",
            R.id.quizQuestionFragment to "home",
            R.id.quizResultFragment to "home",
            R.id.moodLogFragment to "mood",
            R.id.moodHistoryFragment to "mood",
            R.id.journalEditorFragment to "journal",
            R.id.journalHistoryFragment to "journal",
            R.id.audioLibraryFragment to "audio",
            R.id.playerFragment to "audio",
            R.id.broadcastFragment to "audio",
            R.id.broadcastHistoryFragment to "audio",
            R.id.settingsFragment to "profile",
            R.id.adminUploadFragment to "profile",
            R.id.adminAffirmationsFragment to "profile",
            R.id.adminAccountsFragment to "profile",
            R.id.adminEngagementFragment to "profile",
            R.id.galleryFragment to "profile",
        )
    }
}

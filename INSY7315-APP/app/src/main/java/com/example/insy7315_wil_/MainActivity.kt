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

        val items = Tabs.map { NavItem(it.route, it.label, it.icon) }

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
        val tab = Tabs.firstOrNull { it.route == route } ?: return
        if (currentDestination?.id == tab.destination) return

        // Home is the root of the signed-in tab flow. Popping to it directly is important when
        // the current stack is Home -> Broadcast -> History (or Home -> Broadcast -> Player):
        // the Home tab must return to Home immediately instead of adding another destination.
        if (route == "home" && popBackStack(R.id.homeFragment, false)) return

        navigate(
            tab.action,
            null,
            navOptions {
                launchSingleTop = true
                restoreState = true
                popUpTo(R.id.homeFragment) { saveState = true }
            },
        )
    }

    private class Tab(
        val route: String,
        val label: String,
        val destination: Int,
        val action: Int,
        val icon: Int,
    )

    private companion object {
        // Icons are Material Symbols Rounded (Google, n.d.)
        val Tabs = listOf(
            Tab("home", "Home", R.id.homeFragment, R.id.action_global_homeFragment, R.drawable.ic_nav_home),
            Tab("mood", "Mood", R.id.moodLogFragment, R.id.action_global_moodLogFragment, R.drawable.ic_nav_mood),
            Tab("journal", "Journal", R.id.journalEditorFragment, R.id.action_global_journalEditorFragment, R.drawable.ic_nav_journal),
            Tab("audio", "Audio", R.id.audioLibraryFragment, R.id.action_global_audioLibraryFragment, R.drawable.ic_nav_audio),
            Tab("profile", "Profile", R.id.settingsFragment, R.id.action_global_settingsFragment, R.drawable.ic_nav_profile),
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
            R.id.adminBroadcastsFragment to "profile",
            R.id.adminAccountsFragment to "profile",
            R.id.adminEngagementFragment to "profile",
            R.id.galleryFragment to "profile",
        )
    }
}

/* Reference List
Google. n.d. Material Symbols. [Online]. Available at: https://fonts.google.com/icons [Accessed 30 October 2026].
*/

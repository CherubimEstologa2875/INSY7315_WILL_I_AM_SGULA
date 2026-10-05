package com.example.insy7315_wil_.ui.screens.audio

import com.example.insy7315_wil_.data.`Data classes`.AudioContent

/** Works out which library tab a track belongs on. Plain Kotlin, so the tests can cover it. */
object AudioCategoryGrouping {
    // Category is typed by hand on the upload form, and the wording has drifted: it says
    // "Meditation" now and older records say "Guided meditation". Matching on a few keywords
    // keeps both on the same tab. The order here follows R.array.sgula_audio_tabs.
    val categoryKeywords = listOf(
        listOf("white"),
        listOf("nature"),
        listOf("meditat", "guid"),
    )

    /** True when this track belongs on the tab at [tabIndex]. */
    fun matchesTab(category: String, tabIndex: Int): Boolean =
        categoryKeywords.getOrNull(tabIndex)
            ?.any { keyword -> category.contains(keyword, ignoreCase = true) } == true

    // one pass over tracks instead of scanning it again for every tab
    fun groupByTab(tracks: List<AudioContent>): List<List<AudioContent>> {
        val grouped = List(categoryKeywords.size) { mutableListOf<AudioContent>() }
        tracks.forEach { track ->
            val tabIndex = categoryKeywords.indexOfFirst { keywords ->
                keywords.any { keyword -> track.category.contains(keyword, ignoreCase = true) }
            }
            if (tabIndex >= 0) grouped[tabIndex].add(track)
        }
        return grouped
    }
}

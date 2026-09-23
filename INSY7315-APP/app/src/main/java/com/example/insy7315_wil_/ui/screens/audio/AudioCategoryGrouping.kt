package com.example.insy7315_wil_.ui.screens.audio

import com.example.insy7315_wil_.data.`Data classes`.AudioContent

/** Pure grouping of loaded audio content into library tabs, kept separate so it can be unit tested. */
object AudioCategoryGrouping {
    // category is a free text field in Firestore and matches loosely against the tab names
    // rather than an exact string to allow minor wording differences in seed data to allow it to still group correctly
    val categoryKeywords = listOf("white", "nature", "guid")

    // one pass over tracks instead of scanning it again for every tab
    fun groupByTab(tracks: List<AudioContent>): List<List<AudioContent>> {
        val grouped = List(categoryKeywords.size) { mutableListOf<AudioContent>() }
        tracks.forEach { track ->
            val tabIndex = categoryKeywords.indexOfFirst { keyword ->
                track.category.contains(keyword, ignoreCase = true)
            }
            if (tabIndex >= 0) grouped[tabIndex].add(track)
        }
        return grouped
    }
}

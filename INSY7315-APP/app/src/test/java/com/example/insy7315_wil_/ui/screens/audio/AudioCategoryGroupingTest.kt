package com.example.insy7315_wil_.ui.screens.audio

import com.example.insy7315_wil_.data.`Data classes`.AudioContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCategoryGroupingTest {

    private fun track(category: String, title: String = category) =
        AudioContent(audioId = title, title = title, category = category)

    @Test
    fun groupsEachTrackIntoTheMatchingTab() {
        val tracks = listOf(track("White Noise"), track("Nature"), track("Guided Meditation"))
        val grouped = AudioCategoryGrouping.groupByTab(tracks)

        assertEquals(1, grouped[0].size)
        assertEquals(1, grouped[1].size)
        assertEquals(1, grouped[2].size)
    }

    @Test
    fun matchingIsCaseInsensitiveAndLoose() {
        val grouped = AudioCategoryGrouping.groupByTab(listOf(track("guided sleep session")))
        assertEquals(1, grouped[2].size)
    }

    @Test
    fun tracksWithNoMatchingCategoryAreDropped() {
        val grouped = AudioCategoryGrouping.groupByTab(listOf(track("unrelated category")))
        assertTrue(grouped.all { it.isEmpty() })
    }

    @Test
    fun emptyInputReturnsOneEmptyListPerTab() {
        val grouped = AudioCategoryGrouping.groupByTab(emptyList())
        assertEquals(AudioCategoryGrouping.categoryKeywords.size, grouped.size)
        assertTrue(grouped.all { it.isEmpty() })
    }
}

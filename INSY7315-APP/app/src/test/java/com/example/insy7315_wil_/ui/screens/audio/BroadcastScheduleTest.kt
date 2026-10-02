package com.example.insy7315_wil_.ui.screens.audio

import com.example.insy7315_wil_.data.`Data classes`.Broadcast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for the broadcast schedule: what counts as live, what is still coming (JUnit, n.d.).
class BroadcastScheduleTest {
    // A fixed "now" in epoch milliseconds, the unit java.util.Date works in (Oracle, 2026)
    private val now = 1_000_000L

    private fun broadcast(id: String, at: Long?) =
        Broadcast(broadcastId = id, title = id, message = "", publishedAtMillis = at)

    @Test
    fun aBroadcastIsPublishedOnlyOnceItsTimeHasPassed() {
        assertTrue(BroadcastSchedule.isPublished(broadcast("past", now - 1), now))
        assertTrue(BroadcastSchedule.isPublished(broadcast("exact", now), now))
        assertFalse(BroadcastSchedule.isPublished(broadcast("future", now + 1), now))
    }

    @Test
    fun publishedAreNewestFirstAndUpcomingAreSoonestFirst() {
        val all = listOf(
            broadcast("old", now - 2000),
            broadcast("recent", now - 10),
            broadcast("later", now + 5000),
            broadcast("soon", now + 100),
        )
        assertEquals(listOf("recent", "old"), BroadcastSchedule.published(all, now).map { it.broadcastId })
        assertEquals(listOf("soon", "later"), BroadcastSchedule.upcoming(all, now).map { it.broadcastId })
        assertEquals("recent", BroadcastSchedule.latestPublished(all, now)?.broadcastId)
        assertEquals("soon", BroadcastSchedule.nextUpcoming(all, now)?.broadcastId)
    }

    @Test
    fun aScheduledBroadcastIsLabelledAsComing() {
        assertTrue(BroadcastSchedule.dateLabel(broadcast("future", now + 90_000_000), now).startsWith("Coming "))
        assertFalse(BroadcastSchedule.dateLabel(broadcast("past", now - 90_000_000), now).startsWith("Coming "))
        assertEquals("", BroadcastSchedule.dateLabel(broadcast("undated", null), now))
    }

    @Test
    fun anUndatedBroadcastStillCountsAsPublished() {
        val undated = listOf(broadcast("undated", null))
        assertEquals(1, BroadcastSchedule.published(undated, now).size)
        assertNull(BroadcastSchedule.nextUpcoming(undated, now))
    }
}

/* Reference List
JUnit, n.d. Getting started. [online] Available at: <https://github.com/junit-team/junit4/wiki/Getting-started> [Accessed 2 October 2026].
Oracle, 2026. SimpleDateFormat (Java SE 17 & JDK 17). [online] Available at: <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html> [Accessed 2 October 2026].
*/

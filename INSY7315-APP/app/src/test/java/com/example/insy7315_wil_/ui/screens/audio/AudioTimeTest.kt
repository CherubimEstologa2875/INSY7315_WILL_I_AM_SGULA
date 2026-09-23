package com.example.insy7315_wil_.ui.screens.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioTimeTest {

    @Test
    fun formatTrackDurationAlwaysShowsTwoDigitMinutes() {
        assertEquals("00:00", AudioTime.formatTrackDuration(0))
        assertEquals("01:05", AudioTime.formatTrackDuration(65))
        assertEquals("12:30", AudioTime.formatTrackDuration(750))
    }

    @Test
    fun formatPlaybackTimeDoesNotPadMinutes() {
        assertEquals("0:00", AudioTime.formatPlaybackTime(0))
        assertEquals("1:05", AudioTime.formatPlaybackTime(65))
        assertEquals("12:30", AudioTime.formatPlaybackTime(750))
    }

    @Test
    fun parseDurationReadsMinutesAndSeconds() {
        assertEquals(750, AudioTime.parseDuration("12:30"))
        assertEquals(0, AudioTime.parseDuration("00:00"))
    }

    @Test
    fun parseDurationRejectsAnythingNotShapedLikeMmSs() {
        assertNull(AudioTime.parseDuration("bad"))
        assertNull(AudioTime.parseDuration(""))
        assertNull(AudioTime.parseDuration("1:2:3"))
        assertNull(AudioTime.parseDuration("ab:cd"))
    }

    @Test
    fun sleepTimerMillisMatchesThePickerOptions() {
        assertEquals(15 * 60 * 1000L, AudioTime.sleepTimerMillis("15 minutes"))
        assertEquals(30 * 60 * 1000L, AudioTime.sleepTimerMillis("30 minutes"))
        assertEquals(45 * 60 * 1000L, AudioTime.sleepTimerMillis("45 minutes"))
        assertEquals(60 * 60 * 1000L, AudioTime.sleepTimerMillis("1 hour"))
    }

    @Test
    fun sleepTimerMillisIsNullForOffAndUnknownChoices() {
        assertNull(AudioTime.sleepTimerMillis("Off"))
        assertNull(AudioTime.sleepTimerMillis("not a real option"))
    }
}

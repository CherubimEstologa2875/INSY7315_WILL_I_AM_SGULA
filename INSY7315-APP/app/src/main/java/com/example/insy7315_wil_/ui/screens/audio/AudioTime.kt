package com.example.insy7315_wil_.ui.screens.audio

/** Pure duration formatting and parsing behind the audio screens, kept separate so they can be unit tested. */
object AudioTime {
    // track list style, always shows two digits for minutes
    fun formatTrackDuration(totalSeconds: Int) = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

    // player bar style, matches the existing convention of no leading zero on minutes
    fun formatPlaybackTime(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

    // parses a "mm:ss" string back into seconds, used when a screen only hands over a duration string
    fun parseDuration(value: String): Int? {
        val parts = value.split(":")
        if (parts.size != 2) return null
        val minutes = parts[0].toIntOrNull() ?: return null
        val seconds = parts[1].toIntOrNull() ?: return null
        return minutes * 60 + seconds
    }

    // returns null for "Off" or anything that is not one of the sleep timer picker's own options
    fun sleepTimerMillis(choice: String): Long? = when (choice) {
        "15 minutes" -> 15 * 60 * 1000L
        "30 minutes" -> 30 * 60 * 1000L
        "45 minutes" -> 45 * 60 * 1000L
        "1 hour" -> 60 * 60 * 1000L
        else -> null
    }
}

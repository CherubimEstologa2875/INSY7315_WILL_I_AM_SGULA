package com.example.insy7315_wil_.data.`Data classes`

/**
 * The points the app tells members they will earn. The Cloud Functions are what actually award
 * them (pointsByReason in functions/src/index.ts), so these numbers only decide what
 * the screens say. They have to stay equal to the server's or a screen promises points the
 * member never gets, which is what SucculentPointsTest checks.
 */
object SucculentPoints {
    const val QUIZ = 15
    const val AUDIO = 20
}

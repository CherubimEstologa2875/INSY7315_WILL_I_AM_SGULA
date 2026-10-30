package com.example.insy7315_wil_.ui.screens.audio

import com.example.insy7315_wil_.R
import com.example.insy7315_wil_.ui.widget.SgulaTrackArtView

// Icons are Material Symbols Rounded (Google, n.d.).
internal fun SgulaTrackArtView.showCategory(category: String) {
    when {
        AudioCategoryGrouping.matchesTab(category, 0) ->
            setArt(R.drawable.ic_art_white_noise, R.color.sgula_accent_100, R.color.sgula_accent_700)
        AudioCategoryGrouping.matchesTab(category, 1) ->
            setArt(R.drawable.ic_art_nature, R.color.sgula_plant_100, R.color.sgula_plant_700)
        else ->
            setArt(R.drawable.ic_art_meditation, R.color.sgula_plum_100, R.color.sgula_plum_600)
    }
}

/* Reference List
Google. n.d. Material Symbols. [Online]. Available at: https://fonts.google.com/icons [Accessed 30 October 2026].
*/
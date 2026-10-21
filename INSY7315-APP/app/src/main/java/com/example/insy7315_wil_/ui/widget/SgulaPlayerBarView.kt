package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.google.android.material.card.MaterialCardView
import com.example.insy7315_wil_.R
import kotlin.math.roundToInt

class SgulaPlayerBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val subtitleView: TextView
    private val elapsedView: TextView
    private val totalView: TextView
    private val playView: ImageView
    private val loopView: ImageView
    private val favouriteView: ImageView
    private val seekView: SeekBar

    var onPlayPause: ((Boolean) -> Unit)? = null
    var onToggleLoop: ((Boolean) -> Unit)? = null
    var onToggleFavourite: ((Boolean) -> Unit)? = null

    var isPlaying: Boolean = false
        set(value) {
            field = value
            playView.isSelected = value
            playView.contentDescription = context.getString(
                if (value) R.string.sgula_pause else R.string.sgula_play
            )
        }

    var isLooping: Boolean = false
        set(value) {
            field = value
            loopView.isSelected = value
        }

    var isFavourite: Boolean = false
        set(value) {
            field = value
            favouriteView.isSelected = value
            favouriteView.contentDescription = context.getString(
                if (value) R.string.sgula_favourite_remove else R.string.sgula_favourite_add
            )
        }

    var playbackProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            seekView.progress = (field * 100).roundToInt()
        }

    init {
        radius = resources.getDimension(R.dimen.sgula_radius_lg)
        cardElevation = resources.getDimension(R.dimen.sgula_elevation_md)
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.sgula_surface))
        outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.sgula_shadow)
        outlineSpotShadowColor = ContextCompat.getColor(context, R.color.sgula_shadow)
        strokeWidth = 0

        inflate(context, R.layout.view_player_bar, this)
        titleView = findViewById(R.id.player_title)
        subtitleView = findViewById(R.id.player_subtitle)
        elapsedView = findViewById(R.id.player_elapsed)
        totalView = findViewById(R.id.player_total)
        playView = findViewById(R.id.player_play)
        loopView = findViewById(R.id.player_loop)
        favouriteView = findViewById(R.id.player_favourite)
        seekView = findViewById(R.id.player_seek)

        // marquee sits still unless the view is selected, nothing to do with the card's own state
        titleView.isSelected = true

        playView.setOnClickListener {
            // Let the player confirm the state change. This keeps the button responsive while
            // still preventing a misleading "pause" state during asynchronous preparation.
            onPlayPause?.invoke(!isPlaying)
        }
        loopView.setOnClickListener {
            isLooping = !isLooping
            onToggleLoop?.invoke(isLooping)
        }
        favouriteView.setOnClickListener {
            isFavourite = !isFavourite
            onToggleFavourite?.invoke(isFavourite)
        }

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaPlayerBarView,
            defStyleAttr,
            0,
        ).use { a ->
            titleView.text = a.getString(R.styleable.SgulaPlayerBarView_sgulaTitle)
            subtitleView.text = a.getString(R.styleable.SgulaPlayerBarView_sgulaSubtitle)
            elapsedView.text = a.getString(R.styleable.SgulaPlayerBarView_sgulaElapsed)
            totalView.text = a.getString(R.styleable.SgulaPlayerBarView_sgulaTotal)
            playbackProgress = a.getFloat(R.styleable.SgulaPlayerBarView_sgulaProgress, 0f)
            isPlaying = a.getBoolean(R.styleable.SgulaPlayerBarView_sgulaPlaying, false)
            isLooping = a.getBoolean(R.styleable.SgulaPlayerBarView_sgulaLooping, false)
            isFavourite = a.getBoolean(R.styleable.SgulaPlayerBarView_sgulaFavourite, false)
        }
    }

    fun setTitle(text: CharSequence?) {
        titleView.text = text
    }

    fun setSubtitle(text: CharSequence?) {
        subtitleView.text = text
    }

    fun setElapsed(text: CharSequence?) {
        elapsedView.text = text
    }

    fun setTotal(text: CharSequence?) {
        totalView.text = text
    }
}

package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.use
import com.example.insy7315_wil_.R

class SgulaTrackItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val durationView: TextView
    private val favouriteView: ImageView

    val artView: SgulaTrackArtView

    var onToggleFavourite: ((Boolean) -> Unit)? = null

    var isFavourite: Boolean = false
        set(value) {
            field = value
            favouriteView.isSelected = value
            favouriteView.contentDescription = context.getString(
                if (value) R.string.sgula_favourite_remove else R.string.sgula_favourite_add
            )
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.sgula_track_item_bg)
        val h = resources.getDimensionPixelSize(R.dimen.sgula_space_2)
        val v = resources.getDimensionPixelSize(R.dimen.sgula_space_3)
        setPadding(h, v, h, v)
        isClickable = true
        isFocusable = true

        inflate(context, R.layout.view_track_item, this)
        titleView = findViewById(R.id.track_title)
        durationView = findViewById(R.id.track_duration)
        favouriteView = findViewById(R.id.track_favourite)
        artView = findViewById(R.id.track_art)

        favouriteView.setOnClickListener {
            isFavourite = !isFavourite
            onToggleFavourite?.invoke(isFavourite)
        }

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaTrackItemView,
            defStyleAttr,
            0,
        ).use { a ->
            titleView.text = a.getString(R.styleable.SgulaTrackItemView_sgulaTitle)
            durationView.text = a.getString(R.styleable.SgulaTrackItemView_sgulaDuration)
            isFavourite = a.getBoolean(R.styleable.SgulaTrackItemView_sgulaFavourite, false)
        }
    }

    fun setTitle(text: CharSequence?) {
        titleView.text = text
    }

    fun setDuration(text: CharSequence?) {
        durationView.text = text
    }
}

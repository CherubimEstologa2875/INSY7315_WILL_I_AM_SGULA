package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.example.insy7315_wil_.R

class SgulaTrackArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageView(context, attrs, defStyleAttr) {

    private val cornerRadius = resources.getDimension(R.dimen.sgula_radius_sm)

    private var artSize = resources.getDimensionPixelSize(R.dimen.sgula_track_art)

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaTrackArtView,
            defStyleAttr,
            0,
        ).use { a ->
            artSize = a.getDimensionPixelSize(R.styleable.SgulaTrackArtView_sgulaArtSize, artSize)
        }

        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        clipToOutline = true

        val inset = artSize / 4
        setPadding(inset, inset, inset, inset)
        scaleType = ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO

        setArt(R.drawable.ic_art_meditation, R.color.sgula_plum_100, R.color.sgula_plum_600)
    }

    fun setArt(@DrawableRes icon: Int, @ColorRes background: Int, @ColorRes iconColor: Int) {
        setBackgroundColor(ContextCompat.getColor(context, background))
        setImageResource(icon)
        imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, iconColor))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(artSize, artSize)
    }
}
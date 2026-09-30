package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.example.insy7315_wil_.R

public class SgulaPasswordStrengthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    public enum class Strength(val filledBars: Int) {
        WEAK(1),
        MEDIUM(2),
        STRONG(3),
    }

    private val bars: List<View>
    private val labelView: TextView

    var strength: Strength = Strength.WEAK
        set(value) {
            field = value
            applyStrength()
        }

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        inflate(context, R.layout.view_password_strength, this)
        bars = listOf(
            findViewById(R.id.strength_bar_1),
            findViewById(R.id.strength_bar_2),
            findViewById(R.id.strength_bar_3),
        )
        labelView = findViewById(R.id.strength_label)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaPasswordStrengthView,
            defStyleAttr,
            0,
        ).use { a ->
            strength = Strength.values().getOrElse(
                a.getInt(R.styleable.SgulaPasswordStrengthView_sgulaStrength, 0),
            ) { Strength.WEAK }
        }
    }

    private fun applyStrength() {
        val fillColor: Int
        val labelColor: Int
        val labelText: Int
        when (strength) {
            Strength.WEAK -> {
                fillColor = R.color.sgula_danger
                labelColor = R.color.sgula_danger_hover
                labelText = R.string.sgula_strength_weak
            }
            Strength.MEDIUM -> {
                fillColor = R.color.sgula_accent
                labelColor = R.color.sgula_accent_700
                labelText = R.string.sgula_strength_medium
            }
            Strength.STRONG -> {
                fillColor = R.color.sgula_plum_500
                labelColor = R.color.sgula_plum_700
                labelText = R.string.sgula_strength_strong
            }
        }

        val filled = ColorStateList.valueOf(ContextCompat.getColor(context, fillColor))
        val empty = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.sgula_warm_gray_200)
        )
        bars.forEachIndexed { index, bar ->
            bar.backgroundTintList = if (index < strength.filledBars) filled else empty
        }

        labelView.setText(labelText)
        labelView.setTextColor(ContextCompat.getColor(context, labelColor))
    }
}

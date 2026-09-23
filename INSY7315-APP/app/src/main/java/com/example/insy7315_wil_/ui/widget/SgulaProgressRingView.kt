package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.example.insy7315_wil_.R
import kotlin.math.roundToInt

class SgulaProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val strokeWidth = resources.getDimension(R.dimen.sgula_ring_stroke)
    private val arcBounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = this@SgulaProgressRingView.strokeWidth
        color = ContextCompat.getColor(context, R.color.sgula_warm_gray_200)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.strokeWidth = this@SgulaProgressRingView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.sgula_plum_500)
    }

    private val valueView: TextView
    private val captionView: TextView

    private var diameter = resources.getDimensionPixelSize(R.dimen.sgula_ring_diameter)
    private var valueLabelOverride: String? = null

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            applyValueLabel()
            invalidate()
        }

    var progressColor: Int
        get() = progressPaint.color
        set(value) {
            progressPaint.color = value
            invalidate()
        }

    init {
        setWillNotDraw(false)
        inflate(context, R.layout.view_progress_ring, this)
        valueView = findViewById(R.id.ring_value)
        captionView = findViewById(R.id.ring_caption)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaProgressRingView,
            defStyleAttr,
            0,
        ).use { a ->
            diameter = a.getDimensionPixelSize(
                R.styleable.SgulaProgressRingView_sgulaRingDiameter,
                diameter,
            )
            captionView.text = a.getString(R.styleable.SgulaProgressRingView_sgulaCaption)
                ?: context.getString(R.string.sgula_ring_caption)
            valueLabelOverride = a.getString(R.styleable.SgulaProgressRingView_sgulaValueLabel)
            progress = a.getFloat(R.styleable.SgulaProgressRingView_sgulaProgress, 0f)
        }
    }

    fun setCaption(text: CharSequence?) {
        captionView.text = text
    }

    fun setValueLabel(text: String?) {
        valueLabelOverride = text
        applyValueLabel()
    }

    private fun applyValueLabel() {
        valueView.text = valueLabelOverride ?: "${(progress * 100).roundToInt()}%"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = MeasureSpec.makeMeasureSpec(diameter, MeasureSpec.EXACTLY)
        super.onMeasure(spec, spec)
        setMeasuredDimension(diameter, diameter)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = strokeWidth / 2f
        arcBounds.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(arcBounds, START_ANGLE, 360f, false, trackPaint)
        canvas.drawArc(arcBounds, START_ANGLE, 360f * progress, false, progressPaint)
    }

    private companion object {
        const val START_ANGLE = -90f
    }
}

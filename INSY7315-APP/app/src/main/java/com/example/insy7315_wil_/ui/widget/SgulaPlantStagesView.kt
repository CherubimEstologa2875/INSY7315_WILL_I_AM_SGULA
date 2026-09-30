package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.use
import com.example.insy7315_wil_.R

public class SgulaPlantStagesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val itemViews = mutableListOf<View>()
    private val iconSmallW = resources.getDimensionPixelSize(R.dimen.sgula_plant_stage_icon_sm_w)
    private val iconSmallH = resources.getDimensionPixelSize(R.dimen.sgula_plant_stage_icon_sm_h)
    private val iconLargeW = resources.getDimensionPixelSize(R.dimen.sgula_plant_stage_icon_lg_w)
    private val iconLargeH = resources.getDimensionPixelSize(R.dimen.sgula_plant_stage_icon_lg_h)

    private val stageIcons = intArrayOf(
        R.drawable.ic_plant_seed,
        R.drawable.ic_plant_sprout,
        R.drawable.ic_plant_growing,
        R.drawable.ic_plant_blooming,
        R.drawable.ic_plant_wilted,
    )

    private var wiltedIndex = -1

    public var stageIndex: Int = 0
        set(value) {
            field = value
            applyStages()
        }

    public var wilted: Boolean = false
        set(value) {
            field = value
            applyStages()
        }

    init {
        orientation = HORIZONTAL

        val labels = resources.getStringArray(R.array.sgula_plant_stages).toList()
        wiltedIndex = labels.lastIndex
        buildItems(labels)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaPlantStagesView,
            defStyleAttr,
            0,
        ).use { a ->
            stageIndex = a.getInt(R.styleable.SgulaPlantStagesView_sgulaStageIndex, 0)
            wilted = a.getBoolean(R.styleable.SgulaPlantStagesView_sgulaWilted, false)
        }
    }

    private fun buildItems(labels: List<String>) {
        val inflater = LayoutInflater.from(context)
        val gap = resources.getDimensionPixelSize(R.dimen.sgula_space_2)

        labels.forEachIndexed { index, label ->
            val item = inflater.inflate(R.layout.view_plant_stage, this, false)
            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            if (index > 0) params.marginStart = gap
            item.layoutParams = params

            item.findViewById<TextView>(R.id.plant_label).text = label
            item.findViewById<ImageView>(R.id.plant_icon)
                .setImageResource(stageIcons.getOrElse(index) { stageIcons[0] })

            addView(item)
            itemViews += item
        }
    }

    private fun applyStages() {
        if (itemViews.isEmpty()) return

        val current = if (wilted) wiltedIndex else stageIndex

        itemViews.forEachIndexed { index, item ->
            val isCurrent = index == current
            val isWiltedSlot = isCurrent && wilted

            item.isSelected = isCurrent
            item.isActivated = isWiltedSlot

            val icon = item.findViewById<ImageView>(R.id.plant_icon)
            val isLarge = isCurrent && !isWiltedSlot
            val width = if (isLarge) iconLargeW else iconSmallW
            val height = if (isLarge) iconLargeH else iconSmallH
            if (icon.layoutParams.width != width) {
                icon.layoutParams = icon.layoutParams.apply {
                    this.width = width
                    this.height = height
                }
            }
        }
    }
}

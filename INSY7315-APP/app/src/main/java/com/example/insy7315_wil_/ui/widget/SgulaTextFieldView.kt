package com.example.insy7315_wil_.ui.widget

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.example.insy7315_wil_.R

class SgulaTextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val labelRow: LinearLayout
    private val labelView: TextView
    private val optionalView: TextView
    private val inputView: EditText
    private val helperView: TextView

    private var helperText: CharSequence? = null

    val editText: EditText get() = inputView

    var text: String
        get() = inputView.text?.toString().orEmpty()
        set(value) = inputView.setText(value)

    var error: CharSequence? = null
        set(value) {
            field = value
            applyError()
        }

    init {
        orientation = VERTICAL
        inflate(context, R.layout.view_text_field, this)
        labelRow = findViewById(R.id.field_label_row)
        labelView = findViewById(R.id.field_label)
        optionalView = findViewById(R.id.field_optional)
        inputView = findViewById(R.id.field_input)
        helperView = findViewById(R.id.field_helper)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SgulaTextFieldView,
            defStyleAttr,
            0,
        ).use { a ->
            val label = a.getString(R.styleable.SgulaTextFieldView_sgulaLabel)
            labelView.text = label
            labelRow.isVisible = !label.isNullOrEmpty()

            inputView.hint = a.getString(R.styleable.SgulaTextFieldView_sgulaPlaceholder)
            helperText = a.getString(R.styleable.SgulaTextFieldView_sgulaHelperText)
            optionalView.isVisible =
                a.getBoolean(R.styleable.SgulaTextFieldView_sgulaOptional, false)

            val singleLine = a.getBoolean(R.styleable.SgulaTextFieldView_sgulaSingleLine, true)
            val password = a.getBoolean(R.styleable.SgulaTextFieldView_sgulaPassword, false)
            applyInputMode(singleLine, password)

            inputView.minimumHeight = a.getDimensionPixelSize(
                R.styleable.SgulaTextFieldView_sgulaFieldMinHeight,
                resources.getDimensionPixelSize(R.dimen.sgula_min_touch_target),
            )

            error = a.getString(R.styleable.SgulaTextFieldView_sgulaError)
        }
    }

    private fun applyInputMode(singleLine: Boolean, password: Boolean) {
        inputView.isSingleLine = singleLine
        inputView.inputType = when {
            password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            singleLine -> InputType.TYPE_CLASS_TEXT
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        if (password) inputView.setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        inputView.gravity =
            if (singleLine) Gravity.CENTER_VERTICAL else Gravity.TOP or Gravity.START
    }

    private fun applyError() {
        val message = error
        val hasError = !message.isNullOrEmpty()

        val left = inputView.paddingLeft
        val top = inputView.paddingTop
        val right = inputView.paddingRight
        val bottom = inputView.paddingBottom
        inputView.setBackgroundResource(
            if (hasError) R.drawable.sgula_field_background_error
            else R.drawable.sgula_field_background
        )
        inputView.setPadding(left, top, right, bottom)

        val shown = if (hasError) message else helperText
        helperView.setTextAppearance(
            if (hasError) R.style.TextAppearance_Sgula_FieldError
            else R.style.TextAppearance_Sgula_FieldHint
        )
        helperView.text = shown
        helperView.isVisible = !shown.isNullOrEmpty()
    }

    fun setLabel(value: CharSequence?) {
        labelView.text = value
        labelRow.isVisible = !value.isNullOrEmpty()
    }

    fun setPlaceholder(value: CharSequence?) {
        inputView.hint = value
    }

    fun setHelperText(value: CharSequence?) {
        helperText = value
        applyError()
    }

    fun doOnTextChanged(action: (String) -> Unit) {
        inputView.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) = action(s?.toString().orEmpty())
            }
        )
    }
}

package com.test

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText

class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    var onCaretMoved: ((line: Int, column: Int) -> Unit)? = null

    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(13f)
        typeface = Typeface.MONOSPACE
        color = 0xFF6E7681.toInt()
    }
    private val gutterBg = Paint().apply { color = 0xFF181818.toInt() }
    private val gutterDivider = Paint().apply { color = 0xFF2A2A2A.toInt() }

    private var gutterWidth = 0f
    private var lastDigits = -1

    private val highlightRunnable = Runnable {
        val editable = text ?: return@Runnable
        SyntaxHighlighter.apply(editable)
    }

    init {
        background = null
        typeface = Typeface.MONOSPACE
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(0xFFD4D4D4.toInt())
        setHintTextColor(0xFF5A5A5A.toInt())
        setHint("Start typing...")
        setLineSpacing(0f, 1.12f)
        gravity = Gravity.TOP or Gravity.START
        isSingleLine = false
        setHorizontallyScrolling(true)
        isHorizontalScrollBarEnabled = true
        isVerticalScrollBarEnabled = true
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN

        updateGutter()

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                updateGutter()
                removeCallbacks(highlightRunnable)
                postDelayed(highlightRunnable, 90L)
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentLayout = layout ?: return
        if (gutterWidth <= 0f) return

        val sx = scrollX.toFloat()
        val sy = scrollY.toFloat()
        val viewHeight = height.toFloat()

        canvas.drawRect(sx, sy, sx + gutterWidth, sy + viewHeight, gutterBg)
        canvas.drawRect(sx + gutterWidth - dp(0.6f), sy, sx + gutterWidth, sy + viewHeight, gutterDivider)

        val top = totalPaddingTop
        val firstLine = currentLayout.getLineForVertical((scrollY - top).toInt())
        val lastLine = currentLayout.getLineForVertical((scrollY + height - top).toInt())
        val caretLine = currentLayout.getLineForOffset(selectionStart.coerceIn(0, text?.length ?: 0))

        for (i in firstLine..lastLine) {
            if (i >= currentLayout.lineCount) break
            val baseline = currentLayout.getLineBaseline(i).toFloat() + top
            if (baseline < sy - gutterPaint.textSize || baseline > sy + viewHeight + gutterPaint.textSize) {
                continue
            }
            gutterPaint.color = if (i == caretLine) 0xFFC6C6C6.toInt() else 0xFF6E7681.toInt()
            val label = (i + 1).toString()
            val x = sx + gutterWidth - dp(10f) - gutterPaint.measureText(label)
            canvas.drawText(label, x, baseline, gutterPaint)
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        reportCaret()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(highlightRunnable)
        super.onDetachedFromWindow()
    }

    private fun reportCaret() {
        val editable = text ?: return
        val pos = selectionStart.coerceIn(0, editable.length)
        val currentLayout = layout
        if (currentLayout != null && pos <= currentLayout.text.length) {
            val line = currentLayout.getLineForOffset(pos)
            onCaretMoved?.invoke(line + 1, pos - currentLayout.getLineStart(line) + 1)
        } else {
            var line = 1
            var lineStart = 0
            for (i in 0 until pos) {
                if (editable[i] == '\n') {
                    line++
                    lineStart = i + 1
                }
            }
            onCaretMoved?.invoke(line, pos - lineStart + 1)
        }
    }

    private fun updateGutter() {
        val editable = text ?: return
        var lines = 1
        for (i in 0 until editable.length) {
            if (editable[i] == '\n') lines++
        }
        val digits = lines.toString().length.coerceAtLeast(2)
        if (digits == lastDigits) return
        lastDigits = digits
        gutterWidth = gutterPaint.measureText("8".repeat(digits)) + dp(16f)
        setPadding(
            (gutterWidth + dp(6f)).toInt(),
            paddingTop,
            paddingRight,
            paddingBottom
        )
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}

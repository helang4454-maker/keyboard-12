package com.glasstype.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class GlassTypeImeService : InputMethodService() {
    private var caps = false
    private lateinit var root: LinearLayout
    private lateinit var keyRows: LinearLayout

    override fun onCreate() {
        super.onCreate()
        applyLiveBlur()
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyLiveBlur()
    }

    private fun applyLiveBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                window?.window?.apply {
                    setBackgroundBlurRadius(34)
                    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    attributes = attributes.apply {
                        blurBehindRadius = 26
                        dimAmount = 0f
                    }
                }
            } catch (_: Exception) {
                // Some Android manufacturers disable cross-window blur.
            }
        }
    }

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(8), dp(7), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.argb(112, 242, 245, 252))
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(),
                    dp(0).toFloat(), dp(0).toFloat(), dp(0).toFloat(), dp(0).toFloat())
                setStroke(dp(1), Color.argb(110, 255, 255, 255))
            }
        }

        val toolbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(5), 0, dp(5), dp(5))
        }
        val logo = TextView(this).apply {
            text = "GlassType"
            textSize = 13f
            setTextColor(Color.rgb(55, 62, 76))
            typeface = Typeface.DEFAULT_BOLD
        }
        val spacer = View(this)
        val more = TextView(this).apply {
            text = "•••"
            textSize = 17f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }
        toolbar.addView(logo)
        toolbar.addView(spacer, LinearLayout.LayoutParams(0, dp(24), 1f))
        toolbar.addView(more)
        root.addView(toolbar)

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("😊", "—", "✨").forEach { label ->
            val t = TextView(this).apply {
                text = label
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(55, 62, 76))
            }
            suggestions.addView(t, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
        root.addView(suggestions)

        keyRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keyRows)
        buildKeys()
        return root
    }

    private fun buildKeys() {
        keyRows.removeAllViews()
        addLetterRow("qwertyuiop")
        addLetterRow("asdfghjkl")
        val third = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        third.addView(makeKey("⇧", 1.35f, true) { caps = !caps; buildKeys() })
        "zxcvbnm".forEach { c -> third.addView(makeKey(if (caps) c.uppercase() else c.toString(), 1f) { commit(if (caps) c.uppercase() else c.toString()) }) }
        third.addView(makeKey("⌫", 1.35f, true) { currentInputConnection?.deleteSurroundingText(1, 0) })
        keyRows.addView(third)
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottom.addView(makeKey("123", 1.25f, true) {},)
        bottom.addView(makeKey("☺", 1f, true) { commit("😊") })
        bottom.addView(makeKey("English", 4.2f, false) { commit(" ") })
        bottom.addView(makeKey("⌫", 1.2f, true) { currentInputConnection?.deleteSurroundingText(1, 0) })
        bottom.addView(makeKey("↵", 1.35f, false) { currentInputConnection?.commitText("\n", 1) },)
        keyRows.addView(bottom)
    }

    private fun addLetterRow(chars: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        chars.forEach { c ->
            val label = if (caps) c.uppercase() else c.toString()
            row.addView(makeKey(label, 1f) { commit(label) })
        }
        keyRows.addView(row)
    }

    private fun makeKey(label: String, weight: Float, utility: Boolean = false, action: () -> Unit): Button {
        val d = resources.displayMetrics.density
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = if (utility) 13f else 18f
            setTextColor(Color.rgb(38, 43, 53))
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                setColor(if (utility) Color.argb(205, 194, 202, 216) else Color.argb(220, 255, 255, 255))
                cornerRadius = 7f * d
            }
            minHeight = (42 * d).toInt()
            minimumHeight = (42 * d).toInt()
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(0, (45 * d).toInt(), weight).apply {
            val margin = (2 * d).toInt()
            setMargins(margin, (3 * d).toInt(), margin, (3 * d).toInt())
        }
        return button.also { it.layoutParams = params }
    }

    private fun commit(value: String) {
        currentInputConnection?.commitText(value, 1)
    }
}

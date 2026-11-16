package com.glasstype.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private class GlyphView(context: Context, private val kind: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = minOf(w, h) * 0.27f
        val cx = w / 2f
        val cy = h / 2f
        paint.strokeWidth = s * 0.12f
        if (kind == 0) {
            // Ikon glob
            canvas.drawCircle(cx, cy, s, paint)
            canvas.drawLine(cx - s, cy, cx + s, cy, paint)
            canvas.drawLine(cx, cy - s, cx, cy + s, paint)
            canvas.drawOval(RectF(cx - s * 0.45f, cy - s, cx + s * 0.45f, cy + s), paint)
        } else {
            // Ikon mikrofon
            canvas.drawRoundRect(
                RectF(cx - s * 0.32f, cy - s, cx + s * 0.32f, cy + s * 0.15f),
                s * 0.32f, s * 0.32f, paint
            )
            canvas.drawArc(
                RectF(cx - s * 0.62f, cy - s * 0.45f, cx + s * 0.62f, cy + s * 0.55f),
                0f, 180f, false, paint
            )
            canvas.drawLine(cx, cy + s * 0.55f, cx, cy + s * 0.95f, paint)
            canvas.drawLine(cx - s * 0.35f, cy + s * 0.95f, cx + s * 0.35f, cy + s * 0.95f, paint)
        }
    }
}

class GlassTypeImeService : InputMethodService() {
    private var caps = false
    private lateinit var root: LinearLayout
    private lateinit var keyRows: LinearLayout

    override fun onCreate() {
        super.onCreate()
        applyLiveBlur()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        applyLiveBlur()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyLiveBlur()
        if (::keyRows.isInitialized) buildKeys()
    }

    private fun prefs() = getSharedPreferences("glasstype", Context.MODE_PRIVATE)

    private fun keyHeightDp(): Int = prefs().getInt("key_height_dp", 45).coerceIn(36, 70)

    private fun changeHeight(delta: Int) {
        val v = (keyHeightDp() + delta).coerceIn(36, 70)
        prefs().edit().putInt("key_height_dp", v).apply()
        if (::keyRows.isInitialized) buildKeys()
    }

    private fun applyLiveBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val w = window?.window ?: return
                w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val lp = w.attributes
                lp.blurBehindRadius = 40
                lp.dimAmount = 0f
                w.attributes = lp
                w.setBackgroundBlurRadius(60)
            } catch (_: Exception) {
                // Some Android manufacturers disable cross-window blur.
            }
        }
    }

    private fun glassShape(fill: Int, radiusPx: Float, strokeColor: Int, strokePx: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx
            setStroke(strokePx, strokeColor)
        }
    }

    private fun switchKeyboard() {
        try {
            var switched = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switched = switchToNextInputMethod(false)
            }
            if (!switched) {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        } catch (_: Exception) {
        }
    }

    private fun startVoice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val map = imm.shortcutInputMethodsAndSubtypes
                for ((info, subtypes) in map) {
                    for (st in subtypes) {
                        if (st.mode == "voice") {
                            switchInputMethod(info.id, st)
                            return
                        }
                    }
                }
            }
            Toast.makeText(this, "Input suara tidak tersedia", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Input suara tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildBottomBar(): LinearLayout {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(10), dp(2), dp(10), 0)

        val globe = GlyphView(this, 0)
        globe.setOnClickListener { switchKeyboard() }

        val center = FrameLayout(this)
        val pill = View(this)
        val pillBg = GradientDrawable()
        pillBg.setColor(Color.argb(235, 255, 255, 255))
        pillBg.cornerRadius = dp(3).toFloat()
        pill.background = pillBg
        center.addView(pill, FrameLayout.LayoutParams(dp(134), dp(5), Gravity.CENTER))

        val mic = GlyphView(this, 1)
        mic.setOnClickListener { startVoice() }

        bar.addView(globe, LinearLayout.LayoutParams(dp(44), dp(44)))
        bar.addView(center, LinearLayout.LayoutParams(0, dp(44), 1f))
        bar.addView(mic, LinearLayout.LayoutParams(dp(44), dp(44)))
        return bar
    }

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(8), dp(5), dp(2))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(215, 34, 48, 64), Color.argb(245, 14, 22, 34))
            ).apply {
                cornerRadii = floatArrayOf(
                    dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(),
                    0f, 0f, 0f, 0f
                )
                setStroke(dp(1), Color.argb(55, 255, 255, 255))
            }
        }

        fun tool(label: String, onClick: () -> Unit): TextView {
            val t = TextView(this)
            t.text = label
            t.textSize = 20f
            t.gravity = Gravity.CENTER
            t.setTextColor(Color.rgb(210, 216, 228))
            t.setPadding(dp(16), dp(2), dp(16), dp(2))
            t.setOnClickListener { onClick() }
            return t
        }

        val toolbar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(5), 0, dp(5), dp(5))
        }
        val logo = TextView(this).apply {
            text = "GlassType"
            textSize = 13f
            setTextColor(Color.rgb(210, 216, 228))
            typeface = Typeface.DEFAULT_BOLD
        }
        val spacer = View(this)
        toolbar.addView(logo)
        toolbar.addView(spacer, LinearLayout.LayoutParams(0, dp(24), 1f))
        toolbar.addView(tool("−") { changeHeight(-4) })
        toolbar.addView(tool("+") { changeHeight(4) })
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
                setTextColor(Color.WHITE)
            }
            suggestions.addView(t, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
        root.addView(suggestions)

        keyRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keyRows)
        buildKeys()
        root.addView(buildBottomBar())
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
        "zxcvbnm".forEach { c ->
            val label = if (caps) c.uppercase() else c.toString()
            third.addView(makeKey(label, 1f) { commit(label) })
        }
        third.addView(makeKey("⌫", 1.35f, true) { currentInputConnection?.deleteSurroundingText(1, 0) })
        keyRows.addView(third)
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottom.addView(makeKey("123", 1.6f, true) {})
        bottom.addView(makeKey("English", 5f, false) { commit(" ") })
        bottom.addView(makeKey("↵", 1.6f, true) { currentInputConnection?.commitText("\n", 1) })
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
        val heightPx = (keyHeightDp() * d).toInt()
        val radius = 7f * d
        val strokePx = (1 * d).toInt().coerceAtLeast(1)
        val stroke = Color.argb(35, 255, 255, 255)

        val normal = if (utility) Color.argb(75, 120, 130, 150) else Color.argb(100, 155, 165, 182)
        val pressed = if (utility) Color.argb(150, 170, 180, 200) else Color.argb(175, 205, 212, 225)

        val states = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), glassShape(pressed, radius, stroke, strokePx))
            addState(intArrayOf(), glassShape(normal, radius, stroke, strokePx))
        }

        val button = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = if (utility) 13f else 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            background = states
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(0, heightPx, weight).apply {
            val margin = (2 * d).toInt()
            setMargins(margin, (3 * d).toInt(), margin, (3 * d).toInt())
        }
        button.layoutParams = params
        return button
    }

    private fun commit(value: String) {
        currentInputConnection?.commitText(value, 1)
    }
}

package com.glasstype.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

private class GlyphView(context: Context, private val kind: Int, color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeCap = Paint.Cap.ROUND
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

private class Palette(
    val bgTop: Int,
    val bgBottom: Int,
    val stroke: Int,
    val key: Int,
    val keyUtil: Int,
    val keyDown: Int,
    val keyUtilDown: Int,
    val keyStroke: Int,
    val text: Int,
    val icon: Int
)

class GlassTypeImeService : InputMethodService() {
    private var caps = false
    private var mode = 0 // 0 huruf, 1 nombor, 2 simbol, 3 emoji
    private var emojiCat = 0
    private var lastSig = ""
    private lateinit var root: LinearLayout
    private lateinit var keyRows: LinearLayout
    private lateinit var pal: Palette

    private var snapView: ImageView? = null
    private var snapTint: View? = null
    private var snapBmp: Bitmap? = null

    private val emojiTabs = listOf("😀", "👍", "💖", "🐶", "🍔")
    private val emojiSets: List<List<String>> = listOf(
        listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
            "😘", "😗", "😙", "😚", "😋", "😛", "😜", "🤪",
            "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "😐",
            "😑", "😶", "😏", "😒", "🙄", "😬", "😮", "😯",
            "😲", "😳", "🥺", "😢", "😭", "😤", "😠", "😡",
            "😱", "😨", "😰", "😥", "😓", "🤯", "😴", "🤒"
        ),
        listOf(
            "👍", "👎", "👌", "🤞", "🤟", "🤘", "👏", "🙌",
            "👐", "🤝", "🙏", "💪", "👋", "🤚", "✋", "🖐",
            "👊", "✊", "🤛", "🤜", "👆", "👇", "👈", "👉",
            "🤙", "🖖", "👀", "👅", "👄", "🧠", "👶", "👦"
        ),
        listOf(
            "\u2764\uFE0F", "💖", "💗", "💓", "💞", "💕", "💘", "💝",
            "💔", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "🤎", "✨", "⭐", "🌟", "💫", "🔥", "💯", "🎉",
            "🎊", "✅", "❌", "💥", "💢", "💤", "💬", "🎁"
        ),
        listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈",
            "🙉", "🙊", "🐔", "🐧", "🐦", "🐤", "🦆", "🦅",
            "🦉", "🐺", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌",
            "🐞", "🐢", "🐍", "🐙", "🐠", "🐬", "🐳", "🌸"
        ),
        listOf(
            "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒",
            "🍑", "🍍", "🥭", "🥥", "🥑", "🍅", "🥕", "🌽",
            "🍞", "🧀", "🍔", "🍟", "🍕", "🌭", "🌮", "🍜",
            "🍣", "🍩", "🍪", "🎂", "🍰", "🍫", "🍿", "☕",
            "🍵", "🥤", "🍺", "🍚", "🍗", "🥚", "🍦", "🍯"
        )
    )

    // ---------- Kitaran hayat ----------

    override fun onCreate() {
        super.onCreate()
        applyLiveBlur()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        applyLiveBlur()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting &&
            prefs().getBoolean("snap_bg", false) &&
            !isInputViewShown &&
            !isPasswordField(attribute)
        ) {
            GlassCaptureService.capture { bmp ->
                if (bmp != null) applySnap(bmp)
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyLiveBlur()
        if (!restarting) {
            caps = false
            val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
            mode = if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) 1 else 0
        }
        if (::keyRows.isInitialized) {
            if (settingsSignature() != lastSig) {
                setInputView(onCreateInputView())
            } else {
                buildKeys()
            }
        }
    }

    // ---------- Tetapan ----------

    private fun prefs() = getSharedPreferences("glasstype", Context.MODE_PRIVATE)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun keyHeightDp(): Int = prefs().getInt("key_height_dp", 45).coerceIn(36, 70)

    private fun settingsSignature(): String = prefs().all.toString() + isDark()

    private fun isDark(): Boolean {
        return when (prefs().getInt("theme", 0)) {
            1 -> false
            2 -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            else -> true
        }
    }

    private fun withAlpha(c: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))

    private fun palette(): Palette {
        val f = prefs().getInt("opacity", 85).coerceIn(20, 100) / 100f
        fun a(base: Int): Int = (base * f).toInt().coerceIn(0, 255)
        return if (isDark()) {
            Palette(
                bgTop = Color.argb(a(253), 44, 46, 50),
                bgBottom = Color.argb(a(255), 28, 30, 34),
                stroke = Color.argb(55, 255, 255, 255),
                key = Color.argb(a(118), 155, 165, 182),
                keyUtil = Color.argb(a(88), 120, 130, 150),
                keyDown = Color.argb(a(206), 205, 212, 225),
                keyUtilDown = Color.argb(a(176), 170, 180, 200),
                keyStroke = Color.argb(35, 255, 255, 255),
                text = Color.WHITE,
                icon = Color.WHITE
            )
        } else {
            Palette(
                bgTop = Color.argb(a(253), 246, 248, 253),
                bgBottom = Color.argb(a(255), 228, 233, 245),
                stroke = Color.argb(200, 255, 255, 255),
                key = Color.argb(a(235), 255, 255, 255),
                keyUtil = Color.argb(a(200), 178, 190, 213),
                keyDown = Color.argb(a(255), 226, 231, 243),
                keyUtilDown = Color.argb(a(240), 150, 165, 195),
                keyStroke = Color.argb(40, 90, 100, 120),
                text = Color.rgb(38, 43, 53),
                icon = Color.rgb(60, 68, 85)
            )
        }
    }

    private fun changeHeight(delta: Int) {
        val v = (keyHeightDp() + delta).coerceIn(36, 70)
        prefs().edit().putInt("key_height_dp", v).apply()
        lastSig = settingsSignature()
        if (::keyRows.isInitialized) buildKeys()
    }

    private fun haptic(v: View) {
        if (prefs().getBoolean("haptic", true)) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun isPasswordField(info: EditorInfo?): Boolean {
        val t = info?.inputType ?: return false
        val cls = t and InputType.TYPE_MASK_CLASS
        val v = t and InputType.TYPE_MASK_VARIATION
        return when (cls) {
            InputType.TYPE_CLASS_TEXT ->
                v == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                    v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> v == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    // ---------- Blur ----------

    private fun applyLiveBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val live = prefs().getBoolean("live_blur", false)
                val w = window?.window ?: return
                w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val lp = w.attributes
                if (live) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    lp.blurBehindRadius = 40
                    lp.dimAmount = 0f
                    w.attributes = lp
                    w.setBackgroundBlurRadius(60)
                } else {
                    w.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    lp.blurBehindRadius = 0
                    w.attributes = lp
                    w.setBackgroundBlurRadius(0)
                }
            } catch (_: Exception) {
                // Sesetengah pengeluar mematikan blur antara tetingkap.
            }
        }
    }

    private fun softBlur(src: Bitmap, level: Int): Bitmap {
        if (level <= 0) return src
        val div = 2 + level / 4
        val w = (src.width / div).coerceAtLeast(2)
        val h = (src.height / div).coerceAtLeast(2)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun fitSnap() {
        val iv = snapView ?: return
        val bmp = snapBmp ?: return
        val vw = iv.width.toFloat()
        val vh = iv.height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val s = vw / bmp.width
        val m = Matrix()
        m.postScale(s, s)
        m.postTranslate(0f, vh - bmp.height * s)
        iv.imageMatrix = m
    }

    private fun applySnap(bmp: Bitmap) {
        val iv = snapView ?: return
        val shown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bmp
        } else {
            softBlur(bmp, prefs().getInt("snap_blur", 70).coerceIn(0, 100))
        }
        snapBmp = shown
        iv.setImageBitmap(shown)
        iv.visibility = View.VISIBLE
        snapTint?.visibility = View.VISIBLE
        fitSnap()
    }

    // ---------- Tindakan ----------

    private fun commit(value: String) {
        currentInputConnection?.commitText(value, 1)
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    private fun enterLabel(): String {
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        if (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return "return"
        return when (opts and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "search"
            EditorInfo.IME_ACTION_GO -> "go"
            EditorInfo.IME_ACTION_SEND -> "send"
            EditorInfo.IME_ACTION_NEXT -> "next"
            EditorInfo.IME_ACTION_DONE -> "done"
            else -> "return"
        }
    }

    private fun pressEnter() {
        val ic = currentInputConnection ?: return
        val opts = currentInputEditorInfo?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        if (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0 ||
            action == EditorInfo.IME_ACTION_NONE ||
            action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            ic.commitText("\n", 1)
        } else {
            ic.performEditorAction(action)
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
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            for (info in imm.enabledInputMethodList) {
                for (st in imm.getEnabledInputMethodSubtypeList(info, true)) {
                    if (st.mode == "voice") {
                        switchInputMethod(info.id, st)
                        return
                    }
                }
            }
        } catch (_: Exception) {
        }
        Toast.makeText(this, "Input suara tidak tersedia", Toast.LENGTH_SHORT).show()
    }

    // ---------- Paparan ----------

    override fun onCreateInputView(): View {
        pal = palette()
        lastSig = settingsSignature()
        val match = ViewGroup.LayoutParams.MATCH_PARENT

        val frame = FrameLayout(this)
        snapView = null
        snapTint = null
        snapBmp = null

        // Lapisan asas: gradien lutsinar
        val base = View(this)
        base.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(pal.bgTop, pal.bgBottom)
        )
        frame.addView(base, FrameLayout.LayoutParams(match, match))

        // Lapisan tangkapan skrin yang dikaburkan (gaya Vivo)
        if (prefs().getBoolean("snap_bg", false)) {
            val iv = ImageView(this)
            iv.scaleType = ImageView.ScaleType.MATRIX
            iv.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val level = prefs().getInt("snap_blur", 70).coerceIn(0, 100)
                if (level > 0) {
                    val r = level * resources.displayMetrics.density * 0.3f
                    iv.setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP))
                }
            }
            iv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitSnap() }
            frame.addView(iv, FrameLayout.LayoutParams(match, match))

            val tintAlpha = prefs().getInt("snap_tint", 55).coerceIn(0, 100) * 255 / 100
            val tint = View(this)
            tint.setBackgroundColor(withAlpha(pal.bgBottom, tintAlpha))
            tint.visibility = View.GONE
            frame.addView(tint, FrameLayout.LayoutParams(match, match))

            snapView = iv
            snapTint = tint
            val existing = GlassCaptureService.snapshot
            if (existing != null) applySnap(existing)
        }

        val line = View(this)
        line.setBackgroundColor(pal.stroke)
        frame.addView(line, FrameLayout.LayoutParams(match, dp(1), Gravity.TOP))

        if (prefs().getBoolean("round_top", false)) {
            val r = dp(22)
            frame.clipToOutline = true
            frame.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height + r, r.toFloat())
                }
            }
        }

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(5), dp(8), dp(5), 0)

        if (prefs().getBoolean("show_toolbar", true)) {
            root.addView(buildToolbar())
        }

        keyRows = LinearLayout(this)
        keyRows.orientation = LinearLayout.VERTICAL
        root.addView(keyRows)
        buildKeys()
        root.addView(buildBottomBar())

        frame.addView(root, FrameLayout.LayoutParams(match, ViewGroup.LayoutParams.WRAP_CONTENT))
        return frame
    }

    private fun tool(label: String, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = 20f
        t.gravity = Gravity.CENTER
        t.setTextColor(pal.icon)
        t.setPadding(dp(16), dp(2), dp(16), dp(2))
        t.setOnClickListener { onClick() }
        return t
    }

    private fun buildToolbar(): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(5), 0, dp(5), dp(5))
        val logo = TextView(this)
        logo.text = "GlassType"
        logo.textSize = 13f
        logo.setTextColor(pal.icon)
        logo.typeface = Typeface.DEFAULT_BOLD
        bar.addView(logo)
        bar.addView(View(this), LinearLayout.LayoutParams(0, dp(24), 1f))
        bar.addView(tool("−") { changeHeight(-4) })
        bar.addView(tool("+") { changeHeight(4) })
        return bar
    }

    private fun buildBottomBar(): LinearLayout {
        val h = prefs().getInt("bottom_dp", 64).coerceIn(30, 220)
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.VERTICAL
        bar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(h))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(10), 0, dp(10), 0)

        val globe = GlyphView(this, 0, pal.icon)
        globe.setOnClickListener { haptic(it); switchKeyboard() }
        val mic = GlyphView(this, 1, pal.icon)
        mic.setOnClickListener { haptic(it); startVoice() }

        row.addView(globe, LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT))
        row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        row.addView(mic, LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT))

        val pillBox = FrameLayout(this)
        val pill = View(this)
        val pillBg = GradientDrawable()
        pillBg.setColor(pal.icon)
        pillBg.cornerRadius = dp(3).toFloat()
        pill.background = pillBg
        pill.alpha = 0.9f
        pillBox.addView(pill, FrameLayout.LayoutParams(dp(134), dp(5), Gravity.CENTER))

        bar.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        bar.addView(pillBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)))
        return bar
    }

    // ---------- Kekunci ----------

    private fun buildKeys() {
        keyRows.removeAllViews()
        when (mode) {
            0 -> buildLetters()
            1 -> buildSymbolLayer(false)
            2 -> buildSymbolLayer(true)
            else -> buildEmoji()
        }
    }

    private fun newRow(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        keyRows.addView(row)
        return row
    }

    private fun addChars(row: LinearLayout, chars: List<String>, weight: Float) {
        for (c in chars) {
            val label = if (mode == 0 && caps) c.uppercase() else c
            row.addView(makeKey(label, weight) {
                commit(label)
                if (mode == 0 && caps) {
                    caps = false
                    buildKeys()
                }
            })
        }
    }

    private fun addBottomRow() {
        val row = newRow()
        row.addView(makeKey(if (mode == 0) "123" else "ABC", 1.6f, true) {
            mode = if (mode == 0) 1 else 0
            buildKeys()
        })
        row.addView(makeKey("😊", 1.2f, true) {
            mode = 3
            buildKeys()
        })
        row.addView(makeKey("space", 5f) { commit(" ") })
        row.addView(makeKey(enterLabel(), 2.4f, true) { pressEnter() })
    }

    private fun buildLetters() {
        addChars(newRow(), "qwertyuiop".map { it.toString() }, 1f)

        val r2 = newRow()
        r2.addView(View(this), LinearLayout.LayoutParams(0, 1, 0.5f))
        addChars(r2, "asdfghjkl".map { it.toString() }, 1f)
        r2.addView(View(this), LinearLayout.LayoutParams(0, 1, 0.5f))

        val r3 = newRow()
        r3.addView(makeKey(if (caps) "⬆" else "⇧", 1.4f, true) {
            caps = !caps
            buildKeys()
        })
        addChars(r3, "zxcvbnm".map { it.toString() }, 1f)
        r3.addView(makeKey("⌫", 1.4f, true, true) { backspace() })

        addBottomRow()
    }

    private fun buildSymbolLayer(second: Boolean) {
        val r1 = if (!second) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        } else {
            listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
        }
        val r2 = if (!second) {
            listOf("-", "/", ":", ";", "(", ")", "\$", "&", "@", "\"")
        } else {
            listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
        }
        val r3 = listOf(".", ",", "?", "!", "'")

        addChars(newRow(), r1, 1f)
        addChars(newRow(), r2, 1f)

        val row = newRow()
        row.addView(makeKey(if (!second) "#+=" else "123", 1.5f, true) {
            mode = if (!second) 2 else 1
            buildKeys()
        })
        addChars(row, r3, 1.4f)
        row.addView(makeKey("⌫", 1.5f, true, true) { backspace() })

        addBottomRow()
    }

    private fun buildEmoji() {
        val d = resources.displayMetrics.density
        val cellH = dp(keyHeightDp() + 4)
        val scroll = ScrollView(this)
        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        val items = emojiSets[emojiCat]
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (j in 0 until 8) {
                val e = items.getOrNull(i + j)
                val tv = TextView(this)
                tv.text = e ?: ""
                tv.textSize = 24f
                tv.gravity = Gravity.CENTER
                if (e != null) {
                    tv.setOnClickListener {
                        haptic(tv)
                        commit(e)
                    }
                }
                row.addView(tv, LinearLayout.LayoutParams(0, cellH, 1f))
            }
            grid.addView(row)
            i += 8
        }
        scroll.addView(grid)
        val areaH = ((keyHeightDp() + 6) * 3 * d).toInt()
        keyRows.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, areaH))

        val row = newRow()
        row.addView(makeKey("ABC", 1.6f, true) {
            mode = 0
            buildKeys()
        })
        emojiTabs.forEachIndexed { idx, tab ->
            row.addView(makeKey(tab, 1f, idx == emojiCat) {
                emojiCat = idx
                buildKeys()
            })
        }
        row.addView(makeKey("⌫", 1.6f, true, true) { backspace() })
    }

    private fun glassShape(fill: Int, radiusPx: Float, strokeColor: Int, strokePx: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(fill)
        g.cornerRadius = radiusPx
        g.setStroke(strokePx, strokeColor)
        return g
    }

    private fun makeKey(
        label: String,
        weight: Float,
        utility: Boolean = false,
        repeat: Boolean = false,
        action: () -> Unit
    ): Button {
        val d = resources.displayMetrics.density
        val heightPx = (keyHeightDp() * d).toInt()
        val radius = prefs().getInt("radius_dp", 7).coerceIn(0, 16) * d
        val strokePx = (1 * d).toInt().coerceAtLeast(1)

        val normal = if (utility) pal.keyUtil else pal.key
        val pressed = if (utility) pal.keyUtilDown else pal.keyDown

        val states = StateListDrawable()
        states.addState(
            intArrayOf(android.R.attr.state_pressed),
            glassShape(pressed, radius, pal.keyStroke, strokePx)
        )
        states.addState(intArrayOf(), glassShape(normal, radius, pal.keyStroke, strokePx))

        val small = label.length > 1 && label.all { it.code < 128 }

        val button = Button(this)
        button.text = label
        button.isAllCaps = false
        button.textSize = if (small) 14f else 18f
        button.setTextColor(pal.text)
        button.setPadding(0, 0, 0, 0)
        button.background = states
        button.stateListAnimator = null
        button.minHeight = 0
        button.minimumHeight = 0
        button.minWidth = 0
        button.minimumWidth = 0

        if (repeat) {
            val handler = Handler(Looper.getMainLooper())
            val runner = object : Runnable {
                override fun run() {
                    action()
                    handler.postDelayed(this, 60)
                }
            }
            button.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        haptic(v)
                        action()
                        handler.postDelayed(runner, 400)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        handler.removeCallbacks(runner)
                    }
                }
                true
            }
        } else {
            button.setOnClickListener {
                haptic(it)
                action()
            }
        }

        val params = LinearLayout.LayoutParams(0, heightPx, weight)
        val margin = (2 * d).toInt()
        params.setMargins(margin, (3 * d).toInt(), margin, (3 * d).toInt())
        button.layoutParams = params
        return button
    }
}

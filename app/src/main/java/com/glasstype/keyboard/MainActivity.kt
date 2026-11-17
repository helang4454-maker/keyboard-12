package com.glasstype.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private var svcStatus: TextView? = null

    private fun label(text: String, size: Float, color: Int, top: Int): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        t.setPadding(0, top, 0, 0)
        return t
    }

    private fun slider(
        prefs: SharedPreferences, title: String, key: String,
        min: Int, max: Int, def: Int, unit: String, pad: Int
    ): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(0, pad / 2, 0, 0)
        val cur = prefs.getInt(key, def).coerceIn(min, max)
        val text = TextView(this)
        text.text = "$title: $cur$unit"
        text.textSize = 15f
        text.setTextColor(Color.rgb(55, 62, 76))
        val seek = SeekBar(this)
        seek.max = max - min
        seek.progress = cur - min
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                val v = p + min
                text.text = "$title: $v$unit"
                prefs.edit().putInt(key, v).apply()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        box.addView(text)
        box.addView(seek)
        return box
    }

    private fun toggle(prefs: SharedPreferences, title: String, key: String, def: Boolean): Switch {
        val sw = Switch(this)
        sw.text = title
        sw.textSize = 15f
        sw.setTextColor(Color.rgb(55, 62, 76))
        sw.isChecked = prefs.getBoolean(key, def)
        sw.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(key, checked).apply()
        }
        return sw
    }

    private fun refreshSvcStatus() {
        val on = GlassCaptureService.instance != null
        svcStatus?.text = if (on) {
            "Servis tangkap skrin: AKTIF"
        } else {
            "Servis tangkap skrin: belum aktif"
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSvcStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 22).toInt()
        val prefs = getSharedPreferences("glasstype", Context.MODE_PRIVATE)
        val w = ViewGroup.LayoutParams.MATCH_PARENT
        val h = ViewGroup.LayoutParams.WRAP_CONTENT
        val dark = Color.rgb(35, 43, 60)
        val grey = Color.rgb(90, 99, 115)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad, pad, pad)
        root.setBackgroundColor(Color.rgb(245, 247, 252))

        val title = label("GlassType", 30f, dark, 0)
        title.gravity = Gravity.CENTER
        val desc = label(
            "Aktifkan GlassType, kemudian pilih sebagai keyboard. Tetapan di bawah dipakai bila keyboard dibuka semula.",
            15f, grey, pad / 2
        )
        desc.gravity = Gravity.CENTER

        val enable = Button(this)
        enable.text = "1. Aktifkan GlassType"
        enable.setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        val choose = Button(this)
        choose.text = "2. Pilih GlassType"
        choose.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }

        root.addView(title, w, h)
        root.addView(desc, w, h)
        root.addView(enable, w, h)
        root.addView(choose, w, h)

        // Tema
        root.addView(label("Tema", 17f, dark, pad), w, h)
        val group = RadioGroup(this)
        val names = listOf("Gelap", "Cerah", "Ikut sistem")
        names.forEachIndexed { i, n ->
            val rb = RadioButton(this)
            rb.id = 1000 + i
            rb.text = n
            rb.setTextColor(Color.rgb(55, 62, 76))
            group.addView(rb)
        }
        group.check(1000 + prefs.getInt("theme", 0).coerceIn(0, 2))
        group.setOnCheckedChangeListener { _, id ->
            prefs.edit().putInt("theme", id - 1000).apply()
        }
        root.addView(group, w, h)

        // Latar gaya Vivo
        root.addView(label("Latar gaya Vivo (tangkap skrin)", 17f, dark, pad), w, h)
        val status = label("", 14f, grey, pad / 3)
        svcStatus = status
        refreshSvcStatus()
        root.addView(status, w, h)
        val openAcc = Button(this)
        openAcc.text = "Buka tetapan Kebolehcapaian"
        openAcc.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        root.addView(openAcc, w, h)
        root.addView(toggle(prefs, "Guna blur latar dari skrin terkini", "snap_bg", false), w, h)
        root.addView(slider(prefs, "Tahap blur", "snap_blur", 0, 100, 70, "%", pad), w, h)
        root.addView(slider(prefs, "Lapisan warna gelap/cerah", "snap_tint", 0, 100, 55, "%", pad), w, h)
        val note = label(
            "Cara aktifkan: Kebolehcapaian, cari GlassType Screen Capture, hidupkan. " +
                "Jika disekat, buka Maklumat aplikasi GlassType, tekan menu ⋮ di penjuru, " +
                "pilih Benarkan tetapan terhad, kemudian cuba semula.\n\n" +
                "Privasi: tangkapan skrin hanya disimpan dalam memori untuk dikaburkan. " +
                "Ia tidak disimpan ke fail dan tidak dihantar ke mana-mana kerana aplikasi ini " +
                "tiada kebenaran Internet. Medan kata laluan tidak ditangkap, dan aplikasi yang " +
                "melindungi skrin tidak boleh ditangkap.",
            13f, Color.rgb(105, 112, 126), pad / 2
        )
        root.addView(note, w, h)

        // Saiz dan rupa
        root.addView(label("Saiz dan rupa", 17f, dark, pad), w, h)
        root.addView(slider(prefs, "Kelegapan kekunci (rendah = lebih lutsinar)", "opacity", 20, 100, 85, "%", pad), w, h)
        root.addView(slider(prefs, "Ketinggian kekunci", "key_height_dp", 36, 70, 45, " dp", pad), w, h)
        root.addView(slider(prefs, "Ketinggian bahagian bawah", "bottom_dp", 30, 220, 64, " dp", pad), w, h)
        root.addView(slider(prefs, "Bucu kekunci", "radius_dp", 0, 16, 7, " dp", pad), w, h)

        // Pilihan
        root.addView(label("Pilihan", 17f, dark, pad), w, h)
        root.addView(toggle(prefs, "Getaran bila tekan kekunci", "haptic", true), w, h)
        root.addView(toggle(prefs, "Bucu atas keyboard bulat", "round_top", false), w, h)
        root.addView(toggle(prefs, "Papar bar GlassType (− dan +)", "show_toolbar", true), w, h)
        root.addView(toggle(prefs, "Blur langsung (live), jika telefon sokong", "live_blur", false), w, h)

        // Kotak ujian
        root.addView(label("Cuba keyboard", 17f, dark, pad), w, h)
        val test = EditText(this)
        test.hint = "Taip di sini untuk cuba"
        root.addView(test, w, h)

        val privacy = label(
            "Privasi: aplikasi ini tidak mempunyai kebenaran Internet dan tidak menyimpan teks yang ditaip.",
            13f, Color.rgb(105, 112, 126), pad
        )
        privacy.gravity = Gravity.CENTER
        root.addView(privacy, w, h)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(245, 247, 252))
        scroll.addView(root, w, h)
        setContentView(scroll)
    }
}

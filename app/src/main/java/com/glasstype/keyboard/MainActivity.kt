package com.glasstype.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 22).toInt()
        val prefs = getSharedPreferences("glasstype", Context.MODE_PRIVATE)
        val minH = 36
        val current = prefs.getInt("key_height_dp", 45).coerceIn(36, 70)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.rgb(245, 247, 252))
        }
        val title = TextView(this).apply {
            text = "GlassType"
            textSize = 30f
            setTextColor(Color.rgb(35, 43, 60))
            gravity = Gravity.CENTER
        }
        val desc = TextView(this).apply {
            text = "Keyboard ala iOS dengan kesan Live Background Blur.\n\nAktifkan GlassType, kemudian pilih sebagai keyboard lalai."
            textSize = 16f
            setTextColor(Color.rgb(90, 99, 115))
            gravity = Gravity.CENTER
            setPadding(0, pad / 2, 0, pad)
        }
        val enable = Button(this).apply {
            text = "1. Aktifkan GlassType"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        val choose = Button(this).apply {
            text = "2. Pilih GlassType"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        }
        val heightLabel = TextView(this).apply {
            text = "Ketinggian kekunci: $current dp"
            textSize = 15f
            setTextColor(Color.rgb(55, 62, 76))
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        val seek = SeekBar(this).apply {
            max = 34
            progress = current - minH
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = p + minH
                    heightLabel.text = "Ketinggian kekunci: $v dp"
                    prefs.edit().putInt("key_height_dp", v).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val privacy = TextView(this).apply {
            text = "Privasi: demo ini tidak mempunyai kebenaran Internet dan tidak menyimpan teks yang ditaip."
            textSize = 13f
            setTextColor(Color.rgb(105, 112, 126))
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        val w = ViewGroup.LayoutParams.MATCH_PARENT
        val h = ViewGroup.LayoutParams.WRAP_CONTENT
        root.addView(title, w, h)
        root.addView(desc, w, h)
        root.addView(enable, w, h)
        root.addView(choose, w, h)
        root.addView(heightLabel, w, h)
        root.addView(seek, w, h)
        root.addView(privacy, w, h)
        setContentView(root)
    }
}

package com.glasstype.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (resources.displayMetrics.density * 22).toInt()
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
        val privacy = TextView(this).apply {
            text = "Privasi: demo ini tidak mempunyai kebenaran Internet dan tidak menyimpan teks yang ditaip."
            textSize = 13f
            setTextColor(Color.rgb(105, 112, 126))
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        root.addView(title, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(desc, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(enable, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(choose, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(privacy, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setContentView(root)
    }
}

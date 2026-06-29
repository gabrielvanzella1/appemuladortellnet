package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.VtAttrMapOptions

class VtAttrMapActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var valBoldMode: TextView
    private lateinit var valBlinkMode: TextView
    private lateinit var checkUnderline: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_vt_attr_map)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        valBoldMode   = findViewById(R.id.val_bold_mode)
        valBlinkMode  = findViewById(R.id.val_blink_mode)
        checkUnderline = findViewById(R.id.check_underline)

        loadValues()
        setupListeners()
    }

    private fun loadValues() {
        val opts = settings.vtAttrMap
        valBoldMode.text   = opts.boldMode
        valBlinkMode.text  = opts.blinkMode
        checkUnderline.isChecked = opts.underlineEnabled
    }

    private fun setupListeners() {
        val boldOptions = arrayOf("Negrito", "Cor brilhante", "Negrito+Cor", "Nenhum")
        val blinkOptions = arrayOf("Negrito", "Cor brilhante", "Nenhum")

        findViewById<View>(R.id.row_bold_mode).setOnClickListener {
            pickOption("Negrito", boldOptions, settings.vtAttrMap.boldMode) { picked ->
                settings.vtAttrMap = settings.vtAttrMap.copy(boldMode = picked)
                valBoldMode.text = picked
            }
        }

        findViewById<View>(R.id.row_underline).setOnClickListener {
            checkUnderline.isChecked = !checkUnderline.isChecked
            settings.vtAttrMap = settings.vtAttrMap.copy(underlineEnabled = checkUnderline.isChecked)
        }

        findViewById<View>(R.id.row_blink_mode).setOnClickListener {
            pickOption("Piscante", blinkOptions, settings.vtAttrMap.blinkMode) { picked ->
                settings.vtAttrMap = settings.vtAttrMap.copy(blinkMode = picked)
                valBlinkMode.text = picked
            }
        }
    }

    private fun pickOption(title: String, options: Array<String>, current: String, onPick: (String) -> Unit) {
        val checked = options.indexOf(current).takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                onPick(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

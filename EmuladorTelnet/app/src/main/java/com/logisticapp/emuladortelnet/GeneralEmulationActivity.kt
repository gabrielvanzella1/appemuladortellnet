package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.GeneralEmulationOptions

class GeneralEmulationActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: GeneralEmulationOptions

    private lateinit var ckDestructiveBs: CheckBox
    private lateinit var valCaptureCr: TextView
    private lateinit var etScrollLength: EditText
    private lateinit var valInitialWidth: TextView
    private lateinit var etInitialHeight: EditText

    private val captureOnCrOptions = arrayOf("Desativado", "LF", "CR+LF")
    private val initialWidthOptions = arrayOf("80", "132")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_general_emulation)
        opts = settings.generalEmulationOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        bindViews()
        loadValues()
        setupInteractions()
    }

    private fun bindViews() {
        ckDestructiveBs  = findViewById(R.id.ck_destructive_bs)
        valCaptureCr     = findViewById(R.id.val_capture_cr)
        etScrollLength   = findViewById(R.id.et_scroll_length)
        valInitialWidth  = findViewById(R.id.val_initial_width)
        etInitialHeight  = findViewById(R.id.et_initial_height)
    }

    private fun loadValues() {
        ckDestructiveBs.isChecked  = opts.destructiveBackspace
        valCaptureCr.text          = opts.captureOnCr
        etScrollLength.setText(opts.scrollLength.toString())
        valInitialWidth.text       = opts.initialWidth.toString()
        etInitialHeight.setText(opts.initialHeight.toString())
    }

    private fun setupInteractions() {
        findViewById<View>(R.id.row_destructive_bs).setOnClickListener { ckDestructiveBs.toggle() }

        findViewById<View>(R.id.row_capture_cr).setOnClickListener {
            pick("Capturar em CR", captureOnCrOptions, valCaptureCr.text.toString()) {
                valCaptureCr.text = it
            }
        }

        findViewById<View>(R.id.row_initial_width).setOnClickListener {
            pick("Largura inicial da tela", initialWidthOptions, valInitialWidth.text.toString()) {
                valInitialWidth.text = it
            }
        }
    }

    private fun pick(title: String, options: Array<String>, current: String, onPick: (String) -> Unit) {
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

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        settings.generalEmulationOptions = GeneralEmulationOptions(
            destructiveBackspace = ckDestructiveBs.isChecked,
            captureOnCr          = valCaptureCr.text.toString(),
            scrollLength         = etScrollLength.text.toString().toIntOrNull() ?: 32,
            initialWidth         = valInitialWidth.text.toString().toIntOrNull() ?: 80,
            initialHeight        = etInitialHeight.text.toString().toIntOrNull() ?: 24
        )
    }
}

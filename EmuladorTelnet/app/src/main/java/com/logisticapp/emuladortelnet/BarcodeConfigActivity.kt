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
import com.logisticapp.emuladortelnet.settings.BarcodeOptions

class BarcodeConfigActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: BarcodeOptions

    private lateinit var valDeviceType: TextView
    private lateinit var valAction: TextView
    private lateinit var valRemoveStart: TextView
    private lateinit var valRemoveEnd: TextView
    private lateinit var etTextBefore: EditText
    private lateinit var etTextAfter: EditText
    private lateinit var ckKeyboardMapping: CheckBox
    private lateinit var ckShowStatus: CheckBox

    private val deviceTypeOptions = arrayOf(
        "Honeywell", "Zebra", "Datalogic", "Bluebird", "Urovo",
        "Newland", "Sunmi", "Genérico"
    )
    private val actionOptions = arrayOf("Nenhum", "Enter", "Tab", "Enter + Tab")
    private val removeCountOptions = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_barcode_config)
        opts = settings.barcodeOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        bindViews()
        loadValues()
        setupInteractions()
    }

    private fun bindViews() {
        valDeviceType     = findViewById(R.id.val_device_type)
        valAction         = findViewById(R.id.val_action)
        valRemoveStart    = findViewById(R.id.val_remove_start)
        valRemoveEnd      = findViewById(R.id.val_remove_end)
        etTextBefore      = findViewById(R.id.et_text_before)
        etTextAfter       = findViewById(R.id.et_text_after)
        ckKeyboardMapping = findViewById(R.id.ck_keyboard_mapping)
        ckShowStatus      = findViewById(R.id.ck_show_status)
    }

    private fun loadValues() {
        valDeviceType.text     = opts.deviceType
        valAction.text         = opts.actionAfterScan
        valRemoveStart.text    = opts.removeCharsStart.toString()
        valRemoveEnd.text      = opts.removeCharsEnd.toString()
        etTextBefore.setText(opts.addTextBefore)
        etTextAfter.setText(opts.addTextAfter)
        ckKeyboardMapping.isChecked = opts.useKeyboardMapping
        ckShowStatus.isChecked      = opts.showOnStatusBar
    }

    private fun setupInteractions() {
        findViewById<View>(R.id.row_device_type).setOnClickListener {
            pick("Tipo de dispositivo", deviceTypeOptions, valDeviceType.text.toString()) {
                valDeviceType.text = it
            }
        }
        findViewById<View>(R.id.row_action).setOnClickListener {
            pick("Ação após verificação", actionOptions, valAction.text.toString()) {
                valAction.text = it
            }
        }
        findViewById<View>(R.id.row_remove_start).setOnClickListener {
            pick("Remover caracteres no início", removeCountOptions, valRemoveStart.text.toString()) {
                valRemoveStart.text = it
            }
        }
        findViewById<View>(R.id.row_remove_end).setOnClickListener {
            pick("Remover caracteres no final", removeCountOptions, valRemoveEnd.text.toString()) {
                valRemoveEnd.text = it
            }
        }
        findViewById<View>(R.id.row_keyboard_mapping).setOnClickListener { ckKeyboardMapping.toggle() }
        findViewById<View>(R.id.row_show_status).setOnClickListener { ckShowStatus.toggle() }
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
        settings.barcodeOptions = BarcodeOptions(
            deviceType          = valDeviceType.text.toString(),
            actionAfterScan     = valAction.text.toString(),
            removeCharsStart    = valRemoveStart.text.toString().toIntOrNull() ?: 0,
            removeCharsEnd      = valRemoveEnd.text.toString().toIntOrNull() ?: 0,
            addTextBefore       = etTextBefore.text.toString(),
            addTextAfter        = etTextAfter.text.toString(),
            useKeyboardMapping  = ckKeyboardMapping.isChecked,
            showOnStatusBar     = ckShowStatus.isChecked
        )
    }
}

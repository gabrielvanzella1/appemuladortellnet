package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.PrintOptions

class PrintConfigActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: PrintOptions

    private lateinit var valPrinterType: TextView
    private lateinit var etTimeout: EditText

    private val printerTypeOptions = arrayOf(
        "Padrão",
        "Epson ESC/POS",
        "Star",
        "Zebra ZPL",
        "Citizen",
        "Bixolon"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_print_config)
        opts = settings.printOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        valPrinterType = findViewById(R.id.val_printer_type)
        etTimeout      = findViewById(R.id.et_timeout)

        valPrinterType.text = opts.printerType
        etTimeout.setText(opts.timeoutSeconds.toString())

        findViewById<View>(R.id.row_printer_type).setOnClickListener {
            val checked = printerTypeOptions.indexOf(valPrinterType.text.toString()).takeIf { it >= 0 } ?: 0
            AlertDialog.Builder(this)
                .setTitle("Tipo de impressora")
                .setSingleChoiceItems(printerTypeOptions, checked) { dialog, which ->
                    valPrinterType.text = printerTypeOptions[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        settings.printOptions = PrintOptions(
            printerType    = valPrinterType.text.toString(),
            timeoutSeconds = etTimeout.text.toString().toIntOrNull() ?: 5
        )
    }
}

package com.logisticapp.emuladortelnet

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.PrintOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class PrintConfigActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings

    private lateinit var valConnectionType: TextView
    private lateinit var valBtDevice: TextView
    private lateinit var sectionBluetooth: LinearLayout
    private lateinit var sectionWifi: LinearLayout
    private lateinit var etWifiHost: EditText
    private lateinit var etWifiPort: EditText
    private lateinit var valPrinterType: TextView
    private lateinit var etTimeout: EditText

    private val connectionTypes = arrayOf("Bluetooth", "WiFi")

    private val printerTypeOptions = arrayOf(
        "Padrão",
        "Epson ESC/POS",
        "Star",
        "Zebra ZPL",
        "Citizen",
        "Bixolon"
    )

    private var selectedBtAddress = ""
    private var selectedBtName    = ""

    private val BT_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_print_config)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        valConnectionType = findViewById(R.id.val_connection_type)
        valBtDevice       = findViewById(R.id.val_bt_device)
        sectionBluetooth  = findViewById(R.id.section_bluetooth)
        sectionWifi       = findViewById(R.id.section_wifi)
        etWifiHost        = findViewById(R.id.et_wifi_host)
        etWifiPort        = findViewById(R.id.et_wifi_port)
        valPrinterType    = findViewById(R.id.val_printer_type)
        etTimeout         = findViewById(R.id.et_timeout)

        loadValues()
        setupListeners()
    }

    private fun loadValues() {
        val opts = settings.printOptions
        selectedBtAddress = opts.bluetoothAddress
        selectedBtName    = opts.bluetoothName

        valConnectionType.text = opts.connectionType
        valBtDevice.text = if (opts.bluetoothName.isNotBlank()) opts.bluetoothName
                           else "Nenhum selecionado"
        etWifiHost.setText(opts.wifiHost)
        etWifiPort.setText(opts.wifiPort.toString())
        valPrinterType.text = opts.printerType
        etTimeout.setText(opts.timeoutSeconds.toString())

        updateSectionVisibility(opts.connectionType)
    }

    private fun setupListeners() {
        // Tipo de conexão
        findViewById<View>(R.id.row_connection_type).setOnClickListener {
            val current = connectionTypes.indexOf(valConnectionType.text.toString()).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Tipo de conexão")
                .setSingleChoiceItems(connectionTypes, current) { dialog, which ->
                    valConnectionType.text = connectionTypes[which]
                    updateSectionVisibility(connectionTypes[which])
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Selecionar dispositivo Bluetooth
        findViewById<View>(R.id.row_bt_device).setOnClickListener {
            requestBluetoothPermissionThen { showBluetoothPicker() }
        }

        // Tipo de impressora
        findViewById<View>(R.id.row_printer_type).setOnClickListener {
            val current = printerTypeOptions.indexOf(valPrinterType.text.toString()).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle("Tipo de impressora")
                .setSingleChoiceItems(printerTypeOptions, current) { dialog, which ->
                    valPrinterType.text = printerTypeOptions[which]
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Imprimir página de teste
        findViewById<View>(R.id.row_test_print).setOnClickListener {
            saveAndTestPrint()
        }
    }

    private fun updateSectionVisibility(connectionType: String) {
        sectionBluetooth.visibility = if (connectionType == "Bluetooth") View.VISIBLE else View.GONE
        sectionWifi.visibility      = if (connectionType == "WiFi") View.VISIBLE else View.GONE
    }

    private fun showBluetoothPicker() {
        val devices = EscPosPrinter.getPairedDevices()
        if (devices.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Bluetooth")
                .setMessage("Nenhum dispositivo pareado encontrado.\n\nVá em Configurações do Android → Bluetooth e pareie a impressora antes de selecioná-la aqui.")
                .setPositiveButton("Ok", null)
                .show()
            return
        }
        val names = devices.map { d -> "${d.name ?: "Desconhecido"}\n${d.address}" }.toTypedArray()
        val currentIdx = devices.indexOfFirst { it.address == selectedBtAddress }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Selecionar impressora")
            .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                val device: BluetoothDevice = devices[which]
                selectedBtAddress = device.address
                selectedBtName    = device.name ?: device.address
                valBtDevice.text  = selectedBtName
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveAndTestPrint() {
        save()
        val opts = settings.printOptions
        val printer = EscPosPrinter()

        Toast.makeText(this, "Conectando à impressora…", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connected = printer.connect(opts)
                if (!connected) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PrintConfigActivity,
                            "Falha ao conectar na impressora", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                printer.printLines(listOf(
                    "=== ScanTE - Teste de Impressao ===",
                    "",
                    "Tipo:    ${opts.printerType}",
                    "Conexao: ${opts.connectionType}",
                    if (opts.connectionType == "Bluetooth")
                        "Disp.:   ${opts.bluetoothName}" else
                        "IP:      ${opts.wifiHost}:${opts.wifiPort}",
                    "",
                    "Se voce esta lendo isso,",
                    "a impressora esta funcionando!",
                    ""
                ), opts)
                printer.disconnect()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PrintConfigActivity,
                        "Impressão de teste enviada!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro no teste de impressão")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PrintConfigActivity,
                        "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
                printer.disconnect()
            }
        }
    }

    private fun save() {
        settings.printOptions = PrintOptions(
            printerType       = valPrinterType.text.toString(),
            timeoutSeconds    = etTimeout.text.toString().toIntOrNull() ?: 5,
            connectionType    = valConnectionType.text.toString(),
            bluetoothAddress  = selectedBtAddress,
            bluetoothName     = selectedBtName,
            wifiHost          = etWifiHost.text.toString().trim(),
            wifiPort          = etWifiPort.text.toString().toIntOrNull() ?: 9100
        )
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    // ------------------------------------------------------------------
    // Permissões Bluetooth (Android 12+)
    // ------------------------------------------------------------------

    private fun requestBluetoothPermissionThen(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val perm = Manifest.permission.BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
                action()
            } else {
                pendingBtAction = action
                ActivityCompat.requestPermissions(this, arrayOf(perm), BT_PERMISSION_CODE)
            }
        } else {
            action()
        }
    }

    private var pendingBtAction: (() -> Unit)? = null

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BT_PERMISSION_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                pendingBtAction?.invoke()
            } else {
                Toast.makeText(this,
                    "Permissão Bluetooth necessária para selecionar a impressora",
                    Toast.LENGTH_LONG).show()
            }
            pendingBtAction = null
        }
    }
}

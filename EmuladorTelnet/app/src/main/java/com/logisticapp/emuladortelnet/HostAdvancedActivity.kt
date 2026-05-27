package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class HostAdvancedActivity : AppCompatActivity() {

    private lateinit var repository: TelnetRepository
    private var hostId: Int = -1
    private var currentHost: SavedConnection? = null

    private lateinit var inputUsername: EditText
    private lateinit var inputPassword: EditText
    private lateinit var spinnerTerminal: Spinner
    private lateinit var spinnerEncoding: Spinner
    private lateinit var inputTimeout: EditText
    private lateinit var switchKeepalive: Switch
    private lateinit var switchLocalecho: Switch
    private lateinit var btnSave: Button

    private val terminalTypes = listOf("VT100", "VT220", "ANSI", "XTERM")
    private val encodings = listOf("UTF-8", "ISO-8859-1", "CP850")

    companion object {
        const val EXTRA_HOST_ID = "host_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_advanced)

        repository = TelnetRepository.getInstance(this)
        hostId = intent.getIntExtra(EXTRA_HOST_ID, -1)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        inputUsername   = findViewById(R.id.input_username)
        inputPassword   = findViewById(R.id.input_password)
        spinnerTerminal = findViewById(R.id.spinner_terminal)
        spinnerEncoding = findViewById(R.id.spinner_encoding)
        inputTimeout    = findViewById(R.id.input_timeout)
        switchKeepalive = findViewById(R.id.switch_keepalive)
        switchLocalecho = findViewById(R.id.switch_localecho)
        btnSave         = findViewById(R.id.btn_save_advanced)

        setupSpinners()

        if (hostId > 0) loadHost(hostId)

        btnSave.setOnClickListener { saveAdvanced() }
    }

    private fun setupSpinners() {
        val termAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terminalTypes)
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTerminal.adapter = termAdapter

        val encAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, encodings)
        encAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEncoding.adapter = encAdapter
    }

    private fun loadHost(id: Int) {
        lifecycleScope.launch {
            val host = repository.getConnectionById(id)
            if (host != null) {
                currentHost = host
                inputUsername.setText(host.username)
                inputPassword.setText(host.password)
                inputTimeout.setText(host.timeoutSeconds.toString())
                switchKeepalive.isChecked = host.keepAlive
                switchLocalecho.isChecked = host.localEcho

                val termIdx = terminalTypes.indexOf(host.terminalType).takeIf { it >= 0 } ?: 0
                spinnerTerminal.setSelection(termIdx)

                val encIdx = encodings.indexOf(host.encoding).takeIf { it >= 0 } ?: 0
                spinnerEncoding.setSelection(encIdx)
            }
        }
    }

    private fun saveAdvanced() {
        val base = currentHost ?: run {
            Toast.makeText(this, "Host nao encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        val timeout = inputTimeout.text.toString().toIntOrNull() ?: 30

        val updated = base.copy(
            username      = inputUsername.text.toString().trim(),
            password      = inputPassword.text.toString(),
            terminalType  = terminalTypes[spinnerTerminal.selectedItemPosition],
            encoding      = encodings[spinnerEncoding.selectedItemPosition],
            timeoutSeconds = timeout,
            keepAlive     = switchKeepalive.isChecked,
            localEcho     = switchLocalecho.isChecked
        )

        lifecycleScope.launch {
            repository.updateConnection(updated)
            Timber.d("Config avancada salva: ${updated.name}")
            Toast.makeText(this@HostAdvancedActivity, "Configuracoes salvas!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

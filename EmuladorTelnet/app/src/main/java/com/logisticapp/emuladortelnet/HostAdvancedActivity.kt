package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.settings.AppSettings
import kotlinx.coroutines.launch
import timber.log.Timber

class HostAdvancedActivity : AppCompatActivity() {

    private lateinit var repository: TelnetRepository
    private lateinit var settings: AppSettings
    private var hostId: Int = -1
    private var currentHost: SavedConnection? = null

    private lateinit var spinnerTerminal: Spinner
    private lateinit var switchKeepalive: Switch
    private lateinit var btnSave: Button

    private val terminalTypes = listOf("VT100", "VT220", "ANSI", "XTERM")

    companion object {
        const val EXTRA_HOST_ID = "host_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_advanced)

        repository = TelnetRepository.getInstance(this)
        settings = AppSettings.get(this)
        hostId = intent.getIntExtra(EXTRA_HOST_ID, -1)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        spinnerTerminal = findViewById(R.id.spinner_terminal)
        switchKeepalive = findViewById(R.id.switch_keepalive)
        btnSave         = findViewById(R.id.btn_save_advanced)

        setupSpinner()
        loadGlobalValues()

        if (hostId > 0) loadHost(hostId)

        btnSave.setOnClickListener { saveAdvanced() }
    }

    private fun setupSpinner() {
        val termAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terminalTypes)
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTerminal.adapter = termAdapter
    }

    /**
     * Tipo de Terminal e Keep-Alive são os mesmos valores da Configuração geral
     * (Configurações > Comunicação > Telnet Opções) — editar aqui, na seta da sessão,
     * ou lá, tem o mesmo efeito.
     */
    private fun loadGlobalValues() {
        val opts = settings.telnetOptions
        val termIdx = terminalTypes.indexOfFirst { it.equals(opts.terminalType, ignoreCase = true) }
            .takeIf { it >= 0 } ?: 1 // default VT220
        spinnerTerminal.setSelection(termIdx)
        switchKeepalive.isChecked = !opts.keepAliveType.equals("Desligado", ignoreCase = true)
    }

    private fun loadHost(id: Int) {
        lifecycleScope.launch {
            currentHost = repository.getConnectionById(id)
        }
    }

    private fun saveAdvanced() {
        if (currentHost == null && hostId > 0) {
            Toast.makeText(this, "Host nao encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Tipo de Terminal e Keep-Alive são compartilhados com a Configuração geral
        val opts = settings.telnetOptions
        settings.telnetOptions = opts.copy(
            terminalType = terminalTypes[spinnerTerminal.selectedItemPosition],
            keepAliveType = if (switchKeepalive.isChecked)
                opts.keepAliveType.takeIf { !it.equals("Desligado", ignoreCase = true) } ?: "TCP"
            else "Desligado"
        )

        Timber.d("Config avancada salva (terminal/keep-alive)")
        Toast.makeText(this, "Configuracoes salvas!", Toast.LENGTH_SHORT).show()
        finish()
    }
}

package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.database.AppDatabase
import com.logisticapp.emuladortelnet.database.LicenseInfo
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.databinding.ActivityMainBinding
import com.logisticapp.emuladortelnet.license.LicenseManager
import com.logisticapp.emuladortelnet.ui.LicenseDialog
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TelnetViewModel
    private lateinit var repository: TelnetRepository
    private lateinit var connectionAdapter: ArrayAdapter<String>
    private lateinit var licenseManager: LicenseManager
    private var savedConnectionsList = emptyList<SavedConnection>()
    private var isSpinnerInitializing = true  // Flag para ignorar primeira seleção
    private var currentLicenseKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Repository e ViewModel com Factory
        val database = AppDatabase.getInstance(this)
        repository = TelnetRepository.getInstance(database)
        val factory = TelnetViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(TelnetViewModel::class.java)

        // Inicializar LicenseManager
        licenseManager = LicenseManager(this)
        
        // Carregar licença do banco
        lifecycleScope.launch {
            val license = repository.getLicense()
            currentLicenseKey = license?.licenseKey
            Timber.d("Licença carregada: ${currentLicenseKey?.take(20)}")
        }

        // Definir valores padrão
        binding.portInput.setText("23")

        // Setup Spinner adapter
        connectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf("Selecionar conexão...")
        )
        connectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.savedConnectionsSpinner.adapter = connectionAdapter

        // Setup listeners
        setupListeners()

        // Observar mudanças do ViewModel
        observeViewModel()
    }

    private fun setupListeners() {
        binding.connectButton.setOnClickListener {
            val host = binding.hostInput.text.toString().trim()
            val port = binding.portInput.text.toString().trim()

            if (host.isEmpty()) {
                binding.statusText.text = "Por favor, insira um host"
                return@setOnClickListener
            }

            if (port.isEmpty()) {
                binding.statusText.text = "Por favor, insira uma porta"
                return@setOnClickListener
            }

            viewModel.connect(host, port)
        }

        binding.disconnectButton.setOnClickListener {
            viewModel.disconnect()
        }

        // Saved connections spinner
        binding.savedConnectionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                Timber.d("Spinner: onItemSelected position=$position, isInitializing=$isSpinnerInitializing, listSize=${savedConnectionsList.size}")
                
                // Ignorar primeira seleção automática do Android
                if (isSpinnerInitializing) {
                    isSpinnerInitializing = false
                    return
                }
                
                if (position > 0 && position <= savedConnectionsList.size) {
                    val connection = savedConnectionsList[position - 1]
                    Timber.d("Selecionado: ${connection.name} - ${connection.host}:${connection.port}")
                    binding.hostInput.setText(connection.host)
                    binding.portInput.setText(connection.port.toString())
                    binding.statusText.text = "Selecionado: ${connection.name}"
                } else if (position == 0) {
                    Timber.d("Selecionado: placeholder (nenhuma ação)")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Save connection button
        binding.saveConnectionButton.setOnClickListener {
            val host = binding.hostInput.text.toString().trim()
            val portStr = binding.portInput.text.toString().trim()
            
            if (host.isEmpty()) {
                binding.statusText.text = "Por favor, insira um host para salvar"
                return@setOnClickListener
            }
            
            if (portStr.isEmpty()) {
                binding.statusText.text = "Por favor, insira uma porta"
                return@setOnClickListener
            }
            
            val port = portStr.toIntOrNull() ?: 23
            
            // Dialog para pedir nome da conexão
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Salvar Conexão")
                .setMessage("Nome da conexão:")
                .setView(android.widget.EditText(this).apply {
                    id = android.R.id.edit
                    hint = "ex: Servidor Prod"
                })
                .setPositiveButton("Salvar") { dialog, _ ->
                    val nameInput = (dialog as android.app.AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                    val name = nameInput?.text.toString().trim()
                    if (name.isNotEmpty()) {
                        viewModel.saveCurrentConnection(name, host, port)
                        binding.statusText.text = "Conexão '$name' ($host:$port) salva!"
                        Timber.d("Salvando: $name - $host:$port")
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
            dialog.show()
        }

        binding.terminalOutput.setOnClickListener {
            // Scroll para baixo quando clicar no terminal
            binding.scrollView.post {
                binding.scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
            }
        }

        // Send button
        binding.sendButton.setOnClickListener {
            val command = binding.commandInput.text.toString()
            if (command.isNotEmpty()) {
                viewModel.sendCommand(command)
                binding.commandInput.text.clear()
                viewModel.resetInputHistory()
                hideKeyboard()  // Esconder teclado após enviar
            }
        }

        // Command input - Enter key
        binding.commandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)) {
                
                val command = binding.commandInput.text.toString()
                if (command.isNotEmpty()) {
                    viewModel.sendCommand(command)
                    binding.commandInput.text.clear()
                    viewModel.resetInputHistory()
                    hideKeyboard()  // Esconder teclado após enviar
                }
                return@setOnEditorActionListener true
            }
            false
        }

        // Command input - Arrow keys for history
        binding.commandInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        val prevCommand = viewModel.getPreviousCommand()
                        if (prevCommand != null) {
                            binding.commandInput.setText(prevCommand)
                            binding.commandInput.setSelection(prevCommand.length)
                        }
                        return@setOnKeyListener true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val nextCommand = viewModel.getNextCommand()
                        binding.commandInput.setText(nextCommand ?: "")
                        if (nextCommand != null) {
                            binding.commandInput.setSelection(nextCommand.length)
                        }
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun observeViewModel() {
        // Observar conexões salvas
        lifecycleScope.launch {
            viewModel.savedConnections.collect { connections ->
                savedConnectionsList = connections
                val names = mutableListOf("Selecionar conexão...")
                names.addAll(connections.map { "${it.name} (${it.host}:${it.port})" })
                
                // Reset flag antes de atualizar adapter (para ignorar seleção automática)
                isSpinnerInitializing = true
                
                connectionAdapter.clear()
                connectionAdapter.addAll(names)
                connectionAdapter.notifyDataSetChanged()
                Timber.d("Conexões salvas atualizadas: ${connections.size}")
            }
        }

        // Observar estado de conexão
        viewModel.connectionState.observe(this) { state ->
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    binding.statusText.text = getString(R.string.status_disconnected)
                    binding.statusText.setTextColor(getColor(android.R.color.darker_gray))
                    binding.connectButton.isEnabled = true
                    binding.disconnectButton.isEnabled = false
                    binding.hostInput.isEnabled = true
                    binding.portInput.isEnabled = true
                    binding.commandInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                    showKeyboard(binding.hostInput)  // Mostrar teclado quando desconecta
                }

                ConnectionState.CONNECTING -> {
                    binding.statusText.text = getString(R.string.status_connecting)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                    binding.connectButton.isEnabled = false
                    binding.disconnectButton.isEnabled = false
                    binding.hostInput.isEnabled = false
                    binding.portInput.isEnabled = false
                    binding.commandInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                }

                ConnectionState.CONNECTED -> {
                    binding.statusText.text = getString(R.string.status_connected)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    binding.connectButton.isEnabled = false
                    binding.disconnectButton.isEnabled = true
                    binding.hostInput.isEnabled = false
                    binding.portInput.isEnabled = false
                    binding.commandInput.isEnabled = true
                    binding.sendButton.isEnabled = true
                    hideKeyboard()  // Esconder teclado quando conecta
                    binding.commandInput.requestFocus()  // Focar no input de comando
                }

                ConnectionState.ERROR -> {
                    binding.statusText.text = getString(R.string.status_error)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                    binding.connectButton.isEnabled = true
                    binding.disconnectButton.isEnabled = false
                    binding.hostInput.isEnabled = true
                    binding.portInput.isEnabled = true
                    binding.commandInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                }
            }
        }

        // Observar saída do terminal (com estilos ANSI)
        viewModel.terminalOutputStyled.observe(this) { htmlOutput ->
            try {
                binding.terminalOutput.text = Html.fromHtml(htmlOutput, Html.FROM_HTML_MODE_LEGACY)
            } catch (e: Exception) {
                // Fallback para plain text
                binding.terminalOutput.text = htmlOutput
            }
            // Auto-scroll para baixo
            binding.scrollView.post {
                binding.scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
            }
        }
    }

    /**
     * Criar menu options
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(Menu.NONE, 1, Menu.NONE, "📜 Licença")
        return true
    }

    /**
     * Tratar cliques no menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                showLicenseDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Mostrar diálogo de licença
     */
    private fun showLicenseDialog() {
        val dialog = LicenseDialog(this)
        dialog.show(currentLicenseKey, 
            onGenerateTrial = { generateTrialLicense() },
            onGeneratePremium = { generatePremiumLicense() }
        )
    }

    /**
     * Gerar licença de teste (30 dias)
     */
    private fun generateTrialLicense() {
        val trialKey = licenseManager.generateTrialLicense()
        currentLicenseKey = trialKey
        
        val parts = trialKey.split("-")
        val expiryTimestamp = parts.getOrNull(2)?.toLong() ?: 0L
        
        val licenseInfo = LicenseInfo(
            licenseKey = trialKey,
            licenseType = "TRIAL",
            deviceFingerprint = licenseManager.generateDeviceFingerprint(),
            expiryTimestamp = expiryTimestamp,
            isValid = true
        )
        
        lifecycleScope.launch {
            repository.saveLicense(licenseInfo)
            binding.statusText.text = "✅ Licença TRIAL ativada (30 dias)"
            Timber.d("Licença TRIAL gerada: ${trialKey.take(20)}")
        }
    }

    /**
     * Gerar licença premium (1 ano)
     */
    private fun generatePremiumLicense() {
        val premiumKey = licenseManager.generatePremiumLicense()
        currentLicenseKey = premiumKey
        
        val parts = premiumKey.split("-")
        val expiryTimestamp = parts.getOrNull(2)?.toLong() ?: 0L
        
        val licenseInfo = LicenseInfo(
            licenseKey = premiumKey,
            licenseType = "PREMIUM",
            deviceFingerprint = licenseManager.generateDeviceFingerprint(),
            expiryTimestamp = expiryTimestamp,
            isValid = true
        )
        
        lifecycleScope.launch {
            repository.saveLicense(licenseInfo)
            binding.statusText.text = "✅ Licença PREMIUM ativada (1 ano)"
            Timber.d("Licença PREMIUM gerada: ${premiumKey.take(20)}")
        }
    }

    /**
     * Esconder teclado virtual
     */
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    /**
     * Mostrar teclado virtual
     */
    private fun showKeyboard(view: android.view.View) {
        view.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

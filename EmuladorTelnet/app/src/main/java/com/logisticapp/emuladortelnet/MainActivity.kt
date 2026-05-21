package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.database.AppDatabase
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.databinding.ActivityMainBinding
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TelnetViewModel
    private lateinit var repository: TelnetRepository

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

        // Definir valores padrão
        binding.portInput.setText("23")

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
}

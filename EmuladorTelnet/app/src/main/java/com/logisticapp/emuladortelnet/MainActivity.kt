package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.databinding.ActivityMainBinding
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TelnetViewModel
    private lateinit var repository: TelnetRepository
    private var hasConnected = false

    companion object {
        const val EXTRA_HOST    = "extra_host"
        const val EXTRA_PORT    = "extra_port"
        const val EXTRA_NAME    = "extra_name"
        const val EXTRA_HOST_ID = "extra_host_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TelnetRepository.getInstance(this)
        val factory = TelnetViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(TelnetViewModel::class.java)

        setupListeners()
        observeViewModel()

        val host   = intent.getStringExtra(EXTRA_HOST) ?: ""
        val port   = intent.getIntExtra(EXTRA_PORT, 23)
        val name   = intent.getStringExtra(EXTRA_NAME) ?: host
        val hostId = intent.getIntExtra(EXTRA_HOST_ID, -1)

        // Carregar teclas configuráveis antes de conectar
        if (hostId > 0) {
            lifecycleScope.launch {
                val conn = repository.getConnectionById(hostId)
                if (conn != null) configureCustomKeys(conn)
            }
        }

        if (host.isNotEmpty()) {
            binding.statusText.text = name
            viewModel.connect(host, port.toString())
            Timber.d("Auto-conectando: $name -> $host:$port")
        }
    }

    private fun configureCustomKeys(conn: SavedConnection) {
        binding.keyX.text = conn.customKey1Label.ifBlank { "F1" }
        binding.keyY.text = conn.customKey2Label.ifBlank { "F2" }
        binding.keyZ.text = conn.customKey3Label.ifBlank { "F3" }

        binding.keyX.setOnClickListener {
            val value = conn.customKey1Value
            if (value.isNotEmpty()) viewModel.sendCommand(value)
        }
        binding.keyY.setOnClickListener {
            val value = conn.customKey2Value
            if (value.isNotEmpty()) viewModel.sendCommand(value)
        }
        binding.keyZ.setOnClickListener {
            val value = conn.customKey3Value
            if (value.isNotEmpty()) viewModel.sendCommand(value)
        }
    }

    private fun setupListeners() {
        binding.disconnectButton.setOnClickListener {
            viewModel.disconnect()
        }

        binding.sendButton.setOnClickListener {
            val command = binding.commandInput.text.toString()
            if (command.isNotEmpty()) {
                viewModel.sendCommand(command)
                binding.commandInput.text.clear()
                viewModel.resetInputHistory()
                hideKeyboard()
            }
        }

        binding.commandInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                        event.action == android.view.KeyEvent.ACTION_DOWN)) {
                val command = binding.commandInput.text.toString()
                if (command.isNotEmpty()) {
                    viewModel.sendCommand(command)
                    binding.commandInput.text.clear()
                    viewModel.resetInputHistory()
                    hideKeyboard()
                }
                return@setOnEditorActionListener true
            }
            false
        }

        binding.commandInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        val prev = viewModel.getPreviousCommand()
                        if (prev != null) {
                            binding.commandInput.setText(prev)
                            binding.commandInput.setSelection(prev.length)
                        }
                        return@setOnKeyListener true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val next = viewModel.getNextCommand()
                        binding.commandInput.setText(next ?: "")
                        if (next != null) binding.commandInput.setSelection(next.length)
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }

        // Teclas fixas
        binding.keyEnter.setOnClickListener { viewModel.sendCommand("\r\n") }
        binding.keyEsc.setOnClickListener   { viewModel.sendCommand("") }

        // Teclas X/Y/Z são configuradas em configureCustomKeys() — valores default abaixo
        binding.keyX.setOnClickListener { viewModel.sendCommand("") }
        binding.keyY.setOnClickListener { viewModel.sendCommand("") }
        binding.keyZ.setOnClickListener { viewModel.sendCommand("") }

        binding.terminalOutput.setOnClickListener {
            binding.scrollView.post {
                binding.scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(this) { state ->
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    binding.statusText.text = getString(R.string.status_disconnected)
                    binding.statusText.setTextColor(getColor(android.R.color.darker_gray))
                    binding.disconnectButton.isEnabled = false
                    binding.commandInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                    binding.controlKeysBar.visibility = android.view.View.GONE
                    if (hasConnected) finish()
                }
                ConnectionState.CONNECTING -> {
                    binding.statusText.text = getString(R.string.status_connecting)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                    binding.disconnectButton.isEnabled = false
                    binding.commandInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                    binding.controlKeysBar.visibility = android.view.View.GONE
                }
                ConnectionState.CONNECTED -> {
                    hasConnected = true
                    binding.statusText.text = getString(R.string.status_connected)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    binding.disconnectButton.isEnabled = true
                    binding.commandInput.isEnabled = true
                    binding.sendButton.isEnabled = true
                    binding.controlKeysBar.visibility = android.view.View.VISIBLE
                    binding.keyEnter.isEnabled = true
                    binding.keyEsc.isEnabled = true
                    binding.keyX.isEnabled = true
                    binding.keyY.isEnabled = true
                    binding.keyZ.isEnabled = true
                    hideKeyboard()
                    binding.commandInput.requestFocus()
                }
                ConnectionState.ERROR -> {
                    binding.statusText.text = getString(R.string.status_error)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                    binding.disconnectButton.isEnabled = false
                    binding.commandInput.isEnabled = false
                    binding.sendButton.isEnabled = false
                    binding.controlKeysBar.visibility = android.view.View.GONE
                }
            }
        }

        viewModel.terminalOutputStyled.observe(this) { htmlOutput ->
            try {
                binding.terminalOutput.text = Html.fromHtml(htmlOutput, Html.FROM_HTML_MODE_LEGACY)
            } catch (e: Exception) {
                binding.terminalOutput.text = htmlOutput
            }
            binding.scrollView.post {
                binding.scrollView.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> { sair(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sair() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Deseja sair da aplicacao?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.disconnect()
                val intent = Intent(this, HostsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}

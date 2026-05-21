package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.ui.TelnetViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: TelnetViewModel
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var terminalOutput: TextView
    private lateinit var statusText: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar ViewModel
        viewModel = ViewModelProvider(this).get(TelnetViewModel::class.java)

        // Encontrar views
        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        terminalOutput = findViewById(R.id.terminalOutput)
        statusText = findViewById(R.id.statusText)
        scrollView = findViewById(R.id.scrollView)

        // Definir valores padrão
        portInput.setText("23")

        // Setup listeners
        setupListeners()

        // Observar mudanças do ViewModel
        observeViewModel()
    }

    private fun setupListeners() {
        connectButton.setOnClickListener {
            val host = hostInput.text.toString().trim()
            val port = portInput.text.toString().trim()

            if (host.isEmpty()) {
                statusText.text = "Por favor, insira um host"
                return@setOnClickListener
            }

            if (port.isEmpty()) {
                statusText.text = "Por favor, insira uma porta"
                return@setOnClickListener
            }

            viewModel.connect(host, port)
        }

        disconnectButton.setOnClickListener {
            viewModel.disconnect()
        }

        terminalOutput.setOnClickListener {
            // Scroll para baixo quando clicar no terminal
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun observeViewModel() {
        // Observar estado de conexão
        viewModel.connectionState.observe(this) { state ->
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    statusText.text = getString(R.string.status_disconnected)
                    statusText.setTextColor(getColor(android.R.color.darker_gray))
                    connectButton.isEnabled = true
                    disconnectButton.isEnabled = false
                    hostInput.isEnabled = true
                    portInput.isEnabled = true
                }

                ConnectionState.CONNECTING -> {
                    statusText.text = getString(R.string.status_connecting)
                    statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                    connectButton.isEnabled = false
                    disconnectButton.isEnabled = false
                    hostInput.isEnabled = false
                    portInput.isEnabled = false
                }

                ConnectionState.CONNECTED -> {
                    statusText.text = getString(R.string.status_connected)
                    statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    connectButton.isEnabled = false
                    disconnectButton.isEnabled = true
                    hostInput.isEnabled = false
                    portInput.isEnabled = false
                }

                ConnectionState.ERROR -> {
                    statusText.text = getString(R.string.status_error)
                    statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                    connectButton.isEnabled = true
                    disconnectButton.isEnabled = false
                    hostInput.isEnabled = true
                    portInput.isEnabled = true
                }
            }
        }

        // Observar saída do terminal
        viewModel.terminalOutput.observe(this) { output ->
            terminalOutput.text = output
            // Auto-scroll para baixo
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}

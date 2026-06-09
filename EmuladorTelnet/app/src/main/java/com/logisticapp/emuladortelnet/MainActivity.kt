package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.databinding.ActivityMainBinding
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TelnetViewModel
    private lateinit var repository: TelnetRepository
    private lateinit var settings: AppSettings
    private var hasConnected = false
    private var manualDisconnect = false

    // Dados da conexao atual (para reconexao automatica)
    private var currentHost = ""
    private var currentPort = 23
    private var currentName = ""

    // Receiver para detectar bloqueio de tela
    private var screenOffReceiver: BroadcastReceiver? = null

    companion object {
        const val EXTRA_HOST    = "extra_host"
        const val EXTRA_PORT    = "extra_port"
        const val EXTRA_NAME    = "extra_name"
        const val EXTRA_HOST_ID = "extra_host_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nunca bloquear a tela quando conectado
        if (settings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Tamanho da fonte do terminal (Opcoes de tela)
        binding.terminalOutput.textSize = settings.fontSize.toFloat()

        // Cores da tela
        binding.terminalOutput.setBackgroundColor(settings.colorBackground)
        binding.scrollView.setBackgroundColor(settings.colorBackground)
        binding.statusText.setTextColor(settings.colorStatusForeground)
        if (settings.colorStatusBackground != 0) {
            binding.appBar.setBackgroundColor(settings.colorStatusBackground)
        }

        repository = TelnetRepository.getInstance(this)
        val factory = TelnetViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(TelnetViewModel::class.java)
        viewModel.setForegroundColor(settings.colorForeground)
        viewModel.setFieldColor(settings.colorInputField)

        setupListeners()
        observeViewModel()
        registerScreenOffReceiver()

        currentHost = intent.getStringExtra(EXTRA_HOST) ?: ""
        currentPort = intent.getIntExtra(EXTRA_PORT, 23)
        currentName = intent.getStringExtra(EXTRA_NAME) ?: currentHost

        if (currentHost.isNotEmpty()) {
            binding.statusText.text = currentName
            viewModel.connect(currentHost, currentPort.toString())
            Timber.d("Auto-conectando: $currentName -> $currentHost:$currentPort")
        }
    }

    private fun registerScreenOffReceiver() {
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF && settings.disconnectOnLock) {
                    Timber.d("Tela bloqueada -> desconectando")
                    manualDisconnect = true
                    viewModel.disconnect()
                }
            }
        }
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        super.onDestroy()
        screenOffReceiver?.let { runCatching { unregisterReceiver(it) } }
    }

    private fun setupListeners() {
        binding.disconnectButton.setOnClickListener {
            manualDisconnect = true
            viewModel.disconnect()
        }

        // Digitacao vai direto ao servidor (via TerminalView), como num emulador real
        binding.terminalOutput.onInput = { bytes -> viewModel.sendRaw(bytes) }

        // Botao de mostrar/ocultar teclado (ao lado de Desconectar)
        binding.keyboardToggle.setOnClickListener { toggleKeyboard() }

        // Tocar no terminal abre o teclado e rola para o fim
        binding.terminalOutput.setOnClickListener {
            openKeyboard()
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
                    binding.controlKeysBar.visibility = android.view.View.GONE
                    binding.keyboardToggle.visibility = android.view.View.GONE
                    if (hasConnected) {
                        if (!manualDisconnect && settings.autoReconnect && currentHost.isNotEmpty()) {
                            // Reconexao automatica apos conexao perdida
                            Timber.d("Conexao perdida -> reconectando automaticamente")
                            viewModel.connect(currentHost, currentPort.toString())
                        } else {
                            finish()
                        }
                    }
                }
                ConnectionState.CONNECTING -> {
                    binding.statusText.text = getString(R.string.status_connecting)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                    binding.disconnectButton.isEnabled = false
                    binding.controlKeysBar.visibility = android.view.View.GONE
                }
                ConnectionState.CONNECTED -> {
                    hasConnected = true
                    manualDisconnect = false
                    binding.statusText.text = getString(R.string.status_connected)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    binding.disconnectButton.isEnabled = true
                    binding.controlKeysBar.visibility = android.view.View.VISIBLE
                    binding.keyboardToggle.visibility = android.view.View.VISIBLE
                    buildToolbars()
                    // Teclado habilitado: abre automaticamente ao conectar
                    if (settings.keyboardEnabled) {
                        openKeyboard()
                    }
                }
                ConnectionState.ERROR -> {
                    binding.statusText.text = getString(R.string.status_error)
                    binding.statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                    binding.disconnectButton.isEnabled = false
                    binding.controlKeysBar.visibility = android.view.View.GONE
                }
            }
        }

        viewModel.terminalOutputStyled.observe(this) { styled ->
            binding.terminalOutput.text = styled
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
                manualDisconnect = true
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

    /** Abre o teclado virtual focando o terminal (a digitacao vai direto ao servidor). */
    private fun openKeyboard() {
        binding.terminalOutput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.terminalOutput, InputMethodManager.SHOW_IMPLICIT)
    }

    /** Mostra/oculta o teclado virtual. */
    private fun toggleKeyboard() {
        binding.terminalOutput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    /** Gera as 4 barras de ferramentas (linhas de botoes) a partir das configuracoes. */
    private fun buildToolbars() {
        binding.controlKeysBar.removeAllViews()
        val bars = settings.toolbars
        for (bar in bars) {
            if (bar.isEmpty()) continue
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            for (btn in bar) {
                val b = android.widget.Button(this).apply {
                    text = btn.label
                    isAllCaps = false
                    textSize = 11f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(0xFF2E5C6E.toInt())
                    setPadding(0, 0, 0, 0)
                    minWidth = 0
                    minimumWidth = 0
                    val lp = android.widget.LinearLayout.LayoutParams(
                        0, (36 * resources.displayMetrics.density).toInt(), 1f)
                    val m = (2 * resources.displayMetrics.density).toInt()
                    lp.setMargins(m, m, m, m)
                    layoutParams = lp
                }
                b.setOnClickListener { sendAction(btn.action) }
                row.addView(b)
            }
            binding.controlKeysBar.addView(row)
        }
    }

    /** Executa a acao de um botao de barra de ferramentas. */
    private fun sendAction(action: String) {
        val bytes = com.logisticapp.emuladortelnet.toolbar.ToolbarCatalog.bytesFor(action)
        if (bytes != null) {
            viewModel.sendRaw(bytes)
            return
        }
        // Acoes especiais (tratadas no app)
        when (action) {
            "DISCONNECT" -> { manualDisconnect = true; viewModel.disconnect() }
            "CONNECT" -> {
                if (currentHost.isNotEmpty()) viewModel.connect(currentHost, currentPort.toString())
            }
            "BREAK" -> viewModel.sendRaw(byteArrayOf(255.toByte(), 243.toByte())) // IAC BRK
            "COPY", "PASTE" -> android.widget.Toast.makeText(
                this, "$action: em breve", android.widget.Toast.LENGTH_SHORT).show()
            else -> Timber.d("Acao nao tratada: $action")
        }
    }
}

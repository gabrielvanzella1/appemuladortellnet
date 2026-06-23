package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        // Garante que o controlKeysBar fique colado acima do teclado (Android 11+)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // Padding bottom = altura do teclado (quando aberto) ou barra de navegação
            view.setPadding(0, 0, 0, maxOf(imeInsets.bottom, navInsets.bottom))
            insets
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
        viewModel.setTerminalType(settings.telnetOptions.terminalType)
        viewModel.setBinaryMode(settings.telnetOptions.binaryMode)
        viewModel.setSimulateParity(settings.telnetOptions.simulateParity)
        viewModel.setKeepAlive(
            settings.telnetOptions.keepAliveType,
            settings.telnetOptions.keepAliveInterval.toIntOrNull() ?: 0
        )
        viewModel.setAutoLogin(
            settings.telnetOptions.waitLoginPrompt,
            settings.telnetOptions.loginWith,
            settings.telnetOptions.waitPasswordPrompt,
            settings.telnetOptions.password,
            settings.telnetOptions.waitCommandPrompt,
            settings.telnetOptions.doCommand
        )
        // SSL: carrega bytes do certificado cliente, se houver
        val sslOpts = settings.telnetOptions
        val certBytes: ByteArray? = if (sslOpts.useSsl && sslOpts.clientCertFile.isNotBlank()) {
            try { java.io.File(sslOpts.clientCertFile).readBytes() } catch (e: Exception) { null }
        } else null
        viewModel.setSsl(sslOpts.useSsl, certBytes, sslOpts.clientCertPassword)

        // SSH: carrega chave privada, se houver
        val keyBytes: ByteArray? = if (sslOpts.useSsh && sslOpts.sshPrivateKey.isNotBlank()) {
            try { java.io.File(sslOpts.sshPrivateKey).readBytes() } catch (e: Exception) { null }
        } else null
        val sshPortInt = currentPort  // usa a porta da sessão, ou pode vir de sshServer separado
        val sshHostStr = sslOpts.sshServer.ifBlank { currentHost }
        val sshPortParsed = sslOpts.sshServer.substringAfter(":", "22").toIntOrNull() ?: sshPortInt
        val sshHostParsed = sslOpts.sshServer.substringBefore(":").ifBlank { sshHostStr }
        viewModel.setSshConfig(
            sslOpts.useSsh,
            sshHostParsed,
            sshPortParsed,
            sslOpts.sshUsername,
            sslOpts.sshPassword,
            keyBytes,
            sslOpts.sshKeepAlive.toIntOrNull() ?: 0
        )

        // Proxy HTTP CONNECT
        val proxyOpts = settings.proxyOptions
        val proxyPortInt = proxyOpts.port.toIntOrNull() ?: 30855
        val proxyHostStr = proxyOpts.address.substringBefore(":")
        val proxyPortParsed = proxyOpts.address.substringAfter(":", "").toIntOrNull() ?: proxyPortInt
        viewModel.setProxy(proxyOpts.useServer, proxyHostStr, proxyPortParsed, proxyOpts.secureComm)

        // Terminador de linha (Enter): aplica ao teclado
        binding.terminalOutput.lineTerminator = lineTerminatorBytes()

        // VT Opções (ECHO, Modo ROLO, Backspace, alarme, etc.)
        val vtOpts = settings.vtOptions
        viewModel.setVtOptions(vtOpts)
        binding.terminalOutput.backspaceAsDel = vtOpts.backspaceAction == "DEL"
        binding.terminalOutput.f5PuttySequence = vtOpts.f5PuttySequence

        // Transliteração (charset, lowercase, SISO, etc.)
        viewModel.setTransliterationOptions(settings.transliterationOptions)

        // Opções gerais de emulação (BS destrutivo, captura CR, tamanho da tela)
        viewModel.setGeneralEmulationOptions(settings.generalEmulationOptions)

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

        // Alarme do host (BEL): toca som + vibra quando não silenciado
        viewModel.bellEvent.observe(this) { ev ->
            if (ev != null) {
                playBell()
                viewModel.onBellHandled()
            }
        }
    }

    /** Toca o som de notificação e vibra (alarme do host / BEL). */
    private fun playBell() {
        try {
            val tone = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 100
            )
            tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
            binding.terminalOutput.postDelayed({ tone.release() }, 300)
        } catch (e: Exception) {
            Timber.w(e, "Falha ao tocar alarme")
        }
        try {
            val vib = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vib?.vibrate(android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vib?.vibrate(120)
            }
        } catch (e: Exception) {
            Timber.w(e, "Falha ao vibrar")
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
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    /** Gera as barras de ferramentas com scroll horizontal quando há muitos botões. */
    private fun buildToolbars() {
        binding.controlKeysBar.removeAllViews()
        val bars = settings.toolbars
        val density = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels
        val btnH = (36 * density).toInt()
        val margin = (2 * density).toInt()
        val minBtnW = (72 * density).toInt()

        for (bar in bars) {
            if (bar.isEmpty()) continue

            // Largura por botão: distribui igualmente se couber, senão usa mínimo e rola
            val btnW = if (bar.size * minBtnW <= screenW) screenW / bar.size else minBtnW

            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
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
                    val lp = android.widget.LinearLayout.LayoutParams(btnW, btnH)
                    lp.setMargins(margin, margin, margin, margin)
                    layoutParams = lp
                }
                b.setOnClickListener { sendAction(btn.action) }
                row.addView(b)
            }

            val scroll = android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            scroll.addView(row)
            binding.controlKeysBar.addView(scroll)
        }
    }

    /** Bytes do terminador de linha configurado (Telnet Opcoes). */
    private fun lineTerminatorBytes(): ByteArray = when (settings.telnetOptions.lineTerminator) {
        "CR+LF" -> byteArrayOf(13, 10)
        "LF"    -> byteArrayOf(10)
        else    -> byteArrayOf(13)   // CR
    }

    /** Executa a acao de um botao de barra de ferramentas. */
    private fun sendAction(action: String) {
        // Enter respeita o terminador de linha configurado
        if (action == "ENTER") {
            viewModel.sendRaw(lineTerminatorBytes())
            return
        }
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
            "BREAK" -> {
                // "Usar IP para BRK": envia Interrupt Process (244) em vez de Break (243)
                val cmd = if (settings.telnetOptions.useIpForBrk) 244 else 243
                viewModel.sendRaw(byteArrayOf(255.toByte(), cmd.toByte()))
            }
            "COPY", "PASTE" -> android.widget.Toast.makeText(
                this, "$action: em breve", android.widget.Toast.LENGTH_SHORT).show()
            else -> Timber.d("Acao nao tratada: $action")
        }
    }
}

package com.logisticapp.emuladortelnet.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.data.TelnetConnection
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.network.TelnetClient
import com.logisticapp.emuladortelnet.settings.GeneralEmulationOptions
import com.logisticapp.emuladortelnet.settings.TransliterationOptions
import com.logisticapp.emuladortelnet.settings.VtAttrMapOptions
import com.logisticapp.emuladortelnet.settings.VtOptions
import com.logisticapp.emuladortelnet.terminal.TerminalEmulator
import com.logisticapp.emuladortelnet.terminal.InputHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Estados da máquina de auto-login Telnet.
 */
private enum class TelnetAutoLoginState {
    IDLE, WAITING_LOGIN, SENDING_LOGIN, WAITING_PASSWORD, SENDING_PASSWORD,
    WAITING_COMMAND, SENDING_COMMAND, COMPLETE
}

/**
 * ViewModel para gerenciar estado da conexão Telnet
 */
class TelnetViewModel(private val repository: TelnetRepository) : ViewModel() {

    // Cliente Telnet
    private val telnetClient = TelnetClient()

    // Emulador de tela VT100 (dimensões configuráveis via setGeneralEmulationOptions)
    private var emulator = TerminalEmulator()

    /** Define a cor padrao do texto do terminal (Cores da tela > Primeiro plano). */
    fun setForegroundColor(color: Int) {
        emulator.setDefaultForeground(color)
    }

    /** Define a cor de fundo dos campos de preenchimento (0 = reverso natural). */
    fun setFieldColor(color: Int) {
        emulator.setFieldColor(color)
    }

    /** Define o tipo de terminal informado ao servidor (Telnet Opcoes). */
    fun setTerminalType(type: String) {
        telnetClient.setTerminalType(type)
    }

    /** Define tipo e cor do cursor (Opções de tela). */
    fun setCursorSettings(type: String, color: Int) {
        emulator.cursorDisplayType  = type
        emulator.cursorDisplayColor = color
    }

    /** Define o modo de exibição dos campos variáveis 3D (Opções de tela). */
    fun setFields3dMode(mode: String) {
        emulator.fields3dMode = mode
    }

    /** Define as cores de ajuste (dim/bright/fundo) para o emulador (Ajuste de cor). */
    fun setColorAdjust(dimColor: Int, brightColor: Int, bgAdjust: Int) {
        emulator.dimFgColor    = dimColor
        emulator.brightFgColor = brightColor
        emulator.bgAdjustColor = bgAdjust
    }

    /** Aplica o mapeamento de atributos VT ao emulador (VT Mapeamento de atributos). */
    fun setVtAttrMap(opts: VtAttrMapOptions) {
        emulator.boldMode         = opts.boldMode
        emulator.underlineEnabled = opts.underlineEnabled
        emulator.blinkMode        = opts.blinkMode
    }

    /** Alterna visibilidade do cursor para efeito de piscada. Chamar no main thread. */
    fun toggleCursor() {
        emulator.showCursor = !emulator.showCursor
        _terminalOutputStyled.value = emulator.renderSpannable()
    }

    // ---- VT Options ----
    private var localEcho = false
    private var lineMode = "Desativado"

    // ---- Transliteration Options ----
    private var allowLowercase = true
    private var host8bit = true
    private var nationalTranslitMode = "Padrão (sem transliteração)"

    // Mapa de transliteração para línguas latinas (PT, FR, DE, ES, IT)
    private val TRANSLIT_LATIN = mapOf(
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
        'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
        'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c', 'ñ' to 'n', 'ý' to 'y',
        'Á' to 'A', 'À' to 'A', 'Â' to 'A', 'Ã' to 'A', 'Ä' to 'A',
        'É' to 'E', 'È' to 'E', 'Ê' to 'E', 'Ë' to 'E',
        'Í' to 'I', 'Ì' to 'I', 'Î' to 'I', 'Ï' to 'I',
        'Ó' to 'O', 'Ò' to 'O', 'Ô' to 'O', 'Õ' to 'O', 'Ö' to 'O',
        'Ú' to 'U', 'Ù' to 'U', 'Û' to 'U', 'Ü' to 'U',
        'Ç' to 'C', 'Ñ' to 'N', 'Ý' to 'Y'
    )
    // Adiciona caracteres nórdicos ao mapa base
    private val TRANSLIT_NORDIC = TRANSLIT_LATIN + mapOf(
        'å' to 'a', 'æ' to 'a', 'ø' to 'o',
        'Å' to 'A', 'Æ' to 'A', 'Ø' to 'O'
    )

    private fun getTranslitMap(): Map<Char, Char> = when (nationalTranslitMode) {
        "Português (Brasil)", "Espanhol", "Francês", "Alemão", "Italiano" -> TRANSLIT_LATIN
        "Sueco / Finlandês", "Norueguês / Dinamarquês" -> TRANSLIT_NORDIC
        else -> emptyMap()
    }

    private fun charsetNameFromLabel(label: String): String = when {
        label.contains("8859-2") -> "ISO-8859-2"
        label.contains("8859-5") -> "ISO-8859-5"
        label.contains("8859-7") -> "ISO-8859-7"
        label.contains("8859-9") -> "ISO-8859-9"
        label.contains("8859-15") -> "ISO-8859-15"
        label.contains("1252") -> "windows-1252"
        label.contains("1251") -> "windows-1251"
        label.contains("850") -> "IBM850"
        label.contains("437") -> "IBM437"
        else -> "ISO-8859-1"
    }

    /**
     * Aplica as configurações de transliteração ao cliente Telnet e ao emulador.
     * Deve ser chamado em MainActivity.onCreate() após ler settings.transliterationOptions.
     */
    fun setTransliterationOptions(opts: TransliterationOptions) {
        allowLowercase = opts.allowLowercase
        host8bit = opts.host8bit
        nationalTranslitMode = opts.nationalTranslit

        val charsetName = if (opts.utf8Encoding) "UTF-8" else charsetNameFromLabel(opts.hostCharset)
        telnetClient.setCharset(charsetName)
        emulator.useSiso = opts.useSiso
    }

    // Evento de BEL: a Activity observa e toca o som
    private val _bellEvent = MutableLiveData<Unit?>(null)
    val bellEvent: LiveData<Unit?> = _bellEvent
    fun onBellHandled() { _bellEvent.value = null }

    /**
     * Aplica todas as VT Opções ao emulador e ao cliente Telnet.
     * Deve ser chamado em MainActivity.onCreate() após ler settings.vtOptions.
     */
    fun setVtOptions(opts: VtOptions) {
        lineMode  = opts.lineMode
        // Modo de linha: Local força eco local; Remoto suprime; Desativado segue ECHO mode
        localEcho = when (opts.lineMode) {
            "Local"  -> true
            "Remoto" -> false
            else     -> opts.echoMode
        }

        emulator.addLfToCr           = opts.addLfToCr
        emulator.noAutoWrap          = opts.noColumn81
        emulator.scrollMode          = opts.scrollMode
        emulator.silenceHostAlarm    = opts.silenceHostAlarm
        emulator.maxConsecutiveAlarms = when (opts.maxConsecutiveAlarms) {
            "Max" -> Int.MAX_VALUE
            else  -> opts.maxConsecutiveAlarms.toIntOrNull() ?: Int.MAX_VALUE
        }
        emulator.ignoreUnknownEscapes = opts.ignoreUnknownEscapes
        emulator.answerbackString     = opts.answerbackString
        emulator.vtDaAlias            = opts.vtDaAlias

        // Resposta ao ENQ (0x05) do servidor
        emulator.onEnq = { str ->
            if (str.isNotEmpty()) viewModelScope.launch(Dispatchers.IO) {
                telnetClient.sendRawBytes(str.toByteArray(Charsets.ISO_8859_1))
            }
        }

        // Resposta ao DA query (ESC[c)
        emulator.onDeviceAttrQuery = { alias ->
            val response = when (alias.uppercase()) {
                "VT52"  -> "/Z"
                "VT220" -> "[?62;1;6c"
                "VT320" -> "[?63;1;6c"
                else    -> "[?1;0c"   // VT100 / ANSI
            }
            viewModelScope.launch(Dispatchers.IO) {
                telnetClient.sendRawBytes(response.toByteArray(Charsets.ISO_8859_1))
            }
        }

        // BEL → notifica a Activity para tocar o som
        emulator.onBell = { _bellEvent.postValue(Unit) }

        // VT DA Alias define também o tipo de terminal negociado no Telnet
        if (opts.vtDaAlias.isNotBlank()) telnetClient.setTerminalType(opts.vtDaAlias)
    }

    /**
     * Aplica as opções gerais de emulação (BS destrutivo, captura CR, tamanho da tela).
     * Deve ser chamado em MainActivity.onCreate() após ler settings.generalEmulationOptions.
     */
    fun setGeneralEmulationOptions(opts: GeneralEmulationOptions) {
        emulator.destructiveBackspace = opts.destructiveBackspace
        emulator.captureOnCr         = opts.captureOnCr
        // Tamanho da tela: recria o emulador com as novas dimensões (antes de conectar)
        if (opts.initialWidth != emulator.cols || opts.initialHeight != emulator.rows) {
            emulator = TerminalEmulator(opts.initialHeight, opts.initialWidth)
        }
        telnetClient.setScreenSize(opts.initialWidth, opts.initialHeight)
    }

    /** Ativa/desativa o modo binario (transmissao 8-bit). */
    fun setBinaryMode(enabled: Boolean) {
        telnetClient.setBinaryMode(enabled)
    }

    /** Ativa/desativa a simulacao de paridade (mascara o 8o bit). */
    fun setSimulateParity(enabled: Boolean) {
        telnetClient.setSimulateParity(enabled)
    }

    // Keep-alive (Mantenha o tipo vivo)
    private var keepAliveType = "TCP"
    private var keepAliveIntervalSec = 0
    private var keepAliveJob: Job? = null

    /** Configura o keep-alive: tipo (TCP/NVT/Desligado) e intervalo em segundos. */
    fun setKeepAlive(type: String, intervalSeconds: Int) {
        keepAliveType = type
        keepAliveIntervalSec = intervalSeconds
        telnetClient.setKeepAliveType(type)
    }

    /** Configura SSL/TLS: ativa TLS e, opcionalmente, certificado cliente (.p12). */
    fun setSsl(enabled: Boolean, certBytes: ByteArray? = null, certPassword: String = "") {
        telnetClient.setSsl(enabled, certBytes, certPassword)
    }

    /** Configura proxy HTTP CONNECT para rotear a conexão Telnet/SSL. */
    fun setProxy(
        enabled: Boolean, host: String, port: Int, secure: Boolean,
        username: String = "", password: String = ""
    ) {
        telnetClient.setProxy(enabled, host, port, secure, username, password)
    }

    /** Configura SSH: ativa conexão via protocolo SSH. */
    fun setSshConfig(
        enabled: Boolean, host: String, port: Int,
        username: String, password: String,
        privateKeyBytes: ByteArray? = null,
        keepAliveSec: Int = 0
    ) {
        telnetClient.setSshConfig(enabled, host, port, username, password, privateKeyBytes, keepAliveSec)
    }

    /** Inicia o envio periodico de NOP quando o keep-alive e NVT. */
    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        if (keepAliveType.equals("NVT", ignoreCase = true) && keepAliveIntervalSec > 0) {
            keepAliveJob = viewModelScope.launch(Dispatchers.IO) {
                while (isActive && telnetClient.isConnectedStatus()) {
                    delay(keepAliveIntervalSec * 1000L)
                    if (telnetClient.isConnectedStatus()) {
                        telnetClient.sendNop()
                        Timber.d("Keep-alive NVT: NOP enviado")
                    }
                }
            }
        }
    }

    // Auto-login Telnet
    private var loginState = TelnetAutoLoginState.IDLE
    private var loginPrompt = ""
    private var username = ""
    private var passwordPrompt = ""
    private var password = ""
    private var commandPrompt = ""
    private var command = ""
    private var screenBuffer = ""  // Monitora o texto recebido

    /** Configura auto-login (todos os prompts e valores). */
    fun setAutoLogin(
        loginPrompt: String, username: String,
        passwordPrompt: String, password: String,
        commandPrompt: String, command: String
    ) {
        this.loginPrompt = loginPrompt
        this.username = username
        this.passwordPrompt = passwordPrompt
        this.password = password
        this.commandPrompt = commandPrompt
        this.command = command
        // Se todos os campos estão preenchidos, ativa auto-login ao conectar
        val hasLogin = loginPrompt.isNotBlank() && username.isNotBlank()
        loginState = if (hasLogin) TelnetAutoLoginState.WAITING_LOGIN else TelnetAutoLoginState.IDLE
        Timber.d("Auto-login configurado: estado=$loginState")
    }

    /** Monitora o texto recebido e dispara as ações de auto-login (monitorar no addTerminalOutput). */
    private fun checkAutoLogin(receivedText: String) {
        if (loginState == TelnetAutoLoginState.IDLE || loginPrompt.isBlank()) return

        screenBuffer += receivedText

        when (loginState) {
            TelnetAutoLoginState.WAITING_LOGIN -> {
                if (screenBuffer.contains(loginPrompt, ignoreCase = true)) {
                    Timber.d("Auto-login: encontrado prompt '$loginPrompt', enviando usuário")
                    sendCommand(username)
                    loginState = TelnetAutoLoginState.WAITING_PASSWORD
                    screenBuffer = ""
                }
            }
            TelnetAutoLoginState.WAITING_PASSWORD -> {
                if (screenBuffer.contains(passwordPrompt, ignoreCase = true)) {
                    Timber.d("Auto-login: encontrado prompt '$passwordPrompt', enviando senha")
                    sendCommand(password)
                    loginState = if (commandPrompt.isNotBlank()) TelnetAutoLoginState.WAITING_COMMAND else TelnetAutoLoginState.COMPLETE
                    screenBuffer = ""
                }
            }
            TelnetAutoLoginState.WAITING_COMMAND -> {
                if (screenBuffer.contains(commandPrompt, ignoreCase = true)) {
                    Timber.d("Auto-login: encontrado prompt '$commandPrompt'")
                    if (command.isNotBlank()) {
                        sendCommand(command)
                        Timber.d("Auto-login: comando enviado, concluído")
                    }
                    loginState = TelnetAutoLoginState.COMPLETE
                    screenBuffer = ""
                }
            }
            else -> {}
        }
    }

    /** Envia bytes brutos ao servidor (setas, Ctrl, Esc...) sem adicionar CRLF. */
    fun sendRaw(bytes: ByteArray) {
        if (_connectionState.value != ConnectionState.CONNECTED) return

        // Aplicar transformações de transliteração antes de enviar
        var out = bytes

        // 1. Transliteração nacional: troca acentuados pelo base ASCII
        val translitMap = getTranslitMap()
        if (translitMap.isNotEmpty()) {
            out = out.map { b ->
                val c = (b.toInt() and 0xFF).toChar()
                val mapped = translitMap[c]
                mapped?.code?.toByte() ?: b
            }.toByteArray()
        }

        // 2. Proibir minúsculas: converte a-z → A-Z
        if (!allowLowercase) {
            out = out.map { b ->
                val c = b.toInt() and 0xFF
                if (c in 0x61..0x7A) (c - 0x20).toByte() else b
            }.toByteArray()
        }

        // 3. Host 7-bit: mascara o 8º bit
        if (!host8bit) {
            out = out.map { b -> (b.toInt() and 0x7F).toByte() }.toByteArray()
        }

        // ECHO mode: o terminal local exibe o que foi digitado (quando o host não ecoa)
        if (localEcho) {
            val printable = out.none { it.toInt() == 27 || (it.toInt() in 0..31 && it.toInt() != 13 && it.toInt() != 10 && it.toInt() != 8 && it.toInt() != 9) }
            if (printable) addTerminalOutput(String(out, Charsets.ISO_8859_1))
        }

        viewModelScope.launch(Dispatchers.IO) {
            telnetClient.sendRawBytes(out)
        }
    }

    // Input History Manager
    private val inputHistory = InputHistoryManager()

    // Estado da conexão
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    // Mensagens do terminal (plain text)
    private val _terminalOutput = MutableLiveData("")
    val terminalOutput: LiveData<String> = _terminalOutput

    // Tela do terminal renderizada (com cores/campos via spans)
    private val _terminalOutputStyled = MutableLiveData<CharSequence>("")
    val terminalOutputStyled: LiveData<CharSequence> = _terminalOutputStyled

    // Conexão atual
    private val _currentConnection = MutableLiveData<TelnetConnection?>(null)
    val currentConnection: LiveData<TelnetConnection?> = _currentConnection

    // Histórico de conexões
    private val _connectionHistory = MutableLiveData<List<TelnetConnection>>(emptyList())
    val connectionHistory: LiveData<List<TelnetConnection>> = _connectionHistory
    
    // Conexões salvas do banco de dados
    val savedConnections: Flow<List<SavedConnection>> = repository.getAllConnections()
    
    // ID da sessão atual
    private var currentSessionId: Long = -1L

    /**
     * Conectar a um servidor Telnet (TCP Socket Real)
     */
    fun connect(host: String, port: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionState.postValue(ConnectionState.CONNECTING)
                addTerminalOutput("Conectando a $host:$port...\n")

                val portInt = port.toIntOrNull() ?: 23

                // Conectar via TCP
                val connectResult = telnetClient.connect(host, portInt)

                if (connectResult.isSuccess) {
                    _connectionState.postValue(ConnectionState.CONNECTED)
                    addTerminalOutput("Conectado com sucesso!\n\n")

                    // Guardar conexão
                    val connection = TelnetConnection(
                        name = host,
                        host = host,
                        port = portInt
                    )
                    _currentConnection.postValue(connection)

                    // Iniciar sessão no banco de dados
                    startSession(host, portInt)

                    // Iniciar leitura de dados em background
                    startReadingData()

                    // Iniciar keep-alive (se configurado como NVT)
                    startKeepAlive()

                } else {
                    _connectionState.postValue(ConnectionState.ERROR)
                    addTerminalOutput("Erro ao conectar: ${connectResult.exceptionOrNull()?.message}\n")
                }

            } catch (e: Exception) {
                Timber.e(e, "Erro ao conectar")
                _connectionState.postValue(ConnectionState.ERROR)
                addTerminalOutput("Erro: ${e.message}\n")
            }
        }
    }

    /**
     * Leitura bloqueante contínua — bloqueia em readData() até dados chegarem
     */
    private fun startReadingData() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive && telnetClient.isConnectedStatus()) {
                val result = telnetClient.readData()
                if (result.isSuccess) {
                    val text = result.getOrNull() ?: ""
                    if (text.isNotEmpty()) addTerminalOutput(text)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Desconectado"
                    Timber.w("Leitura encerrada: $error")
                    if (!telnetClient.isConnectedStatus()) {
                        _connectionState.postValue(ConnectionState.DISCONNECTED)
                        addTerminalOutput("\nConexao perdida.\n")
                    }
                    break
                }
            }
        }
    }

    /**
     * Desconectar do servidor
     */
    fun disconnect() {
        keepAliveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val disconnectResult = telnetClient.disconnect()

                if (disconnectResult.isSuccess) {
                    // Encerrar sessão no banco de dados
                    endCurrentSession("User disconnect")
                    
                    _connectionState.postValue(ConnectionState.DISCONNECTED)
                    addTerminalOutput("Desconectado.\n")
                    _currentConnection.postValue(null)
                } else {
                    addTerminalOutput("Erro ao desconectar: ${disconnectResult.exceptionOrNull()?.message}\n")
                }

            } catch (e: Exception) {
                Timber.e(e, "Erro ao desconectar")
                endCurrentSession("Error: ${e.message}")
                _connectionState.postValue(ConnectionState.DISCONNECTED)
                addTerminalOutput("Erro ao desconectar: ${e.message}\n")
            }
        }
    }

    /**
     * Enviar comando Telnet
     */
    fun sendCommand(command: String) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            // Adicionar ao histórico de input e persistir no log de sessão
            inputHistory.add(command)
            saveCommandToHistory(command)

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Sem echo local: num terminal de tela cheia o servidor ecoa e redesenha
                    val sendResult = telnetClient.sendCommand(command)

                    if (sendResult.isFailure) {
                        Timber.w("Erro ao enviar: ${sendResult.exceptionOrNull()?.message}")
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Erro ao enviar comando")
                }
            }
        } else {
            addTerminalOutput("Erro: Não conectado ao servidor.\n")
        }
    }

    /**
     * Obter comando anterior do histórico (seta ↑)
     */
    fun getPreviousCommand(): String? {
        return inputHistory.getPrevious()
    }

    /**
     * Obter próximo comando do histórico (seta ↓)
     */
    fun getNextCommand(): String? {
        return inputHistory.getNext()
    }

    /**
     * Reset do histórico (novo input começou)
     */
    fun resetInputHistory() {
        inputHistory.reset()
    }

    /**
     * Alimenta o emulador de tela com o texto recebido e renderiza a grade inteira.
     */
    private fun addTerminalOutput(text: String) {
        try {
            emulator.feed(text)
            _terminalOutputStyled.postValue(emulator.renderSpannable())
            // Verificar auto-login (monitora prompts e envia credenciais)
            checkAutoLogin(text)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar tela")
        }
    }

    /**
     * Limpar a tela do terminal
     */
    fun clearTerminal() {
        emulator.reset()
        _terminalOutputStyled.postValue(emulator.renderSpannable())
    }

    /** Re-renderiza a tela atual sem alterar o conteúdo (usado após mudança de configurações). */
    fun refreshDisplay() {
        _terminalOutputStyled.postValue(emulator.renderSpannable())
    }

    /** Retorna o conteúdo atual da tela como lista de strings (para impressão). */
    fun getScreenLines(): List<String> = emulator.getScreenText()

    /**
     * Salvar conexão atual no banco de dados
     */
    fun saveCurrentConnection(name: String, host: String, port: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedConnection = SavedConnection(
                name = name,
                host = host,
                port = port
            )
            
            val result = repository.saveConnection(savedConnection)
            if (result > 0) {
                addTerminalOutput("Conexão '$name' ($host:$port) salva com sucesso!\n")
                Timber.d("Conexão salva: $name - $host:$port")
            } else {
                addTerminalOutput("Erro ao salvar conexão.\n")
            }
        }
    }

    /**
     * Conectar a uma conexão salva
     */
    fun connectToSaved(connection: SavedConnection) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLastUsed(connection.id)
            connect(connection.host, connection.port.toString())
        }
    }

    /**
     * Iniciar nova sessão no banco de dados
     */
    fun startSession(host: String, port: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            currentSessionId = repository.startSession(0, host, port)
        }
    }

    /**
     * Encerrar sessão no banco de dados
     */
    fun endCurrentSession(reason: String = "User disconnect") {
        if (currentSessionId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.endSession(currentSessionId.toInt(), reason)
                currentSessionId = -1L
            }
        }
    }

    /**
     * Salvar comando no histórico
     */
    fun saveCommandToHistory(command: String) {
        if (currentSessionId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveCommand(currentSessionId.toInt(), command)
            }
        }
    }

    /**
     * Cleanup ao destruir ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            telnetClient.disconnect()
        }
    }
}

/**
 * Factory para criar TelnetViewModel com Repository injetado
 */
class TelnetViewModelFactory(private val repository: TelnetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelnetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelnetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

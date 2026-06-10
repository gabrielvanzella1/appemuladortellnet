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

    // Emulador de tela VT100 (grade 80x24)
    private val emulator = TerminalEmulator()

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
    fun setProxy(enabled: Boolean, host: String, port: Int, secure: Boolean) {
        telnetClient.setProxy(enabled, host, port, secure)
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
        viewModelScope.launch(Dispatchers.IO) {
            telnetClient.sendRawBytes(bytes)
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
            // Adicionar ao histórico
            inputHistory.add(command)
            
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

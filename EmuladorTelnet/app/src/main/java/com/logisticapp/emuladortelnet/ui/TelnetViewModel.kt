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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

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

    // Mensagens do terminal (com estilos ANSI processados)
    private val _terminalOutputStyled = MutableLiveData("")
    val terminalOutputStyled: LiveData<String> = _terminalOutputStyled

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
            _terminalOutputStyled.postValue(emulator.renderHtml())
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar tela")
        }
    }

    /**
     * Limpar a tela do terminal
     */
    fun clearTerminal() {
        emulator.reset()
        _terminalOutputStyled.postValue(emulator.renderHtml())
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

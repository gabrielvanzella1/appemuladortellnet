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
import com.logisticapp.emuladortelnet.terminal.ANSIParser
import com.logisticapp.emuladortelnet.terminal.InputHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    // ANSI Parser
    private val ansiParser = ANSIParser()

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
     * Iniciar leitura contínua de dados
     */
    private fun startReadingData() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive && telnetClient.isConnectedStatus()) {
                try {
                    val readResult = telnetClient.readAvailable()

                    if (readResult.isSuccess) {
                        val lines = readResult.getOrNull() ?: emptyList()
                        for (line in lines) {
                            addTerminalOutput("$line\n")
                        }
                    } else {
                        // Erro na leitura
                        val error = readResult.exceptionOrNull()?.message ?: "Desconectado"
                        Timber.w("Erro ao ler dados: $error")

                        if (!telnetClient.isConnectedStatus()) {
                            _connectionState.postValue(ConnectionState.DISCONNECTED)
                            addTerminalOutput("Conexão perdida.\n")
                            break
                        }
                    }

                    // Aguardar um pouco antes de tentar ler novamente
                    delay(500)

                } catch (e: Exception) {
                    Timber.e(e, "Exceção ao ler dados")
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
                    addTerminalOutput("> $command\n")

                    val sendResult = telnetClient.sendCommand(command)

                    if (sendResult.isFailure) {
                        addTerminalOutput("Erro ao enviar: ${sendResult.exceptionOrNull()?.message}\n")
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Erro ao enviar comando")
                    addTerminalOutput("Erro ao enviar comando: ${e.message}\n")
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
     * Adicionar texto ao terminal (thread-safe)
     * Processa ANSI escape sequences
     */
    private fun addTerminalOutput(text: String) {
        val current = _terminalOutput.value ?: ""
        val newText = current + text

        // Atualizar plain text
        _terminalOutput.postValue(newText)

        // Processar ANSI e atualizar styled
        try {
            val styledSegments = ansiParser.parse(newText)
            val htmlOutput = ansiParser.toHtmlSpan(styledSegments)
            Timber.d("ANSI parsed: ${styledSegments.size} segments")
            Timber.d("HTML output: ${htmlOutput.take(100)}...")
            _terminalOutputStyled.postValue(htmlOutput)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar ANSI")
            _terminalOutputStyled.postValue(newText)
        }
    }

    /**
     * Limpar saída do terminal
     */
    fun clearTerminal() {
        _terminalOutput.value = ""
    }

    /**
     * Salvar conexão atual no banco de dados
     */
    fun saveCurrentConnection(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val host = _currentConnection.value?.host ?: return@launch
            val port = _currentConnection.value?.port ?: 23
            
            val savedConnection = SavedConnection(
                name = name,
                host = host,
                port = port.toInt()
            )
            
            val result = repository.saveConnection(savedConnection)
            if (result > 0) {
                addTerminalOutput("Conexão '$name' salva com sucesso!\n")
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

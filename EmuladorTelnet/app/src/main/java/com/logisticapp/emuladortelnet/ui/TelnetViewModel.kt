package com.logisticapp.emuladortelnet.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.data.TelnetConnection
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar estado da conexão Telnet
 */
class TelnetViewModel : ViewModel() {

    // Estado da conexão
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    // Mensagens do terminal
    private val _terminalOutput = MutableLiveData("")
    val terminalOutput: LiveData<String> = _terminalOutput

    // Conexão atual
    private val _currentConnection = MutableLiveData<TelnetConnection?>(null)
    val currentConnection: LiveData<TelnetConnection?> = _currentConnection

    // Histórico de conexões
    private val _connectionHistory = MutableLiveData<List<TelnetConnection>>(emptyList())
    val connectionHistory: LiveData<List<TelnetConnection>> = _connectionHistory

    /**
     * Conectar a um servidor Telnet
     */
    fun connect(host: String, port: String) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                _terminalOutput.value = "Conectando a $host:$port...\n"

                // TODO: Implementar conexão real
                Thread.sleep(1000)

                _connectionState.value = ConnectionState.CONNECTED
                _terminalOutput.value = "${_terminalOutput.value}Conectado com sucesso!\n"

                // Guardar conexão no histórico
                val connection = TelnetConnection(
                    name = host,
                    host = host,
                    port = port.toIntOrNull() ?: 23
                )
                _currentConnection.value = connection

            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                _terminalOutput.value = "${_terminalOutput.value}Erro: ${e.message}\n"
            }
        }
    }

    /**
     * Desconectar do servidor
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.DISCONNECTED
                _terminalOutput.value = "${_terminalOutput.value}Desconectado.\n"
                _currentConnection.value = null
            } catch (e: Exception) {
                _terminalOutput.value = "${_terminalOutput.value}Erro ao desconectar: ${e.message}\n"
            }
        }
    }

    /**
     * Enviar comando Telnet
     */
    fun sendCommand(command: String) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            viewModelScope.launch {
                try {
                    _terminalOutput.value = "${_terminalOutput.value}> $command\n"
                    // TODO: Implementar envio real
                } catch (e: Exception) {
                    _terminalOutput.value = "${_terminalOutput.value}Erro ao enviar comando: ${e.message}\n"
                }
            }
        }
    }

    /**
     * Limpar saída do terminal
     */
    fun clearTerminal() {
        _terminalOutput.value = ""
    }
}

package com.logisticapp.emuladortelnet.data

/**
 * Modelo de dados para uma conexão Telnet
 */
data class TelnetConnection(
    val id: Long = 0,
    val name: String = "",
    val host: String = "",
    val port: Int = 23,
    val username: String = "",
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Enum para estados de conexão
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Classe para representar um evento de conexão
 */
data class ConnectionEvent(
    val state: ConnectionState,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

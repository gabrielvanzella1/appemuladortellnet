package com.logisticapp.emuladortelnet.database

// Room dependencies removed - using SharedPreferences instead

/**
 * Conexão Telnet salva para reutilização
 */
data class SavedConnection(
    val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 23,
    val username: String = "",
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    // Configuracoes avancadas
    val encoding: String = "UTF-8",         // UTF-8 | ISO-8859-1 | CP850
    val terminalType: String = "VT100",     // VT100 | VT220 | ANSI | XTERM
    val timeoutSeconds: Int = 30,           // Timeout de conexao em segundos
    val keepAlive: Boolean = true,          // Enviar keep-alive
    val localEcho: Boolean = false          // Eco local de comandos
)

/**
 * Histórico de comandos por sessão
 */
data class CommandHistory(
    val id: Int = 0,
    val sessionId: Int,         // Referência à sessão
    val command: String,        // Comando digitado
    val response: String = "",  // Resposta do servidor (primeiras 500 chars)
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Log de sessão (quando conectou, quando desconectou)
 */
data class SessionLog(
    val id: Int = 0,
    val connectionId: Int,      // Qual conexão foi usada
    val host: String,           // Host conectado
    val port: Int,              // Porta
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,     // 0 = ainda conectado
    val reason: String = ""     // Motivo da desconexão
) {
    val duration: Long
        get() = if (endTime == 0L) System.currentTimeMillis() - startTime else endTime - startTime
    
    val isActive: Boolean
        get() = endTime == 0L
}

/**
 * Preferências do app
 */
data class AppPreference(
    val key: String,
    val value: String
)

/**
 * Usuário para autenticação do app
 */
data class User(
    val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = 0L
)


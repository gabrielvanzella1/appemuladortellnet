package com.logisticapp.emuladortelnet.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

/**
 * Conexão Telnet salva para reutilização
 */
@Entity(tableName = "saved_connections")
data class SavedConnection(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // Nome da conexão (ex: "Servidor Principal")
    val host: String,           // IP ou hostname
    val port: Int = 23,         // Porta Telnet
    val username: String = "",  // Username (opcional)
    val password: String = "",  // Password (opcional)
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

/**
 * Histórico de comandos por sessão
 */
@Entity(
    tableName = "command_history",
    foreignKeys = [
        ForeignKey(
            entity = SessionLog::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CommandHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,         // Referência à sessão
    val command: String,        // Comando digitado
    val response: String = "",  // Resposta do servidor (primeiras 500 chars)
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Log de sessão (quando conectou, quando desconectou)
 */
@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey(autoGenerate = true)
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
@Entity(tableName = "preferences")
data class AppPreference(
    @PrimaryKey
    val key: String,
    val value: String
)

/**
 * Informações de Licença
 */
@Entity(tableName = "license_info")
data class LicenseInfo(
    @PrimaryKey
    val id: Int = 1,  // Sempre 1 (uma única licença por app)
    val licenseKey: String,     // Chave de licença
    val licenseType: String,    // TRIAL ou PREMIUM
    val deviceFingerprint: String,  // Fingerprint do device
    val createdAt: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long,  // Timestamp de expiração
    val isValid: Boolean = true,
    val activationDate: Long = System.currentTimeMillis()
)


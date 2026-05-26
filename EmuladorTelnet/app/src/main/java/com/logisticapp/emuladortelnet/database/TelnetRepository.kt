package com.logisticapp.emuladortelnet.database

import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository para abstrair acesso ao banco de dados
 */
class TelnetRepository(private val database: AppDatabase) {
    
    // SavedConnection operations
    suspend fun saveConnection(connection: SavedConnection): Long {
        return try {
            database.savedConnectionDao().insert(connection).also {
                Timber.d("Conexão salva: ${connection.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao salvar conexão")
            -1L
        }
    }
    
    suspend fun updateConnection(connection: SavedConnection) {
        try {
            database.savedConnectionDao().update(connection)
            Timber.d("Conexão atualizada: ${connection.name}")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao atualizar conexão")
        }
    }
    
    suspend fun deleteConnection(id: Int) {
        try {
            database.savedConnectionDao().deleteById(id)
            Timber.d("Conexão deletada: $id")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao deletar conexão")
        }
    }
    
    fun getAllConnections(): Flow<List<SavedConnection>> {
        return database.savedConnectionDao().getAllConnections()
    }
    
    fun getFavoriteConnections(): Flow<List<SavedConnection>> {
        return database.savedConnectionDao().getFavoriteConnections()
    }
    
    suspend fun getConnectionById(id: Int): SavedConnection? {
        return database.savedConnectionDao().getConnectionById(id)
    }
    
    suspend fun updateLastUsed(connectionId: Int) {
        database.savedConnectionDao().updateLastUsed(connectionId)
    }
    
    // CommandHistory operations
    suspend fun saveCommand(sessionId: Int, command: String, response: String = "") {
        try {
            val history = CommandHistory(
                sessionId = sessionId,
                command = command,
                response = response.take(500)  // Limitar resposta a 500 chars
            )
            database.commandHistoryDao().insert(history)
            Timber.d("Comando salvo: $command")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao salvar comando")
        }
    }
    
    suspend fun getCommandsBySession(sessionId: Int): List<CommandHistory> {
        return try {
            database.commandHistoryDao().getCommandsBySession(sessionId)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar comandos")
            emptyList()
        }
    }
    
    suspend fun getRecentCommands(limit: Int = 50): List<CommandHistory> {
        return try {
            database.commandHistoryDao().getRecentCommands(limit)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar comandos recentes")
            emptyList()
        }
    }
    
    // SessionLog operations
    suspend fun startSession(connectionId: Int, host: String, port: Int): Long {
        return try {
            val session = SessionLog(
                connectionId = connectionId,
                host = host,
                port = port
            )
            database.sessionLogDao().insert(session).also {
                Timber.d("Sessão iniciada: $host:$port")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao iniciar sessão")
            -1L
        }
    }
    
    suspend fun endSession(sessionId: Int, reason: String = "") {
        try {
            database.sessionLogDao().closeSession(sessionId, reason = reason)
            Timber.d("Sessão encerrada: $sessionId - $reason")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao encerrar sessão")
        }
    }
    
    fun getAllSessions(): Flow<List<SessionLog>> {
        return database.sessionLogDao().getAllSessions()
    }
    
    suspend fun getSessionsByConnection(connectionId: Int): List<SessionLog> {
        return try {
            database.sessionLogDao().getSessionsByConnection(connectionId)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar sessões")
            emptyList()
        }
    }
    
    // Preferences operations
    suspend fun setPreference(key: String, value: String) {
        try {
            database.appPreferenceDao().setPreference(key, value)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao salvar preferência")
        }
    }
    
    suspend fun getPreference(key: String, defaultValue: String = ""): String {
        return try {
            database.appPreferenceDao().getPreferenceValue(key) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar preferência")
            defaultValue
        }
    }

    // License operations
    suspend fun saveLicense(licenseInfo: LicenseInfo): Long {
        return try {
            database.licenseDao().insert(licenseInfo).also {
                Timber.d("Licença salva: ${licenseInfo.licenseType}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao salvar licença")
            -1L
        }
    }

    suspend fun getLicense(): LicenseInfo? {
        return try {
            database.licenseDao().getLicense()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar licença")
            null
        }
    }

    fun getLicenseFlow(): Flow<LicenseInfo?> {
        return database.licenseDao().getLicenseFlow()
    }

    suspend fun updateLicenseValidity(isValid: Boolean) {
        try {
            database.licenseDao().updateValidityStatus(isValid)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao atualizar validação de licença")
        }
    }

    suspend fun deleteLicense() {
        try {
            database.licenseDao().deleteLicense()
            Timber.d("Licença deletada")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao deletar licença")
        }
    }

    // User operations (Autenticação)
    suspend fun registerUser(email: String, password: String, fullName: String = ""): Long {
        return try {
            val user = User(
                email = email,
                passwordHash = com.logisticapp.emuladortelnet.utils.PasswordUtils.hashPassword(password),
                fullName = fullName
            )
            database.userDao().insert(user).also {
                Timber.d("Usuário registrado: $email")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao registrar usuário")
            -1L
        }
    }

    suspend fun authenticateUser(email: String, password: String): User? {
        return try {
            val user = database.userDao().getUserByEmail(email)
            if (user != null && user.isActive) {
                if (com.logisticapp.emuladortelnet.utils.PasswordUtils.verifyPassword(password, user.passwordHash)) {
                    // Atualizar último login
                    database.userDao().updateLastLogin(user.id)
                    Timber.d("Autenticação bem-sucedida: $email")
                    user
                } else {
                    Timber.w("Falha de autenticação: senha incorreta para $email")
                    null
                }
            } else {
                Timber.w("Usuário não encontrado ou inativo: $email")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao autenticar usuário")
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            database.userDao().getUserByEmail(email)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar usuário")
            null
        }
    }

    suspend fun getUserById(id: Int): User? {
        return try {
            database.userDao().getUserById(id)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao buscar usuário por ID")
            null
        }
    }

    suspend fun updateUserPassword(userId: Int, newPassword: String) {
        try {
            val hashedPassword = com.logisticapp.emuladortelnet.utils.PasswordUtils.hashPassword(newPassword)
            database.userDao().updatePassword(userId, hashedPassword)
            Timber.d("Senha do usuário atualizada: $userId")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao atualizar senha do usuário")
        }
    }

    suspend fun createTestUser() {
        try {
            // Verificar se já existe
            if (database.userDao().getUserByEmail("teste@auticode.com.br") == null) {
                registerUser("teste@auticode.com.br", "Teste@123", "Usuário Teste")
                Timber.d("Usuário de teste criado")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao criar usuário de teste")
        }
    }
    
    companion object {
        private var instance: TelnetRepository? = null
        
        fun getInstance(database: AppDatabase): TelnetRepository {
            return instance ?: synchronized(this) {
                instance ?: TelnetRepository(database).also { instance = it }
            }
        }
    }
}

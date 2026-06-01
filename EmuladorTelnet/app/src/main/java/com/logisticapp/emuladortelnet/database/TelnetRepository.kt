package com.logisticapp.emuladortelnet.database

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class TelnetRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("telnet_hosts", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _connections = MutableStateFlow<List<SavedConnection>>(emptyList())

    init {
        _connections.value = loadConnectionsFromPrefs()
        Timber.d("TelnetRepository: ${_connections.value.size} hosts carregados")
    }

    // ------------------------------------------------------------------
    // Hosts / Conexoes
    // ------------------------------------------------------------------

    fun getAllConnections(): Flow<List<SavedConnection>> = _connections.asStateFlow()

    suspend fun saveConnection(connection: SavedConnection): Long {
        val list = _connections.value.toMutableList()
        val newId = (list.maxOfOrNull { it.id } ?: 0) + 1
        val toSave = connection.copy(id = newId, createdAt = System.currentTimeMillis())
        list.add(toSave)
        persistAndEmit(list)
        Timber.d("Host salvo: ${toSave.name} (id=$newId)")
        return newId.toLong()
    }

    suspend fun updateConnection(connection: SavedConnection) {
        val list = _connections.value.toMutableList()
        val idx = list.indexOfFirst { it.id == connection.id }
        if (idx >= 0) {
            list[idx] = connection
            persistAndEmit(list)
            Timber.d("Host atualizado: ${connection.name}")
        }
    }

    suspend fun deleteConnection(id: Int) {
        val list = _connections.value.filter { it.id != id }.toMutableList()
        persistAndEmit(list)
        Timber.d("Host removido: id=$id")
    }

    suspend fun getConnectionById(id: Int): SavedConnection? =
        _connections.value.firstOrNull { it.id == id }

    suspend fun updateLastUsed(connectionId: Int) {
        val list = _connections.value.toMutableList()
        val idx = list.indexOfFirst { it.id == connectionId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(lastUsed = System.currentTimeMillis())
            persistAndEmit(list)
        }
    }

    fun getFavoriteConnections(): Flow<List<SavedConnection>> =
        MutableStateFlow(_connections.value.filter { it.isFavorite }).asStateFlow()

    /** Lista atual (acesso sincrono) */
    fun currentConnections(): List<SavedConnection> = _connections.value

    /** Exporta todas as sessoes como JSON */
    fun exportJson(): String = gson.toJson(_connections.value)

    /** Importa sessoes de um JSON, atribuindo novos ids. Retorna quantas foram importadas. */
    suspend fun importJson(json: String): Int {
        val imported: List<SavedConnection> = try {
            val type = object : TypeToken<List<SavedConnection>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Timber.e(e, "JSON invalido na importacao")
            null
        } ?: return 0

        val list = _connections.value.toMutableList()
        var nextId = (list.maxOfOrNull { it.id } ?: 0) + 1
        for (conn in imported) {
            list.add(conn.copy(id = nextId))
            nextId++
        }
        persistAndEmit(list)
        Timber.d("Importadas ${imported.size} sessoes")
        return imported.size
    }

    // ------------------------------------------------------------------
    // Persistencia interna
    // ------------------------------------------------------------------

    private fun persistAndEmit(list: List<SavedConnection>) {
        val json = gson.toJson(list)
        prefs.edit().putString("connections", json).apply()
        _connections.value = list
    }

    private fun loadConnectionsFromPrefs(): List<SavedConnection> {
        val json = prefs.getString("connections", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedConnection>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Erro ao carregar hosts")
            emptyList()
        }
    }

    // ------------------------------------------------------------------
    // Preferences gerais
    // ------------------------------------------------------------------

    suspend fun setPreference(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    suspend fun getPreference(key: String, defaultValue: String = ""): String =
        prefs.getString(key, defaultValue) ?: defaultValue

    // ------------------------------------------------------------------
    // Session log (stubs - sem persistencia por enquanto)
    // ------------------------------------------------------------------

    suspend fun startSession(connectionId: Int, host: String, port: Int): Long {
        Timber.d("Sessao iniciada: $host:$port")
        return System.currentTimeMillis()
    }

    suspend fun endSession(sessionId: Int, reason: String = "") {
        Timber.d("Sessao encerrada: $reason")
    }

    suspend fun saveCommand(sessionId: Int, command: String, response: String = "") {
        Timber.d("Comando: $command")
    }

    // ------------------------------------------------------------------
    // Auth stubs (modulo de login desabilitado)
    // ------------------------------------------------------------------

    suspend fun authenticateUser(email: String, password: String): User? = null

    suspend fun createTestUser() { Timber.d("createTestUser stub") }

    // ------------------------------------------------------------------
    // Singleton
    // ------------------------------------------------------------------

    companion object {
        @Volatile private var instance: TelnetRepository? = null

        fun getInstance(context: Context): TelnetRepository =
            instance ?: synchronized(this) {
                instance ?: TelnetRepository(context.applicationContext).also { instance = it }
            }
    }
}

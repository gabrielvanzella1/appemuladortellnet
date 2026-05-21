package com.logisticapp.emuladortelnet.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO para SavedConnection
 */
@Dao
interface SavedConnectionDao {
    
    @Insert
    suspend fun insert(connection: SavedConnection): Long
    
    @Update
    suspend fun update(connection: SavedConnection)
    
    @Delete
    suspend fun delete(connection: SavedConnection)
    
    @Query("SELECT * FROM saved_connections ORDER BY lastUsed DESC")
    fun getAllConnections(): Flow<List<SavedConnection>>
    
    @Query("SELECT * FROM saved_connections WHERE id = :id")
    suspend fun getConnectionById(id: Int): SavedConnection?
    
    @Query("SELECT * FROM saved_connections WHERE isFavorite = 1 ORDER BY name")
    fun getFavoriteConnections(): Flow<List<SavedConnection>>
    
    @Query("SELECT * FROM saved_connections ORDER BY name ASC")
    fun getConnectionsByName(): Flow<List<SavedConnection>>
    
    @Query("UPDATE saved_connections SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM saved_connections WHERE id = :id")
    suspend fun deleteById(id: Int)
}

/**
 * DAO para CommandHistory
 */
@Dao
interface CommandHistoryDao {
    
    @Insert
    suspend fun insert(history: CommandHistory): Long
    
    @Delete
    suspend fun delete(history: CommandHistory)
    
    @Query("SELECT * FROM command_history WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getCommandsBySession(sessionId: Int): List<CommandHistory>
    
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentCommands(limit: Int = 50): List<CommandHistory>
    
    @Query("SELECT DISTINCT command FROM command_history WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getCommandsForSession(sessionId: Int): List<String>
    
    @Query("DELETE FROM command_history WHERE sessionId = :sessionId")
    suspend fun deleteCommandsBySession(sessionId: Int)
}

/**
 * DAO para SessionLog
 */
@Dao
interface SessionLogDao {
    
    @Insert
    suspend fun insert(session: SessionLog): Long
    
    @Update
    suspend fun update(session: SessionLog)
    
    @Delete
    suspend fun delete(session: SessionLog)
    
    @Query("SELECT * FROM session_logs ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionLog>>
    
    @Query("SELECT * FROM session_logs WHERE id = :id")
    suspend fun getSessionById(id: Int): SessionLog?
    
    @Query("SELECT * FROM session_logs WHERE connectionId = :connectionId ORDER BY startTime DESC")
    suspend fun getSessionsByConnection(connectionId: Int): List<SessionLog>
    
    @Query("SELECT * FROM session_logs WHERE endTime = 0 ORDER BY startTime DESC")
    suspend fun getActiveSessions(): List<SessionLog>
    
    @Query("UPDATE session_logs SET endTime = :endTime, reason = :reason WHERE id = :id")
    suspend fun closeSession(id: Int, endTime: Long = System.currentTimeMillis(), reason: String = "")
    
    @Query("DELETE FROM session_logs WHERE id = :id")
    suspend fun deleteById(id: Int)
}

/**
 * DAO para AppPreference
 */
@Dao
interface AppPreferenceDao {
    
    @Insert
    suspend fun insert(preference: AppPreference)
    
    @Update
    suspend fun update(preference: AppPreference)
    
    @Query("SELECT * FROM preferences WHERE key = :key")
    suspend fun getPreference(key: String): AppPreference?
    
    @Query("SELECT value FROM preferences WHERE key = :key")
    suspend fun getPreferenceValue(key: String): String?
    
    @Query("UPDATE preferences SET value = :value WHERE key = :key")
    suspend fun updatePreference(key: String, value: String)
    
    @Query("INSERT OR REPLACE INTO preferences (key, value) VALUES (:key, :value)")
    suspend fun setPreference(key: String, value: String)
}

/**
 * DAO para LicenseInfo
 */
@Dao
interface LicenseDao {
    
    @Insert
    suspend fun insert(license: LicenseInfo): Long
    
    @Update
    suspend fun update(license: LicenseInfo)
    
    @Query("SELECT * FROM license_info WHERE id = 1")
    suspend fun getLicense(): LicenseInfo?
    
    @Query("SELECT * FROM license_info WHERE id = 1")
    fun getLicenseFlow(): Flow<LicenseInfo?>
    
    @Query("UPDATE license_info SET isValid = :isValid WHERE id = 1")
    suspend fun updateValidityStatus(isValid: Boolean)
    
    @Query("DELETE FROM license_info WHERE id = 1")
    suspend fun deleteLicense()
}


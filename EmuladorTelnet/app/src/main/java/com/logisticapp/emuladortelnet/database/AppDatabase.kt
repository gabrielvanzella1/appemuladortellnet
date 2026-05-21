package com.logisticapp.emuladortelnet.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database principal do app com Room
 */
@Database(
    entities = [
        SavedConnection::class,
        CommandHistory::class,
        SessionLog::class,
        AppPreference::class,
        LicenseInfo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun savedConnectionDao(): SavedConnectionDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun licenseDao(): LicenseDao
    
    companion object {
        private var instance: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "emulador_telnet.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

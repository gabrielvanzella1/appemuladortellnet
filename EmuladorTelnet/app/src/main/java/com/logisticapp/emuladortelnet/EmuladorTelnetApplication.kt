package com.logisticapp.emuladortelnet

import android.app.Application
import com.logisticapp.emuladortelnet.database.AppDatabase
import com.logisticapp.emuladortelnet.database.TelnetRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class EmuladorTelnetApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Timber para logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("App inicializado")

        // Criar usuário de teste na inicialização do app
        GlobalScope.launch {
            try {
                val database = AppDatabase.getInstance(applicationContext)
                val repository = TelnetRepository.getInstance(database)
                repository.createTestUser()
                Timber.d("Usuário de teste criado/verificado na inicialização")
            } catch (e: Exception) {
                Timber.e(e, "Erro ao criar usuário de teste na inicialização")
            }
        }
    }
}

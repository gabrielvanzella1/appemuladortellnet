package com.logisticapp.scantemonitor

import android.app.Application
import timber.log.Timber

class MonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Pin.autoCheck()
        }
        // Garante o serviço rodando sempre que o processo sobe (ex.: após ser encerrado)
        MonitorService.iniciar(this)
    }
}

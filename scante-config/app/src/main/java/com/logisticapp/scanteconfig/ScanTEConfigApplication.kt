package com.logisticapp.scanteconfig

import android.app.Application
import timber.log.Timber

class ScanTEConfigApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

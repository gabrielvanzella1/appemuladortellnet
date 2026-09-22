package com.logisticapp.scantebrowser

import android.app.Application
import timber.log.Timber

class ScanTEBrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

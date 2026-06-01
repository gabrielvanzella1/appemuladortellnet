package com.logisticapp.emuladortelnet.settings

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo

/**
 * Configuracoes gerais do app, persistidas em SharedPreferences.
 * Singleton acessado via AppSettings.get(context).
 */
class AppSettings private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Orientacao da tela
    var orientation: Int
        get() = prefs.getInt(K_ORIENTATION, ORIENTATION_AUTO)
        set(v) { prefs.edit().putInt(K_ORIENTATION, v).apply() }

    // Conexao automatica na inicializacao
    var autoConnect: Boolean
        get() = prefs.getBoolean(K_AUTO_CONNECT, false)
        set(v) { prefs.edit().putBoolean(K_AUTO_CONNECT, v).apply() }

    // Reconectar automaticamente apos conexao perdida
    var autoReconnect: Boolean
        get() = prefs.getBoolean(K_AUTO_RECONNECT, false)
        set(v) { prefs.edit().putBoolean(K_AUTO_RECONNECT, v).apply() }

    // Desconectar na tela de bloqueio
    var disconnectOnLock: Boolean
        get() = prefs.getBoolean(K_DISCONNECT_ON_LOCK, false)
        set(v) { prefs.edit().putBoolean(K_DISCONNECT_ON_LOCK, v).apply() }

    // Nunca bloquear a tela quando conectado (manter tela ligada)
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(K_KEEP_SCREEN_ON, false)
        set(v) { prefs.edit().putBoolean(K_KEEP_SCREEN_ON, v).apply() }

    // Ignorar otimizacao de bateria
    var ignoreBattery: Boolean
        get() = prefs.getBoolean(K_IGNORE_BATTERY, false)
        set(v) { prefs.edit().putBoolean(K_IGNORE_BATTERY, v).apply() }

    // Teclado ativado no terminal
    var keyboardEnabled: Boolean
        get() = prefs.getBoolean(K_KEYBOARD_ENABLED, true)
        set(v) { prefs.edit().putBoolean(K_KEYBOARD_ENABLED, v).apply() }

    /** Aplica a orientacao escolhida na Activity informada. */
    fun applyOrientation(activity: Activity) {
        activity.requestedOrientation = when (orientation) {
            ORIENTATION_PORTRAIT  -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else                  -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    companion object {
        const val ORIENTATION_AUTO = 0
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2

        private const val K_ORIENTATION = "orientation"
        private const val K_AUTO_CONNECT = "auto_connect"
        private const val K_AUTO_RECONNECT = "auto_reconnect"
        private const val K_DISCONNECT_ON_LOCK = "disconnect_on_lock"
        private const val K_KEEP_SCREEN_ON = "keep_screen_on"
        private const val K_IGNORE_BATTERY = "ignore_battery"
        private const val K_KEYBOARD_ENABLED = "keyboard_enabled"

        @Volatile private var instance: AppSettings? = null

        fun get(context: Context): AppSettings =
            instance ?: synchronized(this) {
                instance ?: AppSettings(context).also { instance = it }
            }
    }
}

package com.logisticapp.scanteconfig.config

import android.content.Context
import android.content.Intent
import android.os.Environment
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.io.File

/**
 * Lê/grava a config compartilhada em `/sdcard/ScanTE/browser_config.json` —
 * o ScanTE Browser lê desse mesmo arquivo. "Salvar" só grava; "Atualizar
 * Configuração" grava e também avisa o Browser (se estiver aberto) pra
 * recarregar na hora, sem precisar reiniciar — mesmo par de botões do
 * EZConfig da Honeywell (Save / Update Configuration).
 */
class ConfigRepository(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun sharedConfigFile(): File =
        File(Environment.getExternalStorageDirectory(), "ScanTE/browser_config.json")

    fun load(): BrowserConfig {
        val file = sharedConfigFile()
        return try {
            if (file.exists()) {
                gson.fromJson(file.readText(), BrowserConfig::class.java)
            } else {
                BrowserConfig()
            }
        } catch (e: Exception) {
            Timber.e(e, "ConfigRepository: falha ao ler config compartilhada")
            BrowserConfig()
        }
    }

    fun save(config: BrowserConfig) {
        val file = sharedConfigFile()
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(config))
        Timber.d("ConfigRepository: config salva em ${file.absolutePath}")
    }

    /** Broadcast que o ScanTE Browser escuta pra recarregar a config sem reiniciar. */
    fun notifyBrowserToReload() {
        val intent = Intent(ACTION_RELOAD_CONFIG).apply {
            setPackage(BROWSER_PACKAGE)
        }
        context.sendBroadcast(intent)
        Timber.d("ConfigRepository: broadcast de atualização enviado pro ScanTE Browser")
    }

    companion object {
        const val BROWSER_PACKAGE = "com.logisticapp.scantebrowser"
        const val ACTION_RELOAD_CONFIG = "com.logisticapp.scantebrowser.RELOAD_CONFIG"
    }
}

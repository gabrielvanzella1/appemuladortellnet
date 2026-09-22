package com.logisticapp.scantebrowser.config

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import timber.log.Timber
import java.io.File

/**
 * Carrega a configuração do browser (portais + mapeamento de teclas).
 *
 * O ScanTE Browser não tem tela de configuração própria — quem edita é o
 * app separado ScanTE Config (equivalente ao EZConfig da Honeywell, outro
 * ícone no coletor). Os dois apps trocam configuração por um arquivo em
 * pasta pública do armazenamento (não pela pasta privada de cada app, que
 * o outro processo não consegue enxergar).
 *
 * Prioridade: arquivo compartilhado (`/sdcard/ScanTE/browser_config.json`,
 * escrito pelo ScanTE Config) → fallback para `assets/default_config.json`
 * (exemplo embutido no APK, usado só se o ScanTE Config nunca rodou).
 */
class ConfigRepository(private val context: Context) {

    private val gson = Gson()

    fun sharedConfigFile(): File =
        File(Environment.getExternalStorageDirectory(), "ScanTE/browser_config.json")

    fun load(): BrowserConfig {
        val shared = sharedConfigFile()
        return try {
            if (shared.exists()) {
                Timber.d("ConfigRepository: carregando config compartilhada (${shared.absolutePath})")
                gson.fromJson(shared.readText(), BrowserConfig::class.java)
            } else {
                Timber.d("ConfigRepository: sem config do ScanTE Config ainda, usando default dos assets")
                loadDefaultFromAssets()
            }
        } catch (e: Exception) {
            Timber.e(e, "ConfigRepository: falha ao carregar config, caindo para default")
            loadDefaultFromAssets()
        }
    }

    private fun loadDefaultFromAssets(): BrowserConfig {
        val json = context.assets.open("default_config.json").bufferedReader().use { it.readText() }
        return gson.fromJson(json, BrowserConfig::class.java)
    }
}

package com.logisticapp.emuladortelnet.settings

import android.content.Context
import org.json.JSONObject

/**
 * Guarda o aviso de "tem versão nova" vindo da validação de licença (bloco
 * "atualizacao"). Fica em disco (não só em memória) porque quem recebe a
 * resposta é o LicenseViewModel na tela de gate — que já pode ter fechado
 * quando o HostsActivity for checar — igual o CompanyConfigStore resolve o
 * mesmo problema pra tema/teclas.
 */
object UpdateInfoStore {

    private const val PREFS = "update_info"
    private const val K_VERSION_CODE = "version_code"
    private const val K_VERSION_NAME = "version_name"
    private const val K_CHANGELOG = "changelog"
    private const val K_DOWNLOAD_URL = "download_url"
    private const val K_DISMISSED_CODE = "dismissed_version_code"

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val downloadUrl: String
    )

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Salva o bloco "atualizacao" vindo da API. null/vazio = limpa (sem atualização pendente). */
    fun save(context: Context, atualizacaoJson: String?) {
        val p = prefs(context)
        if (atualizacaoJson.isNullOrBlank() || atualizacaoJson == "null") {
            p.edit().remove(K_VERSION_CODE).remove(K_VERSION_NAME).remove(K_CHANGELOG).remove(K_DOWNLOAD_URL).apply()
            return
        }
        try {
            val obj = JSONObject(atualizacaoJson)
            p.edit()
                .putInt(K_VERSION_CODE, obj.optInt("version_code"))
                .putString(K_VERSION_NAME, obj.optString("version_name"))
                .putString(K_CHANGELOG, obj.optString("changelog"))
                .putString(K_DOWNLOAD_URL, obj.optString("download_url"))
                .apply()
        } catch (e: Exception) {
            // JSON inválido — ignora, não trava o app por causa de um aviso de update
        }
    }

    /** Atualização pendente, ou null se já está na mais recente (ou nunca verificou). */
    fun pending(context: Context): UpdateInfo? {
        val p = prefs(context)
        val code = p.getInt(K_VERSION_CODE, 0)
        if (code <= 0) return null
        return UpdateInfo(
            versionCode = code,
            versionName = p.getString(K_VERSION_NAME, "") ?: "",
            changelog = p.getString(K_CHANGELOG, "") ?: "",
            downloadUrl = p.getString(K_DOWNLOAD_URL, "") ?: ""
        )
    }

    /** "Depois" — não pergunta de novo pra essa MESMA versão (uma versão mais nova ainda avisa). */
    fun dismiss(context: Context, versionCode: Int) {
        prefs(context).edit().putInt(K_DISMISSED_CODE, versionCode).apply()
    }

    fun wasDismissed(context: Context, versionCode: Int): Boolean =
        prefs(context).getInt(K_DISMISSED_CODE, 0) >= versionCode
}

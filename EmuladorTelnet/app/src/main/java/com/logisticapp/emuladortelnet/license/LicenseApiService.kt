package com.logisticapp.emuladortelnet.license

import com.logisticapp.emuladortelnet.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LicenseApiService {

    companion object {
        // BASE_URL e API_SECRET vêm do flavor (production/sandbox) — ver app/build.gradle.kts.
        // DEVEM ser idênticos ao API_SECRET do config.php do respectivo servidor.
        const val BASE_URL = BuildConfig.BASE_URL
        private const val API_SECRET = BuildConfig.API_SECRET
        private const val TIMEOUT_MS = 15_000
    }

    data class ValidacaoResult(
        val sucesso: Boolean,
        val chave: String = "",
        val tipo: String = "",
        val diasRestantes: Int = -1,
        val expiraEm: String = "",
        val erro: String = "",
        // JSON do bloco "config" (personalização da empresa). "" = empresa não personalizou.
        val configJson: String = ""
    )

    suspend fun pingServidor(
        deviceId: String,
        deviceNome: String,
        appVersion: String,
        licenseKey: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/dispositivo/ping")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $API_SECRET")
                setRequestProperty("X-API-KEY", API_SECRET)  // fallback: Apache remove o Authorization em hosts compartilhados
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
            }
            val body = JSONObject().apply {
                put("device_id",   deviceId)
                put("device_nome", deviceNome)
                put("app_version", appVersion)
                if (licenseKey != null) put("license_key", licenseKey)
            }.toString()
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            conn.responseCode
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validarChave(
        chave: String,
        deviceId: String,
        deviceNome: String
    ): Result<ValidacaoResult> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/licenca/validar")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $API_SECRET")
                setRequestProperty("X-API-KEY", API_SECRET)  // fallback: Apache remove o Authorization em hosts compartilhados
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
            }

            val body = JSONObject().apply {
                put("chave", chave.trim().uppercase())
                put("device_id", deviceId)
                put("device_nome", deviceNome)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val response = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            val json = JSONObject(response)

            if (json.optBoolean("valida", false)) {
                val config = if (json.isNull("config")) "" else json.optJSONObject("config")?.toString() ?: ""
                Result.success(
                    ValidacaoResult(
                        sucesso = true,
                        chave = chave.trim().uppercase(),
                        tipo = json.optString("tipo", "vitalicia"),
                        diasRestantes = if (json.isNull("dias_restantes")) -1 else json.optInt("dias_restantes", -1),
                        expiraEm = json.optString("expira_em"),
                        configJson = config
                    )
                )
            } else {
                Result.success(
                    ValidacaoResult(
                        sucesso = false,
                        erro = json.optString("mensagem", json.optString("erro", "Chave inválida ou já vinculada a outro dispositivo."))
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

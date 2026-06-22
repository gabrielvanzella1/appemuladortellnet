package com.logisticapp.emuladortelnet.license

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
        // Altere para a URL de produção após o deploy do scante-admin
        const val BASE_URL = "http://scante-admin.test"
        private const val API_SECRET = "SCANTE_API_SECRET_MUDE_ISSO_2026"
        private const val TIMEOUT_MS = 15_000
    }

    data class ValidacaoResult(
        val sucesso: Boolean,
        val chave: String = "",
        val tipo: String = "",
        val diasRestantes: Int = -1,
        val expiraEm: String = "",
        val erro: String = ""
    )

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

            if (json.optBoolean("sucesso", false)) {
                val licenca = json.optJSONObject("licenca") ?: JSONObject()
                Result.success(
                    ValidacaoResult(
                        sucesso = true,
                        chave = licenca.optString("chave"),
                        tipo = licenca.optString("tipo"),
                        diasRestantes = licenca.optInt("dias_restantes", -1),
                        expiraEm = licenca.optString("expira_em")
                    )
                )
            } else {
                Result.success(
                    ValidacaoResult(
                        sucesso = false,
                        erro = json.optString("erro", "Chave inválida ou já vinculada a outro dispositivo.")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

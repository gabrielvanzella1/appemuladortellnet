package com.logisticapp.scantemonitor

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** API do painel ScanTE (mesma do app ScanTE Telnet). Chamadas bloqueantes: usar fora da main thread. */
object Api {

    private const val BASE_URL = "https://scante.com.br/scante-admin/public"
    // DEVE ser idêntico ao API_SECRET do config.php em produção (o mesmo do ScanTE Telnet)
    private const val API_SECRET = "eab28be7c8536e7f5979e5a46b5ec65ba34fdf891d23c5f772e65a4d07057faf"
    private const val TIMEOUT_MS = 15_000

    data class Validacao(val valida: Boolean, val mensagem: String)

    /** Ativa a licença neste coletor (vincula a chave ao dispositivo). */
    fun validarChave(chave: String, deviceId: String, deviceNome: String): Validacao {
        val resp = postJson("/api/licenca/validar", JSONObject()
            .put("chave", chave.trim().uppercase()).put("device_id", deviceId).put("device_nome", deviceNome))
            ?: return Validacao(false, "Resposta inválida do servidor.")
        return Validacao(
            resp.optBoolean("valida", false),
            resp.optString("mensagem", resp.optString("erro", "Chave inválida ou já vinculada a outro coletor."))
        )
    }

    /** Registra o coletor no painel. Retorna se a empresa usa a cerca/rastreamento. */
    fun ping(deviceId: String, deviceNome: String, chave: String?): Boolean {
        val body = JSONObject().put("device_id", deviceId).put("device_nome", deviceNome)
            .put("app_version", "Monitor ${BuildConfig.VERSION_NAME}")
        if (chave != null) body.put("license_key", chave)
        return postJson("/api/dispositivo/ping", body)?.optBoolean("rastreamento", false) ?: false
    }

    /** Envia um lote de sinais. Retorna se o rastreamento continua ativo para a empresa. */
    fun enviarSinais(deviceId: String, sinais: JSONArray): Boolean {
        val resp = postJson("/api/dispositivo/sinal", JSONObject().put("device_id", deviceId).put("sinais", sinais))
            ?: throw IllegalStateException("Resposta inválida do servidor")
        if (!resp.optBoolean("ok", false)) throw IllegalStateException(resp.optString("erro", "Erro no servidor"))
        return resp.optBoolean("rastreamento", false)
    }

    private fun postJson(path: String, body: JSONObject): JSONObject? {
        val conn = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $API_SECRET")
            setRequestProperty("X-API-KEY", API_SECRET)  // fallback: Apache remove o Authorization em hosts compartilhados
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
        }
        try {
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } } ?: return null
            return try { JSONObject(text) } catch (e: Exception) { null }
        } finally {
            conn.disconnect()
        }
    }
}

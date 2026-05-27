package com.logisticapp.emuladortelnet.license

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MercadoPagoManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    /**
     * Cria uma preferência de pagamento no Mercado Pago.
     * Retorna a URL do checkout (init_point) para abrir no browser.
     * O external_reference é o ANDROID_ID para vincular a licença ao dispositivo.
     */
    suspend fun createPreference(deviceId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("items", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("title", MercadoPagoConfig.ITEM_TITLE)
                        put("quantity", 1)
                        put("unit_price", MercadoPagoConfig.LICENSE_PRICE)
                        put("currency_id", MercadoPagoConfig.CURRENCY_ID)
                    })
                })
                put("back_urls", JSONObject().apply {
                    put("success", MercadoPagoConfig.SUCCESS_URL)
                    put("failure", MercadoPagoConfig.FAILURE_URL)
                    put("pending", MercadoPagoConfig.PENDING_URL)
                })
                put("auto_return", "approved")
                put("external_reference", deviceId)
            }

            val request = Request.Builder()
                .url("https://api.mercadopago.com/checkout/preferences")
                .addHeader("Authorization", "Bearer ${MercadoPagoConfig.ACCESS_TOKEN}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Timber.e("MP createPreference erro ${response.code}: $body")
                return@withContext Result.failure(Exception("Erro ao criar pagamento (${response.code})"))
            }

            val json = JSONObject(body)
            val initPoint = json.getString("init_point")
            Timber.d("Preferência criada: $initPoint")
            Result.success(initPoint)

        } catch (e: Exception) {
            Timber.e(e, "Exceção ao criar preferência MP")
            Result.failure(e)
        }
    }

    /**
     * Consulta o status de um pagamento pelo payment_id recebido no deep link.
     * Retorna "approved", "rejected", "pending", etc.
     */
    suspend fun getPaymentStatus(paymentId: String): Result<PaymentInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.mercadopago.com/v1/payments/$paymentId")
                .addHeader("Authorization", "Bearer ${MercadoPagoConfig.ACCESS_TOKEN}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Timber.e("MP getPaymentStatus erro ${response.code}: $body")
                return@withContext Result.failure(Exception("Erro ao verificar pagamento (${response.code})"))
            }

            val json = JSONObject(body)
            val info = PaymentInfo(
                id = json.getString("id"),
                status = json.getString("status"),
                externalReference = json.optString("external_reference", ""),
                orderId = json.optString("order", "")
            )
            Timber.d("Status pagamento $paymentId: ${info.status}")
            Result.success(info)

        } catch (e: Exception) {
            Timber.e(e, "Exceção ao verificar pagamento MP")
            Result.failure(e)
        }
    }

    data class PaymentInfo(
        val id: String,
        val status: String,
        val externalReference: String,
        val orderId: String
    )
}

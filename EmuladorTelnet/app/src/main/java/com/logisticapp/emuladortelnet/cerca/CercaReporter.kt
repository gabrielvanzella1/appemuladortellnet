package com.logisticapp.emuladortelnet.cerca

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.logisticapp.emuladortelnet.license.LicenseApiService
import com.logisticapp.emuladortelnet.license.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Cerca digital por Wi-Fi: enquanto o app está vivo, envia a cada minuto o roteador (BSSID)
 * em que o coletor está conectado, a rede (SSID), o sinal e a bateria. Não usa GPS.
 *
 * Só roda quando o servidor diz que a empresa do coletor usa a cerca (resposta do ping).
 * Sem rede, os sinais ficam numa fila e sobem depois. Qualquer erro é só registrado —
 * a cerca nunca pode atrapalhar o uso do terminal.
 */
object CercaReporter {

    private const val PREFS = "cerca_digital"
    private const val KEY_ATIVA = "ativa"
    private const val INTERVALO_MS = 60_000L
    private const val MAX_FILA = 200            // ~3 h de sinais sem rede

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val fila = ArrayDeque<JSONObject>()

    fun estaAtiva(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ATIVA, false)

    /** Chamado com a resposta do servidor (ping ou envio): liga ou desliga o envio. */
    fun definirAtiva(ctx: Context, ativa: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ATIVA, ativa).apply()
        if (ativa) iniciar(ctx) else parar()
    }

    @Synchronized
    fun iniciar(ctx: Context) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        Timber.d("Cerca digital: envio iniciado")
        job = scope.launch {
            while (isActive) {
                try {
                    coletarEEnviar(app)
                } catch (e: Exception) {
                    Timber.w(e, "Cerca digital: falha ao coletar/enviar")
                }
                delay(INTERVALO_MS)
            }
        }
    }

    @Synchronized
    fun parar() {
        job?.cancel()
        job = null
        synchronized(fila) { fila.clear() }
    }

    fun temPermissao(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private suspend fun coletarEEnviar(ctx: Context) {
        val lote = synchronized(fila) {
            fila.addLast(lerSinal(ctx))
            while (fila.size > MAX_FILA) fila.removeFirst()
            JSONArray(fila.toList())
        }
        val deviceId = LicenseManager(ctx).getDeviceId()
        val resultado = LicenseApiService().enviarSinais(deviceId, lote)
        if (resultado.isSuccess) {
            synchronized(fila) { repeat(minOf(lote.length(), fila.size)) { fila.removeFirst() } }
            if (resultado.getOrNull() == false) {
                Timber.d("Cerca digital: desativada no servidor, parando")
                definirAtiva(ctx, false)
            }
        } else {
            Timber.w("Cerca digital: envio falhou, ${lote.length()} sinal(is) na fila")
        }
    }

    /** Lê roteador Wi-Fi (se houver permissão) e bateria. */
    private fun lerSinal(ctx: Context): JSONObject {
        val sinal = JSONObject().put("capturado_em", System.currentTimeMillis() / 1000)

        if (temPermissao(ctx)) {
            try {
                val wifi = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val info = wifi.connectionInfo
                if (info != null && info.networkId != -1 && info.bssid != null) {
                    sinal.put("bssid", info.bssid)
                    sinal.put("ssid", info.ssid)
                    sinal.put("rssi", info.rssi)
                }
            } catch (e: Exception) {
                Timber.w(e, "Cerca digital: não foi possível ler o Wi-Fi")
            }
        }

        ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { bat ->
            val nivel = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val escala = bat.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (nivel >= 0 && escala > 0) sinal.put("bateria", nivel * 100 / escala)
            sinal.put("carregando", bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
        }
        return sinal
    }
}

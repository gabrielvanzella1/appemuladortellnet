package com.logisticapp.scantemonitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Lê um "sinal" do coletor: roteador Wi-Fi conectado (zona no CD), localização (GPS se o
 * aparelho tiver; senão localização por rede Wi-Fi + Google, que funciona sem GPS) e bateria.
 * Nunca lança exceção: o que não der para ler fica de fora do sinal.
 */
class Coletor(private val ctx: Context) {

    companion object {
        private const val CICLOS_LOCALIZACAO_NOVA = 5          // leitura nova a cada 5 envios (~5 min)
        private const val LOCALIZACAO_VALIDA_MS = 10 * 60_000L
    }

    private var ciclo = 0

    fun temLocalizacao(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun ler(): JSONObject {
        val sinal = JSONObject().put("capturado_em", System.currentTimeMillis() / 1000)
        if (temLocalizacao()) {
            lerWifi(sinal)
            try {
                lerLocalizacao()?.let { loc ->
                    sinal.put("lat", loc.latitude)
                    sinal.put("lng", loc.longitude)
                    if (loc.hasAccuracy()) sinal.put("precisao", loc.accuracy.toInt())
                    sinal.put("fonte", loc.provider ?: "")
                }
            } catch (e: Exception) {
                Timber.w(e, "Monitor: falha ao ler localização")
            }
        }
        lerBateria(sinal)
        ciclo++
        return sinal
    }

    private fun lerWifi(sinal: JSONObject) {
        try {
            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wifi.connectionInfo
            if (info != null && info.networkId != -1 && info.bssid != null) {
                sinal.put("bssid", info.bssid)
                sinal.put("ssid", info.ssid)
                sinal.put("rssi", info.rssi)
            }
        } catch (e: Exception) {
            Timber.w(e, "Monitor: falha ao ler o Wi-Fi")
        }
    }

    private fun lerBateria(sinal: JSONObject) {
        val bat = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val nivel = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val escala = bat.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (nivel >= 0 && escala > 0) sinal.put("bateria", nivel * 100 / escala)
        sinal.put("carregando", bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
    }

    /**
     * GPS primeiro, se o coletor tiver (até 15 s — dentro do galpão costuma não pegar);
     * depois "fused" (Android 12+) ou rede (até 10 s). Nos ciclos intermediários usa a
     * última localização conhecida, se for recente.
     */
    @SuppressLint("MissingPermission") // conferida em temLocalizacao()
    private suspend fun lerLocalizacao(): Location? {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) return null
        val ativos = lm.getProviders(true)

        val ultima = ativos.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        val recente = ultima != null && System.currentTimeMillis() - ultima.time < LOCALIZACAO_VALIDA_MS
        if (recente && ciclo % CICLOS_LOCALIZACAO_NOVA != 0) return ultima

        if (LocationManager.GPS_PROVIDER in ativos) {
            lerAgora(lm, LocationManager.GPS_PROVIDER, 15_000)?.let { return it }
        }
        val rede = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in ativos -> LocationManager.FUSED_PROVIDER
            LocationManager.NETWORK_PROVIDER in ativos -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        rede?.let { p -> lerAgora(lm, p, 10_000)?.let { return it } }
        return ultima?.takeIf { recente }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lerAgora(lm: LocationManager, provedor: String, timeoutMs: Long): Location? =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(lm, provedor, cancel, ContextCompat.getMainExecutor(ctx)) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Monitor: provedor $provedor falhou")
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
}

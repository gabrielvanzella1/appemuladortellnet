package com.logisticapp.scantemonitor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

/** Estado persistido do Monitor (ativação + último envio, para a tela de status). */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("scante_monitor", Context.MODE_PRIVATE)
    private val appCtx = ctx.applicationContext

    var chave: String?
        get() = sp.getString("chave", null)
        set(v) = sp.edit().putString("chave", v).apply()

    val ativado: Boolean get() = !chave.isNullOrBlank()

    var rastreamentoAtivo: Boolean
        get() = sp.getBoolean("rastreamento", false)
        set(v) = sp.edit().putBoolean("rastreamento", v).apply()

    /** JSON do último sinal coletado (roteador, localização, bateria). */
    var ultimoSinal: String?
        get() = sp.getString("ultimo_sinal", null)
        set(v) = sp.edit().putString("ultimo_sinal", v).apply()

    var ultimoEnvioOk: Long
        get() = sp.getLong("ultimo_envio_ok", 0L)
        set(v) = sp.edit().putLong("ultimo_envio_ok", v).apply()

    var ultimoErro: String?
        get() = sp.getString("ultimo_erro", null)
        set(v) = sp.edit().putString("ultimo_erro", v).apply()

    var naFila: Int
        get() = sp.getInt("na_fila", 0)
        set(v) = sp.edit().putInt("na_fila", v).apply()

    /** Senha local (trava o app após ativado). null = ainda usa a senha padrão [Pin.PADRAO]. */
    private var pinHash: String?
        get() = sp.getString("pin_hash", null)
        set(v) = sp.edit().putString("pin_hash", v).apply()

    fun conferirPin(digitado: String): Boolean = Pin.hash(digitado) == (pinHash ?: Pin.hash(Pin.PADRAO))
    fun definirPin(novo: String) { pinHash = Pin.hash(novo) }

    /** Mesmo ID que o ScanTE Telnet usa (ANDROID_ID; igual nos dois apps por usarem a mesma assinatura). */
    @SuppressLint("HardwareIds")
    fun deviceId(): String =
        Settings.Secure.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID) ?: "MONITOR_${Build.MODEL}"

    fun deviceNome(): String = "${Build.MANUFACTURER} ${Build.MODEL}"
}

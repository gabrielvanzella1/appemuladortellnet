package com.logisticapp.scantemonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Serviço sempre ligado (primeiro plano, com notificação fixa): a cada minuto lê o sinal do
 * coletor e envia ao painel. Sem rede, guarda até 200 sinais e envia depois. Volta sozinho
 * após reinício do coletor (BootReceiver) e se o sistema o encerrar (START_STICKY).
 */
class MonitorService : Service() {

    companion object {
        private const val CANAL = "monitor"
        private const val NOTIF_ID = 1
        private const val INTERVALO_MS = 60_000L
        private const val INTERVALO_SEM_RASTREAMENTO_MS = 5 * 60_000L
        private const val PING_A_CADA = 30          // re-registra no painel a cada ~30 min
        private const val MAX_FILA = 200
        const val ACAO_ENVIAR_AGORA = "enviar_agora"

        fun iniciar(ctx: Context, acao: String? = null) {
            if (!Prefs(ctx).ativado) return
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java).setAction(acao))
            } catch (e: Exception) {
                Timber.w(e, "Monitor: não foi possível iniciar o serviço")
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val fila = ArrayDeque<JSONObject>()
    private lateinit var prefs: Prefs
    private lateinit var coletor: Coletor

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        coletor = Coletor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.ativado) { stopSelf(); return START_NOT_STICKY }
        entrarEmPrimeiroPlano()
        if (job?.isActive != true) {
            job = scope.launch { loop() }
        } else if (intent?.action == ACAO_ENVIAR_AGORA) {
            scope.launch { rodada() }
        }
        return START_STICKY
    }

    private fun entrarEmPrimeiroPlano() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CANAL, "Monitoramento", NotificationManager.IMPORTANCE_LOW))
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notif: Notification = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle(getString(R.string.app_name) + " ativo")
            .setContentText("Coletor monitorado: localização e bateria a cada minuto.")
            .setOngoing(true)
            .setContentIntent(abrir)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Com permissão de localização, serviço do tipo "localização" (lê roteador/posição com a tela apagada)
                val tipo = if (coletor.temLocalizacao()) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                           else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                startForeground(NOTIF_ID, notif, tipo)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            Timber.w(e, "Monitor: startForeground falhou")
        }
    }

    private suspend fun loop() {
        var voltas = 0
        while (scope.isActive) {
            if (voltas % PING_A_CADA == 0) {
                try {
                    prefs.rastreamentoAtivo = Api.ping(prefs.deviceId(), prefs.deviceNome(), prefs.chave)
                } catch (e: Exception) {
                    Timber.w(e, "Monitor: ping falhou")
                }
            }
            rodada()
            voltas++
            delay(if (prefs.rastreamentoAtivo) INTERVALO_MS else INTERVALO_SEM_RASTREAMENTO_MS)
        }
    }

    /** Lê um sinal e envia a fila. */
    private suspend fun rodada() {
        try {
            val sinal = coletor.ler()
            prefs.ultimoSinal = sinal.toString()
            val lote = synchronized(fila) {
                fila.addLast(sinal)
                while (fila.size > MAX_FILA) fila.removeFirst()
                JSONArray(fila.toList())
            }
            val ativo = Api.enviarSinais(prefs.deviceId(), lote)
            synchronized(fila) { repeat(minOf(lote.length(), fila.size)) { fila.removeFirst() } }
            prefs.rastreamentoAtivo = ativo
            prefs.ultimoEnvioOk = System.currentTimeMillis()
            prefs.ultimoErro = null
        } catch (e: Exception) {
            prefs.ultimoErro = e.message ?: e.javaClass.simpleName
            Timber.w(e, "Monitor: envio falhou")
        } finally {
            prefs.naFila = synchronized(fila) { fila.size }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

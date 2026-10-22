package com.logisticapp.emuladortelnet

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.terminal.ScreenRecorder
import timber.log.Timber
import java.io.File

/**
 * Configurações → Gravação de telas.
 * Inicia, pausa/retoma e para a gravação das telas do host (para montar a futura "tela gráfica").
 * A gravação é global (vale para a sessão que estiver aberta) e se salva sozinha a cada poucos passos.
 */
class GravacaoActivity : AppCompatActivity() {

    private val rec get() = ScreenRecorder.instance
    private val ui = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { atualizar(); ui.postDelayed(this, 1000) }
    }

    private lateinit var txtStatus: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnPausar: Button
    private lateinit var btnParar: Button
    private lateinit var chkDownloads: android.widget.CheckBox
    private val prefs by lazy { getSharedPreferences("gravacao_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.get(this).applyOrientation(this)
        setContentView(R.layout.activity_gravacao)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        txtStatus = findViewById(R.id.txt_status)
        btnIniciar = findViewById(R.id.btn_iniciar)
        btnPausar = findViewById(R.id.btn_pausar)
        btnParar = findViewById(R.id.btn_parar)

        chkDownloads = findViewById(R.id.chk_downloads)
        chkDownloads.isChecked = prefs.getBoolean("copiar_downloads", true)
        chkDownloads.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("copiar_downloads", v).apply() }

        btnIniciar.setOnClickListener { iniciar() }
        btnPausar.setOnClickListener {
            if (rec.paused) { rec.resume(); rec.captureNow() } else rec.pause()
            atualizar()
        }
        btnParar.setOnClickListener { pararEEnviar() }
    }

    override fun onResume() { super.onResume(); ui.post(refresh) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(refresh) }

    private fun pasta() = File(getExternalFilesDir(null) ?: filesDir, "gravacoes")

    /** Apaga da pasta Downloads as cópias de gravação (criadas pelo app) com mais de 7 dias. */
    private fun apagarCopiasAntigas() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val limite = System.currentTimeMillis() / 1000 - 7L * 24 * 3600
            contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.MediaColumns.DATE_ADDED} < ?",
                arrayOf("gravacao-%", limite.toString())
            )
        }
    }

    private fun iniciar() {
        apagarCopiasAntigas()
        rec.appVersion = BuildConfig.VERSION_NAME
        rec.deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        rec.start(pasta())
        rec.captureNow()       // se já há uma sessão aberta, pega a tela atual
        Toast.makeText(
            this,
            "Gravando. Volte para a sessão e faça o trabalho normal. Use Pausar aqui se precisar.",
            Toast.LENGTH_LONG
        ).show()
        atualizar()
    }

    private fun atualizar() {
        val ativo = rec.active
        txtStatus.text = when {
            !ativo -> "Parada.\n\nToque em Iniciar, volte para a sessão (conecte no host) e faça o trabalho normal."
            rec.paused -> "PAUSADA — ${rec.screens} telas gravadas.\n\nToque em Retomar para continuar."
            else -> "● GRAVANDO — ${rec.screens} telas, ${rec.eventCount} eventos.\n\nVolte para a sessão e trabalhe normalmente."
        }
        txtStatus.setTextColor(
            when { !ativo -> 0xFF555555.toInt(); rec.paused -> 0xFFB26A00.toInt(); else -> 0xFFD32F2F.toInt() }
        )
        btnIniciar.isEnabled = !ativo
        btnPausar.isEnabled = ativo
        btnPausar.text = if (rec.paused) "Retomar" else "Pausar"
        btnParar.isEnabled = ativo
    }

    private fun pararEEnviar() {
        val telas = rec.screens
        val arquivo = runCatching { rec.stop(pasta()) }.getOrNull()
        atualizar()
        if (arquivo == null) {
            Toast.makeText(this, "Nada foi gravado.", Toast.LENGTH_LONG).show()
            return
        }
        val emDownloads = chkDownloads.isChecked && copiarParaDownloads(arquivo)
        Toast.makeText(
            this,
            "Gravação salva: $telas telas." + if (emDownloads) " Cópia na pasta Downloads: ${arquivo.name}" else "",
            Toast.LENGTH_LONG
        ).show()
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Gravação de telas ScanTE")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Enviar gravação"))
        }.onFailure { Timber.w(it, "Falha ao abrir o compartilhamento da gravação") }
    }

    /** Cópia na pasta Downloads (Android 10+): qualquer gerenciador de arquivos acha. */
    private fun copiarParaDownloads(arquivo: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, arquivo.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            contentResolver.openOutputStream(uri)?.use { out -> arquivo.inputStream().use { it.copyTo(out) } }
            true
        }.getOrDefault(false)
    }
}

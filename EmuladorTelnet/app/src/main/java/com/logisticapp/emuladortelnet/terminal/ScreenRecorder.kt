package com.logisticapp.emuladortelnet.terminal

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gravador de telas do host (captura para a futura "tela gráfica").
 *
 * Liga/desliga pelo menu. Enquanto grava, guarda uma linha do tempo com:
 *  - as TELAS do host (texto da grade 24x80, cursor, campos de preenchimento e trechos
 *    destacados/coloridos) — só quando a tela muda e fica parada ~0,4 s;
 *  - as TECLAS enviadas, SEM o conteúdo digitado: texto vira só "digitou: n caracteres";
 *    teclas especiais viram nomes (ENTER, TAB, F1...);
 *  ATENÇÃO: as TELAS mostram o que o HOST exibiu, então valores digitados que o host ecoa
 *  (usuário, códigos, quantidades) APARECEM nas telas. Linhas com rótulo de senha
 *  ("senha", "password", "pin"...) têm o valor mascarado como proteção extra, mas a
 *  máscara é só um melhor-esforço. Arquivos com mais de 7 dias são apagados ao iniciar.
 *  - os ALARMES (BEL) que o host mandou — mostra se o sistema avisa erro com bipe.
 *
 * O arquivo (JSON) é entregue pelo usuário; nada é enviado sozinho.
 */
class ScreenRecorder {

    /** Uma tela capturada. */
    class Snapshot(
        val rows: List<String>,
        val cursorRow: Int,
        val cursorCol: Int,
        val fields: List<IntArray>,   // [linha, coluna, tamanho]  — campos de preenchimento
        val styled: List<JSONObject>  // trechos com cor/negrito/sublinhado/pisca
    ) {
        fun fingerprint(): String =
            rows.joinToString("\n") + "|" + cursorRow + "," + cursorCol + "|" + fields.size
    }

    @Volatile var active = false
        private set
    @Volatile var paused = false
        private set

    /** Foto da tela da sessão que está em primeiro plano (registrada pela MainActivity). */
    @Volatile var provider: (() -> Snapshot)? = null

    private val events = JSONArray()
    private var startedAtWall = 0L
    private var startedAtMono = 0L
    private var lastFingerprint = ""
    private var pendingText = 0          // caracteres digitados ainda não "fechados" por uma tecla especial
    private var screensCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var pendingCapture: Runnable? = null

    var host: String = ""
    var port: Int = 0
    var appVersion: String = ""
    var deviceModel: String = ""

    private var dirOut: File? = null
    val screens: Int get() = screensCount
    val eventCount: Int get() = events.length()

    private var outFile: File? = null
    private var sinceAutosave = 0

    /** Grava em [dir] (o arquivo é regravado a cada 15 eventos: nada se perde se o app fechar). */
    fun start(dir: File) {
        synchronized(this) {
            if (active) return
            active = true
            paused = false
            host = ""; port = 0           // o host é "amarrado" na primeira sessão que abrir (ver attachSession)
            outFile = null
            sinceAutosave = 0
            dirOut = dir
            runCatching {
                val limite = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                dir.listFiles { f -> f.name.startsWith("gravacao-") && f.lastModified() < limite }?.forEach { it.delete() }
            }
            startedAtWall = System.currentTimeMillis()
            startedAtMono = SystemClock.elapsedRealtime()
            lastFingerprint = ""
            pendingText = 0
            screensCount = 0
            while (events.length() > 0) events.remove(0)
        }
    }

    /** Para e devolve o arquivo gravado em [dir], ou null se não havia nada para gravar. */
    fun stop(dir: File): File? {
        synchronized(this) {
            if (!active) return null
            active = false
            paused = false
            pendingCapture?.let { handler.removeCallbacks(it) }
            flushText()
        }
        return if (screensCount == 0 && events.length() == 0) null else write(dir)
    }

    /** Regrava o arquivo parcial (chamado de dentro do lock). Falha de disco não derruba a gravação. */
    private fun autosave() {
        val dir = dirOut ?: return
        runCatching { write(dir) }
    }

    // ---- entradas (chamadas pelo ViewModel) ----

    /** Chamado a cada dado recebido do host: agenda a captura quando a tela "assentar". */
    fun onScreenData(capture: () -> Snapshot) {
        if (!active || paused) return
        pendingCapture?.let { handler.removeCallbacks(it) }
        val r = Runnable { synchronized(this) { if (active && !paused) addScreen(capture()) } }
        pendingCapture = r
        handler.postDelayed(r, 400)
    }

    fun onKey(bytes: ByteArray) {
        if (!active || paused) return
        synchronized(this) {
            val name = specialKeyName(bytes)
            if (name == null) {
                // texto comum: só conta os caracteres, nunca guarda o conteúdo
                pendingText += bytes.count { (it.toInt() and 0xFF) >= 32 && it.toInt() != 127 }
            } else {
                flushText()
                add(JSONObject().put("tipo", "tecla").put("tecla", name))
            }
        }
    }

    fun onBell() {
        if (!active || paused) return
        synchronized(this) { flushText(); add(JSONObject().put("tipo", "alarme_host")) }
    }

    /**
     * Registra a sessão em primeiro plano. A gravação fica amarrada ao primeiro host aberto depois de
     * iniciar; se outra sessão (host/porta diferente) for aberta, PAUSA sozinha e devolve true para avisar.
     */
    fun attachSession(h: String, p: Int): Boolean {
        synchronized(this) {
            if (!active || host.isEmpty()) { host = h; port = p; return false }
            if (host == h && port == p) return false
            if (!paused) { pause(); return true }
            return false
        }
    }

    fun pause() {
        synchronized(this) {
            if (!active || paused) return
            pendingCapture?.let { handler.removeCallbacks(it) }
            flushText()
            add(JSONObject().put("tipo", "pausa"))
            paused = true
            autosave()
        }
    }

    fun resume() {
        synchronized(this) {
            if (!active || !paused) return
            paused = false
            lastFingerprint = ""            // a mesma tela volta a ser capturada após a pausa
            add(JSONObject().put("tipo", "retomada"))
        }
    }

    /** Captura a tela atual agora (ao iniciar/retomar e ao voltar para a sessão). */
    fun captureNow() {
        val p = provider ?: return
        synchronized(this) { if (active && !paused) addScreen(p()) }
    }

    // ---- internos ----

    /** Mantém o rótulo ("Senha:") e troca o que vem depois por '*' (melhor-esforço, protege host que ecoa a senha). */
    private fun mascarar(linha: String): String {
        val m = SENSIVEL.find(linha) ?: return linha
        val dois = linha.indexOf(':', m.range.first)
        val corte = if (dois >= 0) dois + 1 else m.range.last + 1
        return linha.substring(0, corte) + linha.substring(corte).map { if (it == ' ') it else '*' }.joinToString("")
    }

    private fun protegido(s: Snapshot): Snapshot {
        val rows = s.rows.map(::mascarar)
        val styled = s.styled.map { o ->
            val r = o.optInt("linha", -1)
            if (r in rows.indices && rows[r] != s.rows[r]) JSONObject(o.toString()).put("texto", "*".repeat(o.optInt("tamanho", 0))) else o
        }
        return Snapshot(rows, s.cursorRow, s.cursorCol, s.fields, styled)
    }

    private fun addScreen(raw: Snapshot) {
        val s = protegido(raw)
        val fp = s.fingerprint()
        if (fp == lastFingerprint) return
        lastFingerprint = fp
        flushText()
        screensCount++
        add(JSONObject().apply {
            put("tipo", "tela")
            put("n", screensCount)
            put("linhas", JSONArray(s.rows))
            put("cursor", JSONArray(listOf(s.cursorRow, s.cursorCol)))
            put("campos", JSONArray().also { a -> s.fields.forEach { a.put(JSONArray(it.toList())) } })
            put("destaques", JSONArray().also { a -> s.styled.forEach { a.put(it) } })
        })
    }

    private fun flushText() {
        if (pendingText > 0) {
            add(JSONObject().put("tipo", "digitou").put("caracteres", pendingText))
            pendingText = 0
        }
    }

    private fun add(o: JSONObject) {
        if (events.length() >= MAX_EVENTS) return
        o.put("t_ms", SystemClock.elapsedRealtime() - startedAtMono)
        events.put(o)
        if (++sinceAutosave >= 15) { sinceAutosave = 0; autosave() }
    }

    private fun write(dir: File): File {
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(startedAtWall))
        val safeHost = host.replace(Regex("[^A-Za-z0-9.-]"), "_").ifEmpty { "host" }
        val file = outFile ?: File(dir, "gravacao-$safeHost-$stamp.json").also { outFile = it }
        val root = JSONObject().apply {
            put("formato", "scante-gravacao-telas")
            put("versao_formato", 1)
            put("app_versao", appVersion)
            put("aparelho", deviceModel)
            put("host", host)
            put("porta", port)
            put("iniciou_em", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(startedAtWall)))
            put("duracao_ms", SystemClock.elapsedRealtime() - startedAtMono)
            put("grade", JSONArray(listOf(24, 80)))
            put("telas", screensCount)
            put("aviso", "Teclas digitadas NAO sao gravadas (so a quantidade de caracteres). " +
                "As telas mostram o que o host exibiu; revise antes de compartilhar.")
            put("eventos", events)
        }
        file.writeText(root.toString(1), Charsets.UTF_8)
        return file
    }

    companion object {
        /** Um gravador único no app: as telas de Configurações e a sessão falam com o mesmo. */
        val instance = ScreenRecorder()

        private const val MAX_EVENTS = 4000
        private val SENSIVEL = Regex("""(?i)(senha|password|passwd|pwd|\bpin\b)""")

        /** Nome da tecla especial, ou null se for texto comum. */
        fun specialKeyName(b: ByteArray): String? {
            if (b.isEmpty()) return null
            val s = String(b, Charsets.ISO_8859_1)
            return when (s) {
                "\r", "\r\n", "\n" -> "ENTER"
                "\t" -> "TAB"
                "\b", "\u007f" -> "BACKSPACE"
                "\u001b" -> "ESC"
                "\u001bOP" -> "F1"; "\u001bOQ" -> "F2"; "\u001bOR" -> "F3"; "\u001bOS" -> "F4"
                "\u001b[15~" -> "F5"; "\u001b[17~" -> "F6"; "\u001b[18~" -> "F7"; "\u001b[19~" -> "F8"
                "\u001b[20~" -> "F9"; "\u001b[21~" -> "F10"; "\u001b[23~" -> "F11"; "\u001b[24~" -> "F12"
                "\u001b[A", "\u001bOA" -> "SETA_CIMA"
                "\u001b[B", "\u001bOB" -> "SETA_BAIXO"
                "\u001b[C", "\u001bOC" -> "SETA_DIREITA"
                "\u001b[D", "\u001bOD" -> "SETA_ESQUERDA"
                "\u001b[5~" -> "PGUP"; "\u001b[6~" -> "PGDN"
                "\u001b[H", "\u001b[1~" -> "HOME"; "\u001b[F", "\u001b[4~" -> "END"
                "\u001b[2~" -> "INSERT"; "\u001b[3~" -> "DELETE"
                else -> when {
                    s.length == 1 && s[0].code in 1..26 -> "CTRL+" + ('A' + (s[0].code - 1))
                    s.startsWith("\u001b") -> "ESC_SEQ"   // sequência de escape desconhecida
                    else -> null
                }
            }
        }
    }
}

package com.logisticapp.scantemonitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.logisticapp.scantemonitor.databinding.ActivityMainBinding
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ativação (chave de licença ScanTE) e status do monitoramento neste coletor. */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private val jaPedido = mutableSetOf<String>()   // etapas de permissão já pedidas nesta abertura

    // ponytail: trava só ao recriar a Activity (reabrir o app pelo ícone); dentro da mesma
    // sessão não pede de novo. Se precisar travar também ao sair pra segundo plano, resetar
    // no onStop.
    private var desbloqueado = false

    private val atualizar = object : Runnable {
        override fun run() { mostrar(); handler.postDelayed(this, 5_000) }
    }

    private val pedirLocalizacao =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reiniciarServicoEContinuar() }
    private val pedirPermissao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { reiniciarServicoEContinuar() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        b.inputNome.setText(Build.MODEL)
        mascararChave()
        b.btnAtivar.setOnClickListener { ativar() }
        b.btnPermissoes.setOnClickListener { jaPedido.clear(); continuarPermissoes() }
        b.btnEnviar.setOnClickListener {
            MonitorService.iniciar(this, MonitorService.ACAO_ENVIAR_AGORA)
            b.txtStatus.postDelayed({ mostrar() }, 3_000)
        }
        b.btnTrocarSenha.setOnClickListener { trocarSenha() }
        b.btnDesativar.setOnClickListener { confirmarDesativacao() }
        b.txtBloqueado.setOnClickListener { pedirSenha() }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.ativado) MonitorService.iniciar(this)
        handler.post(atualizar)
        if (prefs.ativado) {
            if (desbloqueado) continuarPermissoes() else pedirSenha()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(atualizar)
        super.onPause()
    }

    // ---------------------------------------------------------------- ativação

    private fun ativar() {
        val chave = b.inputChave.text.toString().trim().uppercase()
        val nome = b.inputNome.text.toString().trim()
        if (chave.isEmpty()) return resultado(false, "Digite a chave de ativação.")
        if (nome.isEmpty()) return resultado(false, "Informe um nome para identificar o coletor.")

        b.btnAtivar.isEnabled = false
        resultado(true, "Validando...")
        Thread {
            val msg = try {
                val v = Api.validarChave(chave, prefs.deviceId(), nome)
                if (v.valida) {
                    prefs.chave = chave
                    prefs.rastreamentoAtivo = try { Api.ping(prefs.deviceId(), prefs.deviceNome(), chave) } catch (e: Exception) { false }
                    null
                } else v.mensagem
            } catch (e: Exception) {
                "Sem conexão com o servidor: ${e.message}"
            }
            runOnUiThread {
                b.btnAtivar.isEnabled = true
                if (msg == null) {
                    MonitorService.iniciar(this)
                    desbloqueado = true   // acabou de ativar nesta mesma sessão: já entra liberado
                    mostrar()
                    continuarPermissoes()
                } else resultado(false, msg)
            }
        }.start()
    }

    private fun resultado(ok: Boolean, msg: String) {
        b.txtResultado.visibility = View.VISIBLE
        b.txtResultado.text = msg
        b.txtResultado.setTextColor(if (ok) Color.parseColor("#00A651") else Color.parseColor("#B3261E"))
    }

    /** SCTE-XXXXXX-XXXXXX-XXXXXX: coloca os hífens sozinho (igual ao ScanTE). */
    private fun mascararChave() {
        var formatando = false
        b.inputChave.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bf: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (formatando || s == null) return
                formatando = true
                val raw = s.toString().uppercase().filter { it.isLetterOrDigit() }
                val grupos = mutableListOf<String>()
                var i = 0
                for (tam in intArrayOf(4, 6, 6, 6)) {
                    if (i >= raw.length) break
                    val fim = minOf(i + tam, raw.length)
                    grupos.add(raw.substring(i, fim)); i = fim
                }
                val fmt = grupos.joinToString("-")
                if (fmt != s.toString()) s.replace(0, s.length, fmt)
                formatando = false
            }
        })
    }

    // ---------------------------------------------------------------- senha do coletor

    /**
     * Pede a senha do coletor (não é a chave de licença) — trava as configurações e o botão
     * de desativar para o funcionário não conseguir mexer. Padrão de fábrica: [Pin.PADRAO].
     */
    private fun pedirSenha() {
        if (isFinishing) return
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Senha do coletor"
        }
        AlertDialog.Builder(this)
            .setTitle("Coletor monitorado")
            .setMessage("Este coletor é monitorado pela empresa. Só o responsável entra nas configurações.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Entrar") { _, _ ->
                if (prefs.conferirPin(input.text.toString().trim())) {
                    desbloqueado = true
                    mostrar()
                    continuarPermissoes()
                } else {
                    Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show()
                    pedirSenha()
                }
            }
            .setNegativeButton("Fechar") { _, _ -> finish() }
            .show()
    }

    private fun trocarSenha() {
        val novo = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Nova senha (4 a 8 dígitos)"
        }
        AlertDialog.Builder(this)
            .setTitle("Nova senha do coletor")
            .setView(novo)
            .setPositiveButton("Salvar") { _, _ ->
                val p = novo.text.toString().trim()
                if (p.length < 4) Toast.makeText(this, "Use pelo menos 4 dígitos.", Toast.LENGTH_SHORT).show()
                else { prefs.definirPin(p); Toast.makeText(this, "Senha alterada.", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarDesativacao() {
        AlertDialog.Builder(this)
            .setTitle("Desativar monitoramento")
            .setMessage("O coletor para de aparecer no painel da empresa. Para reativar, é preciso a chave de licença de novo.")
            .setPositiveButton("Desativar") { _, _ ->
                stopService(Intent(this, MonitorService::class.java))
                prefs.chave = null
                prefs.rastreamentoAtivo = false
                desbloqueado = false
                jaPedido.clear()
                mostrar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------------------------------------------------------------- permissões

    private fun tem(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    private fun localizacaoLigada() =
        LocationManagerCompat.isLocationEnabled(getSystemService(LOCATION_SERVICE) as LocationManager)
    private fun semRestricaoBateria() =
        (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

    private fun reiniciarServicoEContinuar() {
        MonitorService.iniciar(this)   // serviço passa a ser do tipo "localização"
        continuarPermissoes()
    }

    /** Pede, em ordem, o que falta. Cada etapa só é pedida uma vez por abertura. */
    @SuppressLint("BatteryLife")
    private fun continuarPermissoes() {
        mostrar()
        when {
            !tem(Manifest.permission.ACCESS_FINE_LOCATION) && jaPedido.add("loc") -> explicar(
                "Localização",
                "O ScanTE Monitor informa ao painel da empresa em qual roteador Wi-Fi o coletor está, " +
                "onde ele está (GPS quando houver, ou localização por rede) e a bateria.\n\n" +
                "O Android pede a permissão de localização para isso."
            ) { pedirLocalizacao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tem(Manifest.permission.ACCESS_FINE_LOCATION) &&
                !tem(Manifest.permission.ACCESS_BACKGROUND_LOCATION) && jaPedido.add("bg") -> explicar(
                "Permitir o tempo todo",
                "Para o monitoramento continuar com a tela apagada ou depois de reiniciar o coletor, " +
                "escolha \"Permitir o tempo todo\" na próxima tela."
            ) { pedirPermissao.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !tem(Manifest.permission.POST_NOTIFICATIONS) &&
                jaPedido.add("notif") -> pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)

            !localizacaoLigada() && jaPedido.add("gps") -> explicar(
                "Ligue a Localização",
                "Com a Localização do Android desligada o coletor não informa o roteador nem a posição."
            ) { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }

            !semRestricaoBateria() && jaPedido.add("bat") -> explicar(
                "Sem restrição de bateria",
                "Para o Android não pausar o monitoramento, permita que o ScanTE Monitor rode sem restrição de bateria."
            ) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }
    }

    private fun explicar(titulo: String, msg: String, acao: () -> Unit) {
        if (isFinishing) return
        AlertDialog.Builder(this).setTitle(titulo).setMessage(msg)
            .setPositiveButton("Continuar") { _, _ -> acao() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    // ---------------------------------------------------------------- status

    private fun mostrar() {
        val ativado = prefs.ativado
        b.containerAtivacao.visibility = if (ativado) View.GONE else View.VISIBLE
        b.txtBloqueado.visibility = if (ativado && !desbloqueado) View.VISIBLE else View.GONE
        b.containerStatus.visibility = if (ativado && desbloqueado) View.VISIBLE else View.GONE
        if (!ativado || !desbloqueado) return

        val ok = "✓"; val nao = "✗"
        val permissoes = buildString {
            appendLine((if (tem(Manifest.permission.ACCESS_FINE_LOCATION)) ok else nao) + "  Localização")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                appendLine((if (tem(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) ok else nao) + "  Localização o tempo todo")
            appendLine((if (localizacaoLigada()) ok else nao) + "  Localização do Android ligada")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                appendLine((if (tem(Manifest.permission.POST_NOTIFICATIONS)) ok else nao) + "  Notificações")
            append((if (semRestricaoBateria()) ok else nao) + "  Sem restrição de bateria")
        }

        val s = prefs.ultimoSinal?.let { runCatching { JSONObject(it) }.getOrNull() }
        val hora = SimpleDateFormat("dd/MM HH:mm:ss", Locale("pt", "BR"))
        val leitura = if (s == null) "Nenhuma leitura ainda." else buildString {
            appendLine("Leitura: " + hora.format(Date(s.optLong("capturado_em") * 1000)))
            appendLine(if (s.has("bssid")) "Wi-Fi: ${s.optString("ssid").trim('"')}  ·  ${s.optString("bssid")}  ·  ${s.optInt("rssi")} dBm"
                       else "Wi-Fi: não informado (sem Wi-Fi ou sem permissão)")
            appendLine(if (s.has("lat")) "Localização: %.5f, %.5f  ±%d m (%s)".format(Locale.US,
                           s.optDouble("lat"), s.optDouble("lng"), s.optInt("precisao"), s.optString("fonte"))
                       else "Localização: indisponível")
            append("Bateria: ${s.optInt("bateria")}%" + if (s.optBoolean("carregando")) " (carregando)" else "")
        }
        val envio = when {
            prefs.ultimoErro != null -> "⚠ Último envio falhou: ${prefs.ultimoErro}" + (if (prefs.naFila > 0) " · ${prefs.naFila} na fila" else "")
            prefs.ultimoEnvioOk > 0 -> "Último envio ao painel: " + hora.format(Date(prefs.ultimoEnvioOk))
            else -> "Aguardando o primeiro envio..."
        }
        val chave = prefs.chave.orEmpty()
        b.txtStatus.text = buildString {
            appendLine("Licença: ${chave.take(11)}••••••")
            appendLine(if (prefs.rastreamentoAtivo) "Painel: monitoramento ATIVO para a empresa"
                       else "Painel: monitoramento desativado para esta empresa (fale com a ScanTE)")
            appendLine()
            appendLine(permissoes)
            appendLine()
            appendLine(leitura)
            appendLine()
            append(envio)
        }
    }
}

package com.logisticapp.emuladortelnet

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.VtOptions

/**
 * Configurações > Emulação > VT Opções.
 * Parâmetros de comportamento do emulador VT. Salva ao sair.
 */
class VtOptionsActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: VtOptions

    private lateinit var ckEchoMode: CheckBox
    private lateinit var ckScrollMode: CheckBox
    private lateinit var valLineMode: TextView
    private lateinit var ckAddLf: CheckBox
    private lateinit var ckNoCol81: CheckBox
    private lateinit var valBackspace: TextView
    private lateinit var inAnswerback: EditText
    private lateinit var valVtAlias: TextView
    private lateinit var ckF5Putty: CheckBox
    private lateinit var ckSilenceAlarm: CheckBox
    private lateinit var valMaxAlarms: TextView
    private lateinit var ckIgnoreEscapes: CheckBox
    private lateinit var valAlarmSound: TextView

    private var alarmSoundUri: String = ""

    private val lineModeOptions = arrayOf("Desativado", "Local", "Remoto")
    private val backspaceOptions = arrayOf("BS", "DEL")
    private val vtAliasOptions = arrayOf("VT52", "VT100", "VT220", "VT320", "ANSI")
    private val maxAlarmsOptions = arrayOf("Max", "1", "2", "3", "5", "10", "25", "50")

    private val pickSystemSound = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) setAlarmSound(uri.toString())
    }

    private val pickFileSound = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // Alguns provedores não suportam permissão persistente — segue mesmo assim
        }
        setAlarmSound(uri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_vt_options)
        opts = settings.vtOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        bindViews()
        loadValues()
        setupInteractions()
    }

    private fun bindViews() {
        ckEchoMode      = findViewById(R.id.ck_echo_mode)
        ckScrollMode    = findViewById(R.id.ck_scroll_mode)
        valLineMode     = findViewById(R.id.val_line_mode)
        ckAddLf         = findViewById(R.id.ck_add_lf)
        ckNoCol81       = findViewById(R.id.ck_no_col81)
        valBackspace    = findViewById(R.id.val_backspace)
        inAnswerback    = findViewById(R.id.in_answerback)
        valVtAlias      = findViewById(R.id.val_vt_alias)
        ckF5Putty       = findViewById(R.id.ck_f5_putty)
        ckSilenceAlarm  = findViewById(R.id.ck_silence_alarm)
        valMaxAlarms    = findViewById(R.id.val_max_alarms)
        ckIgnoreEscapes = findViewById(R.id.ck_ignore_escapes)
        valAlarmSound   = findViewById(R.id.val_alarm_sound)
    }

    private fun loadValues() {
        ckEchoMode.isChecked      = opts.echoMode
        ckScrollMode.isChecked    = opts.scrollMode
        valLineMode.text          = opts.lineMode
        ckAddLf.isChecked         = opts.addLfToCr
        ckNoCol81.isChecked       = opts.noColumn81
        valBackspace.text         = opts.backspaceAction
        inAnswerback.setText(opts.answerbackString)
        valVtAlias.text           = opts.vtDaAlias
        ckF5Putty.isChecked       = opts.f5PuttySequence
        ckSilenceAlarm.isChecked  = opts.silenceHostAlarm
        valMaxAlarms.text         = opts.maxConsecutiveAlarms
        ckIgnoreEscapes.isChecked = opts.ignoreUnknownEscapes
        alarmSoundUri = opts.alarmSoundUri
        valAlarmSound.text = AlarmSoundPlayer.displayName(this, alarmSoundUri)
    }

    private fun setupInteractions() {
        // Checkboxes ativam clicando em qualquer lugar da linha
        findViewById<View>(R.id.row_echo_mode).setOnClickListener { ckEchoMode.toggle() }
        findViewById<View>(R.id.row_scroll_mode).setOnClickListener { ckScrollMode.toggle() }
        findViewById<View>(R.id.row_add_lf).setOnClickListener { ckAddLf.toggle() }
        findViewById<View>(R.id.row_no_col81).setOnClickListener { ckNoCol81.toggle() }
        findViewById<View>(R.id.row_f5_putty).setOnClickListener { ckF5Putty.toggle() }
        findViewById<View>(R.id.row_silence_alarm).setOnClickListener { ckSilenceAlarm.toggle() }
        findViewById<View>(R.id.row_ignore_escapes).setOnClickListener { ckIgnoreEscapes.toggle() }

        // Seletores com diálogo
        findViewById<View>(R.id.row_line_mode).setOnClickListener {
            pick("Modo de linha", lineModeOptions, valLineMode.text.toString()) {
                valLineMode.text = it
            }
        }
        findViewById<View>(R.id.row_backspace).setOnClickListener {
            pick("Ação da tecla Backspace", backspaceOptions, valBackspace.text.toString()) {
                valBackspace.text = it
            }
        }
        findViewById<View>(R.id.row_vt_alias).setOnClickListener {
            pick("VT DA Alias", vtAliasOptions, valVtAlias.text.toString()) {
                valVtAlias.text = it
            }
        }
        findViewById<View>(R.id.row_max_alarms).setOnClickListener {
            pick("Máximo de alarmes consecutivos", maxAlarmsOptions, valMaxAlarms.text.toString()) {
                valMaxAlarms.text = it
            }
        }
        findViewById<View>(R.id.row_alarm_sound).setOnClickListener { pickAlarmSound() }
        findViewById<View>(R.id.row_test_alarm_sound).setOnClickListener {
            AlarmSoundPlayer.play(this, alarmSoundUri)
        }
    }

    /** Escolher entre alertas prontos, toques do sistema ou importar um arquivo de som. */
    private fun pickAlarmSound() {
        val options = arrayOf("Sons de alerta (recomendado)", "Toques do sistema", "Importar arquivo de som")
        AlertDialog.Builder(this)
            .setTitle("Som do alarme do host")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickPresetSound()
                    1 -> {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                            val current = alarmSoundUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
                        }
                        pickSystemSound.launch(intent)
                    }
                    2 -> pickFileSound.launch(arrayOf("audio/*"))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setAlarmSound(uri: String) {
        alarmSoundUri = uri
        valAlarmSound.text = AlarmSoundPlayer.displayName(this, uri)
    }

    /** Lista os alertas prontos — toca uma prévia ao tocar em cada um, confirma com "Usar este". */
    private fun pickPresetSound() {
        val presets = AlarmSoundPlayer.Preset.values()
        val labels = presets.map { it.label }.toTypedArray()
        var chosen = presets.indexOfFirst { it.id == alarmSoundUri }.takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(this)
            .setTitle("Som do alarme do host")
            .setSingleChoiceItems(labels, chosen) { _, which ->
                chosen = which
                AlarmSoundPlayer.play(this, presets[which].id)
            }
            .setPositiveButton("Usar este") { _, _ -> setAlarmSound(presets[chosen].id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pick(title: String, options: Array<String>, current: String, onPick: (String) -> Unit) {
        val checked = options.indexOf(current).takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                onPick(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        settings.vtOptions = VtOptions(
            echoMode             = ckEchoMode.isChecked,
            scrollMode           = ckScrollMode.isChecked,
            lineMode             = valLineMode.text.toString(),
            addLfToCr            = ckAddLf.isChecked,
            noColumn81           = ckNoCol81.isChecked,
            backspaceAction      = valBackspace.text.toString(),
            answerbackString     = inAnswerback.text.toString(),
            vtDaAlias            = valVtAlias.text.toString(),
            f5PuttySequence      = ckF5Putty.isChecked,
            silenceHostAlarm     = ckSilenceAlarm.isChecked,
            maxConsecutiveAlarms = valMaxAlarms.text.toString(),
            ignoreUnknownEscapes = ckIgnoreEscapes.isChecked,
            alarmSoundUri        = alarmSoundUri
        )
    }
}

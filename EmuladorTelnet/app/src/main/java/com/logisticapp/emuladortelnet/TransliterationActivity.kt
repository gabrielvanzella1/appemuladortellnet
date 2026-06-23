package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.TransliterationOptions

/**
 * Configurações > Emulação > Transliteração.
 * Controla o charset e transliteração de caracteres entre o app e o host. Salva ao sair.
 */
class TransliterationActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: TransliterationOptions

    private lateinit var ckUtf8: CheckBox
    private lateinit var ckHost8bit: CheckBox
    private lateinit var ckLowercase: CheckBox
    private lateinit var valHostCharset: TextView
    private lateinit var valNationalTranslit: TextView
    private lateinit var ckSiso: CheckBox

    private val hostCharsetOptions = arrayOf(
        "Padrão (Latim 1)",
        "ISO 8859-2 (Europa Oriental)",
        "ISO 8859-5 (Cirílico)",
        "ISO 8859-7 (Grego)",
        "ISO 8859-9 (Turco)",
        "ISO 8859-15 (Latim 9 com €)",
        "Windows-1252",
        "Windows-1251 (Cirílico)",
        "IBM 850",
        "IBM 437 (Inglês EUA)"
    )

    private val nationalTranslitOptions = arrayOf(
        "Padrão (sem transliteração)",
        "Português (Brasil)",
        "Francês",
        "Alemão",
        "Espanhol",
        "Italiano",
        "Sueco / Finlandês",
        "Norueguês / Dinamarquês"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_transliteration)
        opts = settings.transliterationOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        bindViews()
        loadValues()
        setupInteractions()
    }

    private fun bindViews() {
        ckUtf8          = findViewById(R.id.ck_utf8)
        ckHost8bit      = findViewById(R.id.ck_host8bit)
        ckLowercase     = findViewById(R.id.ck_lowercase)
        valHostCharset  = findViewById(R.id.val_host_charset)
        valNationalTranslit = findViewById(R.id.val_national_translit)
        ckSiso          = findViewById(R.id.ck_siso)
    }

    private fun loadValues() {
        ckUtf8.isChecked      = opts.utf8Encoding
        ckHost8bit.isChecked  = opts.host8bit
        ckLowercase.isChecked = opts.allowLowercase
        valHostCharset.text   = opts.hostCharset
        valNationalTranslit.text = opts.nationalTranslit
        ckSiso.isChecked      = opts.useSiso
    }

    private fun setupInteractions() {
        // Checkboxes ativam clicando em qualquer lugar da linha
        findViewById<View>(R.id.row_utf8).setOnClickListener { ckUtf8.toggle() }
        findViewById<View>(R.id.row_host8bit).setOnClickListener { ckHost8bit.toggle() }
        findViewById<View>(R.id.row_lowercase).setOnClickListener { ckLowercase.toggle() }
        findViewById<View>(R.id.row_siso).setOnClickListener { ckSiso.toggle() }

        // Seletores com diálogo
        findViewById<View>(R.id.row_host_charset).setOnClickListener {
            pick("Conjunto de caracteres hospedeiros", hostCharsetOptions, valHostCharset.text.toString()) {
                valHostCharset.text = it
            }
        }
        findViewById<View>(R.id.row_national_translit).setOnClickListener {
            pick("Transliteração nacional", nationalTranslitOptions, valNationalTranslit.text.toString()) {
                valNationalTranslit.text = it
            }
        }
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
        settings.transliterationOptions = TransliterationOptions(
            utf8Encoding   = ckUtf8.isChecked,
            host8bit       = ckHost8bit.isChecked,
            allowLowercase = ckLowercase.isChecked,
            hostCharset    = valHostCharset.text.toString(),
            nationalTranslit = valNationalTranslit.text.toString(),
            useSiso        = ckSiso.isChecked
        )
    }
}

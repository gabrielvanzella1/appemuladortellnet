package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings

/**
 * Configuracoes > Tela > Opcoes de tela.
 *
 * Por enquanto: tamanho da fonte e os dois checkboxes sao funcionais e persistidos.
 * Os demais seletores exibem o valor atual e serao refinados depois.
 */
class ScreenOptionsActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings

    private lateinit var valFontSize: TextView
    private lateinit var valFontName: TextView
    private lateinit var valCursorType: TextView
    private lateinit var valCursorColor: TextView
    private lateinit var valFields3d: TextView
    private lateinit var valShowToolbar: TextView
    private lateinit var valLimitView: TextView
    private lateinit var valDoubleTap: TextView
    private lateinit var checkCursorBlink: CheckBox
    private lateinit var checkFields3dWhite: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_screen_options)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        valFontSize     = findViewById(R.id.val_font_size)
        valFontName     = findViewById(R.id.val_font_name)
        valCursorType   = findViewById(R.id.val_cursor_type)
        valCursorColor  = findViewById(R.id.val_cursor_color)
        valFields3d     = findViewById(R.id.val_fields_3d)
        valShowToolbar  = findViewById(R.id.val_show_toolbar)
        valLimitView    = findViewById(R.id.val_limit_view)
        valDoubleTap    = findViewById(R.id.val_double_tap)
        checkCursorBlink   = findViewById(R.id.check_cursor_blink)
        checkFields3dWhite = findViewById(R.id.check_fields_3d_white)

        loadValues()
        setupListeners()
    }

    private fun loadValues() {
        valFontSize.text    = settings.fontSize.toString()
        valFontName.text    = settings.fontName
        valCursorType.text  = settings.cursorType
        valCursorColor.text = settings.cursorColor
        valFields3d.text    = settings.fields3D
        valShowToolbar.text = settings.showToolbar
        valLimitView.text   = settings.limitView
        valDoubleTap.text   = settings.doubleTapAction
        checkCursorBlink.isChecked   = settings.cursorBlinking
        checkFields3dWhite.isChecked = settings.fields3DWhiteBg
    }

    private fun setupListeners() {
        // Tamanho da fonte (funcional)
        findViewById<View>(R.id.row_font_size).setOnClickListener { pickFontSize() }

        // Checkboxes (funcionais) — clique na linha alterna
        findViewById<View>(R.id.row_cursor_blink).setOnClickListener {
            checkCursorBlink.isChecked = !checkCursorBlink.isChecked
            settings.cursorBlinking = checkCursorBlink.isChecked
        }
        findViewById<View>(R.id.row_fields_3d_white).setOnClickListener {
            checkFields3dWhite.isChecked = !checkFields3dWhite.isChecked
            settings.fields3DWhiteBg = checkFields3dWhite.isChecked
        }

        // Demais seletores — refinaremos depois
        findViewById<View>(R.id.row_font_name).setOnClickListener { emBreve("Nome da fonte") }
        findViewById<View>(R.id.row_cursor_type).setOnClickListener { emBreve("Tipo de cursor") }
        findViewById<View>(R.id.row_cursor_color).setOnClickListener { emBreve("Cor do cursor") }
        findViewById<View>(R.id.row_fields_3d).setOnClickListener { emBreve("Campos variáveis 3D") }
        findViewById<View>(R.id.row_show_toolbar).setOnClickListener { emBreve("Mostrar barra de ferramentas") }
        findViewById<View>(R.id.row_limit_view).setOnClickListener { emBreve("Limitar visualização da tela") }
        findViewById<View>(R.id.row_double_tap).setOnClickListener { emBreve("Toque duas vezes") }
    }

    private fun pickFontSize() {
        val sizes = arrayOf("8", "9", "10", "11", "12", "14", "16", "18", "20", "24")
        val current = settings.fontSize.toString()
        val checked = sizes.indexOf(current).takeIf { it >= 0 } ?: 4
        AlertDialog.Builder(this)
            .setTitle("Tamanho da fonte")
            .setSingleChoiceItems(sizes, checked) { dialog, which ->
                val size = sizes[which].toInt()
                settings.fontSize = size
                valFontSize.text = size.toString()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun emBreve(nome: String) {
        Toast.makeText(this, "$nome: em breve", Toast.LENGTH_SHORT).show()
    }
}

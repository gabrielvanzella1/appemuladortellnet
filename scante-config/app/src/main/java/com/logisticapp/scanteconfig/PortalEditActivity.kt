package com.logisticapp.scanteconfig

import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.logisticapp.scanteconfig.config.ConfigSession
import com.logisticapp.scanteconfig.config.KeyMapText
import com.logisticapp.scanteconfig.config.Portal

class PortalEditActivity : AppCompatActivity() {

    private var index = -1
    private lateinit var inputName: EditText
    private lateinit var inputHomepage: EditText
    private lateinit var inputKeys: EditText
    private lateinit var seekFontSize: SeekBar
    private lateinit var textFontSizeValue: TextView
    private lateinit var switchZoomControls: SwitchMaterial
    private lateinit var switchWideViewport: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portal_edit)

        index = intent.getIntExtra(MainActivity.EXTRA_INDEX, -1)

        inputName = findViewById(R.id.input_name)
        inputHomepage = findViewById(R.id.input_homepage)
        inputKeys = findViewById(R.id.input_keys)
        seekFontSize = findViewById(R.id.seek_font_size)
        textFontSizeValue = findViewById(R.id.text_font_size_value)
        switchZoomControls = findViewById(R.id.switch_zoom_controls)
        switchWideViewport = findViewById(R.id.switch_wide_viewport)

        val existing = ConfigSession.portals.getOrNull(index)
        val initialFontPercent = existing?.textScalingPercent ?: 100
        seekFontSize.progress = (initialFontPercent - MIN_FONT_PERCENT).coerceIn(0, seekFontSize.max)
        textFontSizeValue.text = "${seekFontSize.progress + MIN_FONT_PERCENT}%"
        seekFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textFontSizeValue.text = "${progress + MIN_FONT_PERCENT}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        switchZoomControls.isChecked = existing?.enableZoomControls ?: false
        switchWideViewport.isChecked = existing?.useWideViewport ?: true

        if (existing != null) {
            inputName.setText(existing.name)
            inputHomepage.setText(existing.homepage)
            inputKeys.setText(KeyMapText.format(ConfigSession.keyActions[existing.id].orEmpty()))
        }

        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btn_save_portal).setOnClickListener { save() }

        val deleteButton = findViewById<android.view.View>(R.id.btn_delete_portal_full)
        deleteButton.visibility = if (existing != null) android.view.View.VISIBLE else android.view.View.GONE
        deleteButton.setOnClickListener { confirmDelete() }
    }

    private fun save() {
        val name = inputName.text.toString().trim()
        val homepage = inputHomepage.text.toString().trim()

        if (name.isEmpty()) {
            inputName.error = "Obrigatório"
            return
        }
        if (homepage.isEmpty() || !(homepage.startsWith("http://") || homepage.startsWith("https://"))) {
            inputHomepage.error = "Informe uma URL começando com http:// ou https://"
            return
        }

        val existing = ConfigSession.portals.getOrNull(index)
        val id = existing?.id ?: ConfigSession.uniqueIdFor(name, index)
        val portal = Portal(
            id = id,
            name = name,
            homepage = homepage,
            useWideViewport = switchWideViewport.isChecked,
            textScalingPercent = seekFontSize.progress + MIN_FONT_PERCENT,
            enableZoomControls = switchZoomControls.isChecked,
            initialScale = existing?.initialScale ?: 0,
            domInjectionScript = existing?.domInjectionScript
        )
        val keys = KeyMapText.parse(inputKeys.text.toString())

        ConfigSession.upsertPortal(index, portal, keys)
        Toast.makeText(this, "Portal \"$name\" pronto. Toque em Salvar/Atualizar na tela anterior para aplicar.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Excluir portal")
            .setMessage("Remover \"${inputName.text}\" da lista de abas?")
            .setPositiveButton("Excluir") { _, _ ->
                ConfigSession.removePortal(index)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        private const val MIN_FONT_PERCENT = 50
    }
}

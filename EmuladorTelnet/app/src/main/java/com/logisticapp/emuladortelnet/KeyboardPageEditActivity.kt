package com.logisticapp.emuladortelnet

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.toolbar.ToolbarButton
import com.logisticapp.emuladortelnet.toolbar.ToolbarCatalog

/**
 * Edição dos botões de uma página personalizada do teclado.
 * Recebe "pageIndex" via intent extra.
 */
class KeyboardPageEditActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private var pageIndex: Int = 0
    private lateinit var llButtons: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings  = AppSettings.get(this)
        pageIndex = intent.getIntExtra("pageIndex", 0)
        setContentView(R.layout.activity_keyboard_page_edit)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Página ${pageIndex + 3}"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        llButtons = findViewById(R.id.ll_buttons)
        rebuildList()

        findViewById<android.view.View>(R.id.btn_add_button).setOnClickListener {
            openAddButton()
        }
    }

    private fun currentPage(): List<ToolbarButton> {
        val pages = settings.keyboardSettings.extraPages
        return if (pageIndex in pages.indices) pages[pageIndex] else emptyList()
    }

    private fun savePage(buttons: List<ToolbarButton>) {
        val s = settings.keyboardSettings
        val pages = s.extraPages.toMutableList()
        if (pageIndex in pages.indices) pages[pageIndex] = buttons
        s.extraPages = pages
        settings.keyboardSettings = s
    }

    private fun rebuildList() {
        llButtons.removeAllViews()
        currentPage().forEachIndexed { idx, btn ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(16), dp(8), dp(16), dp(8))
            }
            val tv = TextView(this).apply {
                text = "${btn.label}  —  ${ToolbarCatalog.describe(btn.action)}"
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnDel = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    val btns = currentPage().toMutableList()
                    btns.removeAt(idx)
                    savePage(btns)
                    rebuildList()
                }
            }
            row.addView(tv)
            row.addView(btnDel)
            llButtons.addView(row)
            val div = android.view.View(this)
            div.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            div.setBackgroundColor(0xFFDDDDDD.toInt())
            llButtons.addView(div)
        }
    }

    private fun openAddButton() {
        val options = arrayOf("Selecionar da lista", "Botão personalizado")
        AlertDialog.Builder(this)
            .setTitle("Adicionar botão")
            .setItems(options) { _, idx ->
                if (idx == 0) pickFromCatalog()
                else createCustomButton()
            }
            .show()
    }

    private fun pickFromCatalog() {
        val items = ToolbarCatalog.available
        val names = items.map { "${it.label}  —  ${ToolbarCatalog.describe(it.action)}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Escolher botão")
            .setItems(names) { _, idx ->
                val btns = currentPage().toMutableList()
                btns.add(items[idx])
                savePage(btns)
                rebuildList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createCustomButton() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        val etLabel = EditText(this).apply { hint = "Rótulo (ex: OK)" }
        val spinnerType = Spinner(this)
        val types = arrayOf("Texto livre", "Ctrl+letra", "Tecla F", "Sequência ESC")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val etValue = EditText(this).apply { hint = "Valor" }
        layout.addView(etLabel)
        layout.addView(spinnerType)
        layout.addView(etValue)

        AlertDialog.Builder(this)
            .setTitle("Criar botão")
            .setView(layout)
            .setPositiveButton("Adicionar") { _, _ ->
                val label  = etLabel.text.toString().take(8).ifBlank { "?" }
                val value  = etValue.text.toString()
                val action = when (spinnerType.selectedItemPosition) {
                    1    -> "CTRL_${value.firstOrNull()?.uppercaseChar() ?: 'A'}"
                    2    -> "F${value.filter { it.isDigit() }.ifBlank { "1" }}"
                    3    -> "TEXT:${value.replace("\\e", "")}"
                    else -> "TEXT:$value"
                }
                val btns = currentPage().toMutableList()
                btns.add(ToolbarButton(label, action))
                savePage(btns)
                rebuildList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

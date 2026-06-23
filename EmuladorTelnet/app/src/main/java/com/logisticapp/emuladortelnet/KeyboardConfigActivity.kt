package com.logisticapp.emuladortelnet

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.toolbar.ToolbarCatalog

/**
 * Personalização do teclado ScanTE (IME):
 *   - Ativar/desativar páginas padrão (QWERTY, Numérica, Navegação, Fn+Ctrl)
 *   - Editar a linha de símbolos da página numérica
 *   - Adicionar/remover páginas personalizadas
 */
class KeyboardConfigActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var ckQwerty: CheckBox
    private lateinit var ckNumeric: CheckBox
    private lateinit var ckNavigation: CheckBox
    private lateinit var ckFunctionKeys: CheckBox
    private lateinit var etSymbols: EditText
    private lateinit var llExtraPages: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        setContentView(R.layout.activity_keyboard_config)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ckQwerty       = findViewById(R.id.ck_qwerty)
        ckNumeric      = findViewById(R.id.ck_numeric)
        ckNavigation   = findViewById(R.id.ck_navigation)
        ckFunctionKeys = findViewById(R.id.ck_function_keys)
        etSymbols      = findViewById(R.id.et_symbols_row)
        llExtraPages   = findViewById(R.id.ll_extra_pages)

        val s = settings.keyboardSettings
        ckQwerty.isChecked       = s.showQwerty
        ckNumeric.isChecked      = s.showNumeric
        ckNavigation.isChecked   = s.showNavigation
        ckFunctionKeys.isChecked = s.showFunctionKeys
        etSymbols.setText(s.symbolsRow)

        rebuildExtraPagesList()

        findViewById<android.view.View>(R.id.btn_add_page).setOnClickListener {
            openAddPage()
        }
    }

    override fun onResume() {
        super.onResume()
        rebuildExtraPagesList()
    }

    private fun openAddPage() {
        val presetNames = ToolbarCatalog.presets.keys.toList()
        AlertDialog.Builder(this)
            .setTitle("Adicionar página ao teclado")
            .setItems(presetNames.toTypedArray()) { _, idx ->
                val buttons = ToolbarCatalog.presets[presetNames[idx]] ?: return@setItems
                settings.addKeyboardPage(buttons)
                rebuildExtraPagesList()
                Toast.makeText(this, "Página \"${presetNames[idx]}\" adicionada", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Página em branco") { _, _ ->
                settings.addKeyboardPage(emptyList())
                rebuildExtraPagesList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun rebuildExtraPagesList() {
        llExtraPages.removeAllViews()
        val pages = settings.keyboardSettings.extraPages
        // Calcula o número da página levando em conta as 4 páginas fixas possíveis
        val ks = settings.keyboardSettings
        var fixedCount = 0
        if (ks.showQwerty)       fixedCount++
        if (ks.showNumeric)      fixedCount++
        if (ks.showNavigation)   fixedCount++
        if (ks.showFunctionKeys) fixedCount++

        pages.forEachIndexed { idx, page ->
            val pageNum = fixedCount + idx + 1
            val row = layoutInflater.inflate(R.layout.item_toolbar_bar, llExtraPages, false)
            val tv  = row.findViewById<TextView>(R.id.tv_bar_name)
            tv.text = "Página $pageNum: ${if (page.isEmpty()) "(vazia)" else page.take(5).joinToString(" ") { it.label }}"

            row.findViewById<android.view.View>(R.id.btn_edit_bar).setOnClickListener {
                startActivity(Intent(this, KeyboardPageEditActivity::class.java)
                    .putExtra("pageIndex", idx))
            }
            row.findViewById<android.view.View>(R.id.btn_remove_bar).setOnClickListener {
                AlertDialog.Builder(this)
                    .setMessage("Remover a página $pageNum?")
                    .setPositiveButton("Remover") { _, _ ->
                        settings.removeKeyboardPage(idx)
                        rebuildExtraPagesList()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            llExtraPages.addView(row)
        }
        if (pages.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Nenhuma página personalizada ainda"
            tv.setTextColor(0xFF888888.toInt())
            tv.setPadding(dp(16), dp(12), dp(16), dp(12))
            llExtraPages.addView(tv)
        }
    }

    override fun onPause() {
        super.onPause()
        val s = settings.keyboardSettings
        s.showQwerty       = ckQwerty.isChecked
        s.showNumeric      = ckNumeric.isChecked
        s.showNavigation   = ckNavigation.isChecked
        s.showFunctionKeys = ckFunctionKeys.isChecked
        s.symbolsRow       = etSymbols.text.toString().ifBlank { "@#\$%&*()[]{}<>/|=_" }
        settings.keyboardSettings = s
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

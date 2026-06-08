package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.toolbar.ToolbarCatalog

/**
 * Configuracoes > Tela > Configuracao da barra de ferramentas.
 * Mostra as 4 barras com seus botoes. ADICIONAR (topo) anexa botoes a uma barra.
 * Toque num botao remove-o.
 */
class ToolbarConfigActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_toolbar_config)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        container = findViewById(R.id.bars_container)
    }

    override fun onResume() {
        super.onResume()
        buildBars()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add_button -> { chooseBarToAdd(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun chooseBarToAdd() {
        val names = Array(ToolbarCatalog.NUM_BARS) { "Barra de ferramentas ${it + 1}" }
        AlertDialog.Builder(this)
            .setTitle("Adicionar a qual barra?")
            .setItems(names) { _, which ->
                val intent = android.content.Intent(this, ToolbarAddActivity::class.java)
                intent.putExtra(ToolbarAddActivity.EXTRA_BAR_INDEX, which)
                startActivity(intent)
            }
            .show()
    }

    private fun buildBars() {
        container.removeAllViews()
        val bars = settings.toolbars
        for ((barIndex, bar) in bars.withIndex()) {
            container.addView(sectionTitle("Barra de ferramentas ${barIndex + 1}"))
            if (bar.isEmpty()) {
                container.addView(emptyHint("(sem botões — use ADICIONAR)"))
            } else {
                for ((btnIndex, btn) in bar.withIndex()) {
                    container.addView(buttonRow(barIndex, btnIndex, btn.label, ToolbarCatalog.describe(btn.action)))
                }
            }
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(0xFF888888.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(16), dp(20), dp(16), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun emptyHint(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(0xFFAAAAAA.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
    }

    private fun buttonRow(barIndex: Int, btnIndex: Int, label: String, desc: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52)
            setPadding(dp(16), dp(6), dp(12), dp(6))
            isClickable = true
            background = themedSelectableBackground()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Pastilha com o rotulo (parecido com os botoes do GlinkVT)
        val chip = TextView(this).apply {
            text = label
            setTextColor(0xFF333333.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            minWidth = dp(56)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = chipBackground()
        }

        val descView = TextView(this).apply {
            text = desc
            setTextColor(0xFF888888.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val removeView = TextView(this).apply {
            text = "✕"
            setTextColor(0xFFCC0000.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dp(12), dp(4), dp(8), dp(4))
        }

        row.addView(chip)
        row.addView(descView)
        row.addView(removeView)

        val remove = { confirmRemove(barIndex, btnIndex, label) }
        row.setOnClickListener { remove() }
        removeView.setOnClickListener { remove() }
        return row
    }

    private fun confirmRemove(barIndex: Int, btnIndex: Int, label: String) {
        AlertDialog.Builder(this)
            .setTitle("Remover botão")
            .setMessage("Remover \"$label\" da Barra de ferramentas ${barIndex + 1}?")
            .setPositiveButton("Remover") { _, _ ->
                settings.removeButton(barIndex, btnIndex)
                buildBars()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun themedSelectableBackground(): android.graphics.drawable.Drawable? {
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return getDrawable(outValue.resourceId)
    }

    private fun chipBackground(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(0xFFE8E8E8.toInt())
            setStroke(dp(1), 0xFFBBBBBB.toInt())
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

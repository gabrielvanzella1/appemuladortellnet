package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.toolbar.ToolbarButton
import com.logisticapp.emuladortelnet.toolbar.ToolbarCatalog

/**
 * Tela "Adicionar botoes": lista os botoes disponiveis com checkbox.
 * Os selecionados sao anexados a barra informada via EXTRA_BAR_INDEX.
 */
class ToolbarAddActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private var barIndex = 0
    private val checks = mutableListOf<Pair<ToolbarButton, CheckBox>>()

    companion object {
        const val EXTRA_BAR_INDEX = "bar_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_toolbar_add)

        barIndex = intent.getIntExtra(EXTRA_BAR_INDEX, 0)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.subtitle = "Barra ${barIndex + 1} — selecione um ou mais"
        toolbar.setNavigationOnClickListener { finish() }

        buildList()

        findViewById<Button>(R.id.btn_add_selected).setOnClickListener { addSelected() }
    }

    private fun buildList() {
        val container = findViewById<LinearLayout>(R.id.buttons_container)
        for (btn in ToolbarCatalog.available) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(52)
                setPadding(dp(16), dp(6), dp(16), dp(6))
                isClickable = true
                background = selectableBg()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val chip = TextView(this).apply {
                text = btn.label
                setTextColor(0xFF333333.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                minWidth = dp(64)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = chipBg()
            }

            val desc = TextView(this).apply {
                text = ToolbarCatalog.describe(btn.action)
                setTextColor(0xFF888888.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(16), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val check = CheckBox(this).apply {
                isClickable = false
                isFocusable = false
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    getColor(R.color.primary))
            }

            row.addView(chip)
            row.addView(desc)
            row.addView(check)
            row.setOnClickListener { check.isChecked = !check.isChecked }

            container.addView(row)
            checks.add(btn to check)
        }
    }

    private fun addSelected() {
        val selected = checks.filter { it.second.isChecked }.map { it.first }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Nenhum botão selecionado", Toast.LENGTH_SHORT).show()
            return
        }
        for (btn in selected) settings.addButtonToBar(barIndex, btn)
        Toast.makeText(this, "${selected.size} botão(ões) adicionado(s)", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun selectableBg(): android.graphics.drawable.Drawable? {
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return getDrawable(outValue.resourceId)
    }

    private fun chipBg(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(0xFFE8E8E8.toInt())
            setStroke(dp(1), 0xFFBBBBBB.toInt())
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

package com.logisticapp.emuladortelnet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Calculadora flutuante acessível pelo menu de 3 pontinhos no terminal.
 * Callback [onSendToTerminal] é chamado quando o usuário clica "→ Enviar ao terminal".
 */
class CalculatorDialog : DialogFragment() {

    var onSendToTerminal: ((String) -> Unit)? = null

    private lateinit var tvDisplay: TextView
    private lateinit var tvExpression: TextView

    private var current   = "0"
    private var operand   = ""
    private var operator  = ""
    private var newInput  = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_calculator, null)

        tvDisplay    = view.findViewById(R.id.tv_display)
        tvExpression = view.findViewById(R.id.tv_expression)

        wireButtons(view)

        val dlg = AlertDialog.Builder(requireContext())
            .setTitle("Calculadora")
            .setView(view)
            .setNegativeButton("Fechar", null)
            .create()

        dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dlg
    }

    private fun wireButtons(v: View) {
        // Números
        val digits = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9"
        )
        digits.forEach { (id, d) -> v.findViewById<Button>(id).setOnClickListener { inputDigit(d) } }

        v.findViewById<Button>(R.id.btn_dot).setOnClickListener { inputDot() }

        // Operadores
        v.findViewById<Button>(R.id.btn_add).setOnClickListener { inputOp("+") }
        v.findViewById<Button>(R.id.btn_sub).setOnClickListener { inputOp("-") }
        v.findViewById<Button>(R.id.btn_mul).setOnClickListener { inputOp("×") }
        v.findViewById<Button>(R.id.btn_div).setOnClickListener { inputOp("÷") }

        // Ações
        v.findViewById<Button>(R.id.btn_equals).setOnClickListener  { calcEquals() }
        v.findViewById<Button>(R.id.btn_clear).setOnClickListener   { clear() }
        v.findViewById<Button>(R.id.btn_sign).setOnClickListener    { toggleSign() }
        v.findViewById<Button>(R.id.btn_percent).setOnClickListener { applyPercent() }

        // Enviar ao terminal (só visível quando há terminal ativo)
        val btnSend = v.findViewById<Button>(R.id.btn_send_terminal)
        if (onSendToTerminal == null) {
            btnSend.visibility = android.view.View.GONE
        } else {
            btnSend.setOnClickListener {
                onSendToTerminal?.invoke(displayValue())
                dismiss()
            }
        }
    }

    // ── Lógica ───────────────────────────────────────────────────────────────

    private fun inputDigit(d: String) {
        if (newInput) {
            current = if (d == "0") "0" else d
            newInput = false
        } else {
            current = if (current == "0") d else current + d
            if (current.length > 15) return
        }
        updateDisplay()
    }

    private fun inputDot() {
        if (newInput) { current = "0."; newInput = false }
        else if (!current.contains('.')) current += "."
        updateDisplay()
    }

    private fun inputOp(op: String) {
        if (operator.isNotEmpty() && !newInput) calcEquals(keepOp = true)
        operand  = current
        operator = op
        newInput = true
        tvExpression.text = "$operand $operator"
    }

    private fun calcEquals(keepOp: Boolean = false) {
        if (operator.isEmpty() || operand.isEmpty()) return
        val a = parse(operand)
        val b = parse(current)
        val result = try {
            when (operator) {
                "+"  -> a.add(b)
                "-"  -> a.subtract(b)
                "×"  -> a.multiply(b)
                "÷"  -> if (b.compareTo(BigDecimal.ZERO) == 0) null
                        else a.divide(b, MathContext(15, RoundingMode.HALF_UP))
                else -> null
            }
        } catch (e: Exception) { null }

        tvExpression.text = "$operand $operator $current ="
        current  = if (result == null) "Erro" else formatResult(result)
        operand  = if (keepOp) current else ""
        if (!keepOp) operator = ""
        newInput = true
        updateDisplay()
    }

    private fun clear() {
        current = "0"; operand = ""; operator = ""; newInput = true
        tvExpression.text = ""; updateDisplay()
    }

    private fun toggleSign() {
        if (current == "0" || current == "Erro") return
        current = if (current.startsWith("-")) current.drop(1) else "-$current"
        updateDisplay()
    }

    private fun applyPercent() {
        val v = parse(current)
        current = formatResult(v.divide(BigDecimal("100"), MathContext(15, RoundingMode.HALF_UP)))
        newInput = true
        updateDisplay()
    }

    private fun updateDisplay() { tvDisplay.text = current }

    private fun displayValue(): String = current

    private fun parse(s: String): BigDecimal = try { BigDecimal(s) } catch (e: Exception) { BigDecimal.ZERO }

    private fun formatResult(v: BigDecimal): String {
        val stripped = v.stripTrailingZeros()
        return if (stripped.scale() <= 0) stripped.toBigInteger().toString()
               else stripped.toPlainString().let {
                   if (it.length > 14) "%.10g".format(v.toDouble()) else it
               }
    }
}

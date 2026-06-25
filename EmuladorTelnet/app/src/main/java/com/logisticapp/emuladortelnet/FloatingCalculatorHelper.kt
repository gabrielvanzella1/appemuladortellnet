package com.logisticapp.emuladortelnet

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Calculadora flutuante e arrastável dentro da janela da activity.
 * Não precisa de permissão SYSTEM_ALERT_WINDOW.
 * Pode ser minimizada para um botão 🧮 e restaurada.
 */
class FloatingCalculatorHelper(
    private val activity: Activity,
    private val onSendResult: ((String) -> Unit)? = null
) {
    private val root = activity.window.decorView as FrameLayout
    private lateinit var fullView: View
    private lateinit var miniView: View

    // Estado da calculadora (preservado entre minimize/restore)
    private var current  = "0"
    private var operand  = ""
    private var operator = ""
    private var newInput = true
    private var tvDisplay: TextView? = null
    private var tvExpr: TextView? = null

    fun show() {
        buildFullView()
        buildMiniView()
        showFull()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Construção das views
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildFullView() {
        fullView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_floating_calculator, root, false)
        fullView.elevation = 24f

        tvDisplay = fullView.findViewById(R.id.tv_display)
        tvExpr    = fullView.findViewById(R.id.tv_expression)

        wireButtons(fullView)

        fullView.findViewById<ImageButton>(R.id.fl_minimize).setOnClickListener { showMini() }
        fullView.findViewById<ImageButton>(R.id.fl_close).setOnClickListener    { dismiss() }

        // Botão "Enviar ao terminal" — aparece só quando há callback
        val btnSend = fullView.findViewById<Button>(R.id.btn_send_terminal)
        if (onSendResult != null) {
            btnSend.visibility = View.VISIBLE
            btnSend.setOnClickListener { onSendResult.invoke(current); dismiss() }
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        root.addView(fullView, lp)

        // Posicionar no canto superior direito após o layout ser medido
        fullView.post {
            val dp = activity.resources.displayMetrics.density
            val margin = (16 * dp).toInt()
            // status bar + action bar ~= 100dp; coloca a 140dp do topo
            fullView.x = (root.width - fullView.width - margin).toFloat()
            fullView.y = (140 * dp)
        }

        // Arrastar apenas pela área do título (não interfere nos botões)
        makeDraggable(fullView.findViewById(R.id.fl_title), fullView)
    }

    private fun buildMiniView() {
        val dp = activity.resources.displayMetrics.density
        val size = (56 * dp).toInt()

        miniView = TextView(activity).apply {
            text = "🧮"
            textSize = 24f
            gravity = Gravity.CENTER
            elevation = 24f
            background = GradientDrawable().apply {
                setColor(0xFF0F2A3D.toInt())
                setStroke((2 * dp).toInt(), 0xFF00D67A.toInt())
                cornerRadius = size / 2f
            }
            setOnClickListener { showFull() }
        }

        val lp = FrameLayout.LayoutParams(size, size)
        root.addView(miniView, lp)

        miniView.post {
            val margin = (20 * dp).toInt()
            miniView.x = (root.width - size - margin).toFloat()
            miniView.y = (root.height - size - 180 * dp)
        }

        makeDraggable(miniView, miniView)
        miniView.visibility = View.GONE
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Visibilidade
    // ──────────────────────────────────────────────────────────────────────────

    private fun showFull() {
        miniView.visibility = View.GONE
        fullView.visibility = View.VISIBLE
        fullView.bringToFront()
    }

    private fun showMini() {
        fullView.visibility = View.GONE
        miniView.visibility = View.VISIBLE
        miniView.bringToFront()
    }

    private fun dismiss() {
        runCatching { root.removeView(fullView) }
        runCatching { root.removeView(miniView) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arrastar
    // ──────────────────────────────────────────────────────────────────────────

    private fun makeDraggable(handle: View, target: View) {
        var startX = 0f; var startY = 0f
        var startRawX = 0f; var startRawY = 0f
        var dragging = false

        handle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX    = target.x
                    startY    = target.y
                    startRawX = ev.rawX
                    startRawY = ev.rawY
                    dragging  = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - startRawX
                    val dy = ev.rawY - startRawY
                    if (!dragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) dragging = true
                    if (dragging) {
                        target.x = startX + dx
                        target.y = startY + dy
                    }
                    dragging
                }
                MotionEvent.ACTION_UP -> {
                    // Se não arrastou, repassa o evento como clique ao próprio handle
                    if (!dragging) handle.performClick()
                    dragging
                }
                else -> false
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lógica da calculadora
    // ──────────────────────────────────────────────────────────────────────────

    private fun wireButtons(v: View) {
        mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2", R.id.btn_3 to "3",
            R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6", R.id.btn_7 to "7",
            R.id.btn_8 to "8", R.id.btn_9 to "9"
        ).forEach { (id, d) -> v.findViewById<Button>(id).setOnClickListener { inputDigit(d) } }

        v.findViewById<Button>(R.id.btn_dot).setOnClickListener     { inputDot() }
        v.findViewById<Button>(R.id.btn_add).setOnClickListener     { inputOp("+") }
        v.findViewById<Button>(R.id.btn_sub).setOnClickListener     { inputOp("-") }
        v.findViewById<Button>(R.id.btn_mul).setOnClickListener     { inputOp("×") }
        v.findViewById<Button>(R.id.btn_div).setOnClickListener     { inputOp("÷") }
        v.findViewById<Button>(R.id.btn_equals).setOnClickListener  { calcEquals() }
        v.findViewById<Button>(R.id.btn_clear).setOnClickListener   { clear() }
        v.findViewById<Button>(R.id.btn_sign).setOnClickListener    { toggleSign() }
        v.findViewById<Button>(R.id.btn_percent).setOnClickListener { applyPercent() }
    }

    private fun inputDigit(d: String) {
        if (newInput) { current = if (d == "0") "0" else d; newInput = false }
        else { if (current.length >= 15) return; current = if (current == "0") d else current + d }
        update()
    }

    private fun inputDot() {
        if (newInput) { current = "0."; newInput = false }
        else if (!current.contains('.')) current += "."
        update()
    }

    private fun inputOp(op: String) {
        if (operator.isNotEmpty() && !newInput) calcEquals(keepOp = true)
        operand = current; operator = op; newInput = true
        tvExpr?.text = "$operand $operator"
    }

    private fun calcEquals(keepOp: Boolean = false) {
        if (operator.isEmpty() || operand.isEmpty()) return
        val a = bd(operand); val b = bd(current)
        val r = runCatching {
            when (operator) {
                "+"  -> a.add(b)
                "-"  -> a.subtract(b)
                "×"  -> a.multiply(b)
                "÷"  -> if (b.compareTo(BigDecimal.ZERO) == 0) null
                        else a.divide(b, MathContext(15, RoundingMode.HALF_UP))
                else -> null
            }
        }.getOrNull()
        tvExpr?.text = "$operand $operator $current ="
        current = if (r == null) "Erro" else fmt(r)
        operand = if (keepOp) current else ""
        if (!keepOp) operator = ""
        newInput = true
        update()
    }

    private fun clear() {
        current = "0"; operand = ""; operator = ""; newInput = true
        tvExpr?.text = ""; update()
    }

    private fun toggleSign() {
        if (current != "0" && current != "Erro")
            current = if (current.startsWith("-")) current.drop(1) else "-$current"
        update()
    }

    private fun applyPercent() {
        current = fmt(bd(current).divide(BigDecimal("100"), MathContext(15, RoundingMode.HALF_UP)))
        newInput = true; update()
    }

    private fun update() { tvDisplay?.text = current }

    private fun bd(s: String) = runCatching { BigDecimal(s) }.getOrDefault(BigDecimal.ZERO)

    private fun fmt(v: BigDecimal): String {
        val s = v.stripTrailingZeros()
        return if (s.scale() <= 0) s.toBigInteger().toString() else s.toPlainString()
    }
}

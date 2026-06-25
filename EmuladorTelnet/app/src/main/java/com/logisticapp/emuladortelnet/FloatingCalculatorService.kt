package com.logisticapp.emuladortelnet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Calculadora flutuante que fica sobre qualquer tela do app (e do Android).
 * Inicia via startService(Intent(ctx, FloatingCalculatorService::class.java)).
 * Pode ser minimizada para um botão circular arrastável.
 */
class FloatingCalculatorService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var fullView: View
    private lateinit var miniView: View

    private var current  = "0"
    private var operand  = ""
    private var operator = ""
    private var newInput = true

    private var tvDisplay: TextView? = null
    private var tvExpr: TextView? = null

    // Parâmetros de layout
    private val TYPE_OVERLAY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else
        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    companion object {
        const val CHANNEL_ID = "floating_calc"
        fun start(ctx: Context) = ctx.startService(Intent(ctx, FloatingCalculatorService::class.java))
        fun stop(ctx: Context)  = ctx.stopService(Intent(ctx, FloatingCalculatorService::class.java))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        buildFullView()
        buildMiniView()
        showFull()
    }

    override fun onDestroy() {
        runCatching { wm.removeView(fullView) }
        runCatching { wm.removeView(miniView) }
        super.onDestroy()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICAÇÃO FOREGROUND (obrigatório Android 8+)
    // ══════════════════════════════════════════════════════════════════════════

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Calculadora flutuante",
                NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val stopIntent = PendingIntent.getService(this, 0,
            Intent(this, FloatingCalculatorService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calculadora ScanTE")
            .setContentText("Calculadora flutuante ativa")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Fechar", stopIntent)
            .build()
        startForeground(1, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    // ══════════════════════════════════════════════════════════════════════════
    // JANELA COMPLETA
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildFullView() {
        fullView = LayoutInflater.from(this).inflate(R.layout.layout_floating_calculator, null)
        tvDisplay = fullView.findViewById(R.id.tv_display)
        tvExpr    = fullView.findViewById(R.id.tv_expression)

        wireButtons(fullView)

        fullView.findViewById<ImageButton>(R.id.fl_minimize).setOnClickListener { showMini() }
        fullView.findViewById<ImageButton>(R.id.fl_close).setOnClickListener { stopSelf() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            TYPE_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 60; y = 200 }

        makeDraggable(fullView.findViewById(R.id.fl_header), params, fullView)
        wm.addView(fullView, params)
        fullView.visibility = View.GONE
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BOTÃO MINIMIZADO
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildMiniView() {
        miniView = TextView(this).apply {
            text = "🧮"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF0F2A3D.toInt())
            // Borda verde jade
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF0F2A3D.toInt())
                setStroke(3, 0xFF00D67A.toInt())
                cornerRadius = 32f
            }
            setPadding(18, 18, 18, 18)
            setOnClickListener { showFull() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            TYPE_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 60; y = 200 }

        makeDraggable(miniView, params, miniView)
        wm.addView(miniView, params)
        miniView.visibility = View.GONE
    }

    private fun showFull() {
        miniView.visibility = View.GONE
        // Re-enable touch input na janela completa
        val params = fullView.layoutParams as? WindowManager.LayoutParams ?: return
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        runCatching { wm.updateViewLayout(fullView, params) }
        fullView.visibility = View.VISIBLE
    }

    private fun showMini() {
        fullView.visibility = View.GONE
        miniView.visibility = View.VISIBLE
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ARRASTAR
    // ══════════════════════════════════════════════════════════════════════════

    private fun makeDraggable(handle: View, params: WindowManager.LayoutParams, target: View) {
        var initX = 0; var initY = 0; var initTouchX = 0f; var initTouchY = 0f
        handle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    initTouchX = ev.rawX; initTouchY = ev.rawY; true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (ev.rawX - initTouchX).toInt()
                    params.y = initY + (ev.rawY - initTouchY).toInt()
                    runCatching { wm.updateViewLayout(target, params) }; true
                }
                else -> false
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LÓGICA DA CALCULADORA (idêntica ao CalculatorDialog)
    // ══════════════════════════════════════════════════════════════════════════

    private fun wireButtons(v: View) {
        mapOf(R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2", R.id.btn_3 to "3",
              R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6", R.id.btn_7 to "7",
              R.id.btn_8 to "8", R.id.btn_9 to "9")
            .forEach { (id, d) -> v.findViewById<Button>(id).setOnClickListener { inputDigit(d) } }

        v.findViewById<Button>(R.id.btn_dot).setOnClickListener { inputDot() }
        v.findViewById<Button>(R.id.btn_add).setOnClickListener { inputOp("+") }
        v.findViewById<Button>(R.id.btn_sub).setOnClickListener { inputOp("-") }
        v.findViewById<Button>(R.id.btn_mul).setOnClickListener { inputOp("×") }
        v.findViewById<Button>(R.id.btn_div).setOnClickListener { inputOp("÷") }
        v.findViewById<Button>(R.id.btn_equals).setOnClickListener  { calcEquals() }
        v.findViewById<Button>(R.id.btn_clear).setOnClickListener   { clear() }
        v.findViewById<Button>(R.id.btn_sign).setOnClickListener    { toggleSign() }
        v.findViewById<Button>(R.id.btn_percent).setOnClickListener { applyPercent() }
    }

    private fun inputDigit(d: String) {
        if (newInput) { current = if (d == "0") "0" else d; newInput = false }
        else { current = if (current == "0") d else current + d; if (current.length > 15) return }
        update()
    }

    private fun inputDot() {
        if (newInput) { current = "0."; newInput = false } else if (!current.contains('.')) current += "."
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
        val r = try {
            when (operator) {
                "+"  -> a.add(b)
                "-"  -> a.subtract(b)
                "×"  -> a.multiply(b)
                "÷"  -> if (b.compareTo(BigDecimal.ZERO) == 0) null
                        else a.divide(b, MathContext(15, RoundingMode.HALF_UP))
                else -> null
            }
        } catch (e: Exception) { null }
        tvExpr?.text = "$operand $operator $current ="
        current = if (r == null) "Erro" else fmt(r)
        operand = if (keepOp) current else ""; if (!keepOp) operator = ""
        newInput = true; update()
    }

    private fun clear()   { current = "0"; operand = ""; operator = ""; newInput = true; tvExpr?.text = ""; update() }
    private fun toggleSign() { if (current != "0" && current != "Erro") { current = if (current.startsWith("-")) current.drop(1) else "-$current" }; update() }
    private fun applyPercent() { current = fmt(bd(current).divide(BigDecimal("100"), MathContext(15, RoundingMode.HALF_UP))); newInput = true; update() }
    private fun update() { tvDisplay?.text = current }
    private fun bd(s: String) = try { BigDecimal(s) } catch (e: Exception) { BigDecimal.ZERO }
    private fun fmt(v: BigDecimal): String {
        val s = v.stripTrailingZeros()
        return if (s.scale() <= 0) s.toBigInteger().toString() else s.toPlainString()
    }
}

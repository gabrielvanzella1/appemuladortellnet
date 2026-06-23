package com.logisticapp.emuladortelnet.keyboard

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.toolbar.ToolbarButton
import com.logisticapp.emuladortelnet.toolbar.ToolbarCatalog
import kotlin.math.abs

/**
 * Teclado personalizado ScanTE.
 *
 * Páginas deslizáveis horizontalmente (swipe ou toque nas abas):
 *   Página 0 — QWERTY  (se showQwerty=true)
 *   Página 1 — Numérico (se showNumeric=true)
 *   Páginas 2… — Páginas personalizadas (Configurações > Teclado)
 */
class TerminalInputMethodService : InputMethodService() {

    private var currentPage = 0
    private var isUpperCase = false
    private var isSymbols   = false
    private lateinit var settings: AppSettings

    private data class PageDef(val label: String, val build: () -> View)

    private var pages: List<PageDef> = emptyList()
    private var pageViews: Array<View?> = emptyArray()
    private var pageContainer: FrameLayout? = null
    private var tabsRow: LinearLayout? = null

    private val BG_KEYBOARD  = Color.parseColor("#0D1B24")
    private val BG_KEY       = Color.parseColor("#1E3240")
    private val BG_KEY_PRESS = Color.parseColor("#2E5C6E")
    private val BG_SPECIAL   = Color.parseColor("#0F1819")
    private val BG_EXTRA_ROW = Color.parseColor("#0A141C")
    private val TAB_ACTIVE   = Color.parseColor("#2E5C6E")
    private val TAB_INACTIVE = Color.parseColor("#1A2D3E")
    private val TXT_KEY      = Color.WHITE
    private val TXT_MUTED    = Color.parseColor("#9DB8C9")

    private val ROW_H_EXTRA  = 38
    private val ROW_H_MAIN   = 44
    private val ROW_H_BOTTOM = 50

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings.get(this)
    }

    override fun onCreateInputView(): View = buildRoot()

    // ══════════════════════════════════════════════════════════════════════════
    // RAIZ
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildRoot(): LinearLayout {
        val ks = settings.keyboardSettings

        pages = buildList {
            if (ks.showQwerty)       add(PageDef("ABC") { buildQwertyPage() })
            if (ks.showNumeric)      add(PageDef("123") { buildNumericPage(ks.symbolsRow) })
            if (ks.showNavigation)   add(PageDef("NAV") { buildNavigationPage() })
            if (ks.showFunctionKeys) add(PageDef("Fn")  { buildFunctionPage() })
            ks.extraPages.forEachIndexed { i, btns ->
                add(PageDef("P${i+1}") { buildCustomPage(btns) })
            }
        }.ifEmpty { listOf(PageDef("ABC") { buildQwertyPage() }) }

        if (currentPage >= pages.size) currentPage = 0
        pageViews = arrayOfNulls(pages.size)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_KEYBOARD)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        var extraCount = 0
        for (bar in settings.toolbars) {
            if (bar.isEmpty() || extraCount >= 2) continue
            root.addView(buildExtraRow(bar))
            extraCount++
        }

        tabsRow = buildTabsRow()
        root.addView(tabsRow)

        pageContainer = buildPageContainer()
        root.addView(pageContainer)

        return root
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ABAS
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildTabsRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BG_EXTRA_ROW)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32))
            pages.forEachIndexed { idx, page ->
                addView(TextView(this@TerminalInputMethodService).apply {
                    // Mostra setas para indicar que há mais páginas
                    val prefix = if (idx > 0) "◀ " else ""
                    val suffix = if (idx < pages.size - 1) " ▶" else ""
                    text = "$prefix${page.label}$suffix"
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    setTextColor(if (idx == currentPage) TXT_KEY else TXT_MUTED)
                    setBackgroundColor(if (idx == currentPage) TAB_ACTIVE else TAB_INACTIVE)
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    lp.setMargins(dp(2), dp(2), dp(2), dp(2))
                    layoutParams = lp
                    setOnClickListener { switchPage(idx) }
                })
            }
        }
    }

    private fun updateTabs() {
        tabsRow?.let { row ->
            for (i in 0 until row.childCount) {
                val tab = row.getChildAt(i) as? TextView ?: continue
                val prefix = if (i > 0) "◀ " else ""
                val suffix = if (i < pages.size - 1) " ▶" else ""
                tab.text = "$prefix${pages[i].label}$suffix"
                tab.setTextColor(if (i == currentPage) TXT_KEY else TXT_MUTED)
                tab.setBackgroundColor(if (i == currentPage) TAB_ACTIVE else TAB_INACTIVE)
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONTAINER COM SWIPE
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildPageContainer(): FrameLayout {
        val frame = SwipeContainer(this) { dx ->
            if (dx < 0 && currentPage < pages.size - 1) switchPage(currentPage + 1)
            else if (dx > 0 && currentPage > 0) switchPage(currentPage - 1)
        }
        frame.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        pages.forEachIndexed { idx, page ->
            val v = page.build()
            pageViews[idx] = v
            v.visibility = if (idx == currentPage) View.VISIBLE else View.GONE
            frame.addView(v)
        }
        return frame
    }

    private fun switchPage(newPage: Int) {
        if (newPage == currentPage || newPage !in pages.indices) return
        val oldPage = currentPage
        currentPage = newPage

        val container = pageContainer ?: return
        val w = container.width.toFloat().let { if (it == 0f) 1000f else it }
        val dir = if (newPage > oldPage) 1f else -1f

        pageViews[oldPage]?.let { old ->
            ObjectAnimator.ofFloat(old, "translationX", 0f, -dir * w).apply { duration = 160; start() }
            old.postDelayed({ old.visibility = View.GONE; old.translationX = 0f }, 160)
        }
        pageViews[newPage]?.let { nw ->
            nw.translationX = dir * w
            nw.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(nw, "translationX", dir * w, 0f).apply { duration = 160; start() }
        }
        updateTabs()
    }

    private inner class SwipeContainer(ctx: Context, val onSwipe: (Float) -> Unit) : FrameLayout(ctx) {
        private var downX = 0f
        private var downY = 0f
        private var intercepting = false
        private var swipeFired = false

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x; downY = ev.y
                    intercepting = false; swipeFired = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(ev.x - downX); val dy = abs(ev.y - downY)
                    // Começa a interceptar quando movimento horizontal > 16dp e mais horizontal que vertical
                    if (!intercepting && dx > dp(16) && dx > dy * 1.1f) intercepting = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    intercepting = false
                }
            }
            return intercepting
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            when (ev.action) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.x - downX; val dy = abs(ev.y - downY)
                    // Dispara ao passar 20% da largura (funciona com arrastar lento no PC)
                    if (!swipeFired && abs(dx) > width * 0.20f && abs(dx) > dy * 1.1f) {
                        swipeFired = true
                        onSwipe(dx)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // Dispara no soltar se ainda não disparou e passou 12% da largura
                    if (!swipeFired) {
                        val dx = ev.x - downX; val dy = abs(ev.y - downY)
                        if (abs(dx) > width * 0.12f && abs(dx) > dy * 1.1f) {
                            onSwipe(dx)
                        }
                    }
                    intercepting = false; swipeFired = false
                }
                MotionEvent.ACTION_CANCEL -> { intercepting = false; swipeFired = false }
            }
            return true
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PÁGINA QWERTY
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildQwertyPage(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (!isSymbols) {
            addView(buildCharRow("1234567890", ROW_H_MAIN))
            addView(buildCharRow("qwertyuiop", ROW_H_MAIN))
            addView(buildCharRow("asdfghjkl",  ROW_H_MAIN, indented = true))
            addView(buildShiftRow())
        } else {
            addView(buildSymbolsScrollRow("~`!@#\$%^&*()-_=+", ROW_H_MAIN))
            addView(buildSymbolsScrollRow("[]{}\\|<>/?", ROW_H_MAIN))
            addView(buildSymbolsScrollRow(";:'\",.€£¥", ROW_H_MAIN))
            addView(buildShiftRow())
        }
        addView(buildQwertyBottomRow())
    }

    private fun buildCharRow(chars: String, rowH: Int, indented: Boolean = false): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            if (indented) setPadding(dp(20), 0, dp(20), 0)
            for (c in chars) {
                val d = if (isUpperCase) c.uppercaseChar().toString() else c.toString()
                addView(makeKey(d, 0, dp(rowH), weight = 1f) {
                    commitChar(if (isUpperCase) c.uppercaseChar().toString() else c.toString())
                    if (isUpperCase) { isUpperCase = false; rebuildCurrentPage() }
                })
            }
        }

    private fun buildSymbolsScrollRow(chars: String, rowH: Int): View {
        val inner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (c in chars) inner.addView(makeKey(c.toString(), dp(44), dp(rowH)) { commitChar(c.toString()) })
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(inner)
        }
    }

    private fun buildShiftRow(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(makeKey(if (isUpperCase) "⇪" else "⇧", 0, dp(ROW_H_MAIN), weight = 1.5f, isSpecial = true) {
            isUpperCase = !isUpperCase; rebuildCurrentPage()
        })
        val mid = if (!isSymbols) "zxcvbnm" else "@#+=×÷"
        for (c in mid) {
            val d = if (isUpperCase && !isSymbols) c.uppercaseChar().toString() else c.toString()
            addView(makeKey(d, 0, dp(ROW_H_MAIN), weight = 1f) {
                commitChar(if (isUpperCase && !isSymbols) c.uppercaseChar().toString() else c.toString())
                if (isUpperCase) { isUpperCase = false; rebuildCurrentPage() }
            })
        }
        addView(makeKey("⌫", 0, dp(ROW_H_MAIN), weight = 1.5f, isSpecial = true, textSize = 18f) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        })
    }

    private fun buildQwertyBottomRow(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(makeKey(if (isSymbols) "ABC" else "?!", 0, dp(ROW_H_BOTTOM), weight = 1.3f, isSpecial = true, textSize = 13f) {
            isSymbols = !isSymbols; if (isSymbols) isUpperCase = false; rebuildCurrentPage()
        })
        addView(makeKey(",", 0, dp(ROW_H_BOTTOM), weight = 0.8f) { commitChar(",") })
        addView(makeKey(" ", 0, dp(ROW_H_BOTTOM), weight = 4f)   { commitChar(" ") })
        addView(makeKey(".", 0, dp(ROW_H_BOTTOM), weight = 0.8f) { commitChar(".") })
        addView(makeKey("↵", 0, dp(ROW_H_BOTTOM), weight = 1.3f, isSpecial = true, textSize = 18f) { sendEnter() })
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PÁGINA NUMÉRICA
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildNumericPage(symbolsRow: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (symbolsRow.isNotEmpty()) addView(buildSymbolsScrollRow(symbolsRow, ROW_H_MAIN - 4))
        listOf(
            listOf("7","8","9","⌫"),
            listOf("4","5","6","  "),
            listOf("1","2","3","↵"),
            listOf("+","-","0",".")
        ).forEachIndexed { ri, row ->
            val h = if (ri == 3) ROW_H_BOTTOM else ROW_H_MAIN
            addView(LinearLayout(this@TerminalInputMethodService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                for (lbl in row) {
                    val spec = lbl.trim() in listOf("⌫","↵","  ")
                    addView(makeKey(lbl.trim(), 0, dp(h), weight = 1f, isSpecial = spec,
                        textSize = if (spec) 18f else 20f) {
                        when (lbl.trim()) {
                            "⌫"  -> currentInputConnection?.deleteSurroundingText(1, 0)
                            "↵"  -> sendEnter()
                            "  " -> commitChar(" ")
                            else -> commitChar(lbl)
                        }
                    })
                }
            })
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PÁGINA NAVEGAÇÃO
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildNavigationPage(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        // Linha 1: setas + Esc/Tab
        addView(navRow(listOf(
            "Esc" to "ESC", "↑" to "UP", "Tab" to "TAB", "⌫" to "_BS"
        )))
        // Linha 2: ←  ↓  →
        addView(navRow(listOf(
            "←" to "LEFT", "↓" to "DOWN", "→" to "RIGHT", "Del" to "DELCHAR"
        )))
        // Linha 3: Home/End/PgUp/PgDn
        addRow(listOf("Home" to "HOME", "End" to "END", "PgUp" to "PREVS", "PgDn" to "NEXTS"))
        // Linha 4: Ins/BkTab/Espaço/Enter
        addView(navRow(listOf(
            "Ins" to "INSERT", "⇤" to "BACKTAB", " " to "_SP", "↵" to "_ENTER"
        )))
    }

    private fun LinearLayout.addRow(items: List<Pair<String, String>>) {
        addView(navRow(items))
    }

    private fun navRow(items: List<Pair<String, String>>): LinearLayout =
        LinearLayout(this@TerminalInputMethodService).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for ((label, action) in items) {
                addView(makeKey(label, 0, dp(ROW_H_MAIN + 4), weight = 1f, isSpecial = false, textSize = 15f) {
                    when (action) {
                        "_BS"    -> currentInputConnection?.deleteSurroundingText(1, 0)
                        "_SP"    -> commitChar(" ")
                        "_ENTER" -> sendEnter()
                        else     -> sendAction(action)
                    }
                })
            }
        }

    // ══════════════════════════════════════════════════════════════════════════
    // PÁGINA TECLAS DE FUNÇÃO
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildFunctionPage(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        // F1-F6
        addView(LinearLayout(this@TerminalInputMethodService).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for (i in 1..6) addView(makeKey("F$i", 0, dp(ROW_H_MAIN + 4), weight = 1f, textSize = 14f) { sendAction("F$i") })
        })
        // F7-F12
        addView(LinearLayout(this@TerminalInputMethodService).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for (i in 7..12) addView(makeKey("F$i", 0, dp(ROW_H_MAIN + 4), weight = 1f, textSize = 14f) { sendAction("F$i") })
        })
        // Ctrl: A-J
        addView(LinearLayout(this@TerminalInputMethodService).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for (c in "ABCDEFGHIJ") addView(makeKey("^$c", 0, dp(ROW_H_MAIN), weight = 1f, isSpecial = true, textSize = 11f) { sendAction("CTRL_$c") })
        })
        // Ctrl: K-T
        addView(LinearLayout(this@TerminalInputMethodService).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for (c in "KLMNOPQRST") addView(makeKey("^$c", 0, dp(ROW_H_MAIN), weight = 1f, isSpecial = true, textSize = 11f) { sendAction("CTRL_$c") })
        })
        // Ctrl: U-Z + ações de conexão
        addView(LinearLayout(this@TerminalInputMethodService).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            for (c in "UVWXYZ") addView(makeKey("^$c", 0, dp(ROW_H_BOTTOM), weight = 1f, isSpecial = true, textSize = 11f) { sendAction("CTRL_$c") })
            addView(makeKey("Esc", 0, dp(ROW_H_BOTTOM), weight = 1f, textSize = 13f) { sendAction("ESC") })
            addView(makeKey("Brk", 0, dp(ROW_H_BOTTOM), weight = 1f, isSpecial = true, textSize = 11f) { sendAction("BREAK") })
            addView(makeKey("↵",   0, dp(ROW_H_BOTTOM), weight = 1f, textSize = 18f) { sendEnter() })
            addView(makeKey("⌫",   0, dp(ROW_H_BOTTOM), weight = 1f, isSpecial = true, textSize = 18f) { currentInputConnection?.deleteSurroundingText(1, 0) })
        })
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PÁGINAS PERSONALIZADAS
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildCustomPage(buttons: List<ToolbarButton>): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (buttons.isEmpty()) {
            addView(TextView(this@TerminalInputMethodService).apply {
                text = "Página vazia\nConfigure em Configurações > Teclado"
                setTextColor(TXT_MUTED)
                gravity = android.view.Gravity.CENTER
                setPadding(dp(16), dp(24), dp(16), dp(24))
            })
            return@apply
        }
        buttons.chunked(5).forEach { chunk ->
            addView(LinearLayout(this@TerminalInputMethodService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                for (btn in chunk) {
                    addView(makeKey(btn.label, 0, dp(ROW_H_MAIN), weight = 1f, isSpecial = true, textSize = 13f) {
                        sendAction(btn.action)
                    })
                }
                repeat(5 - chunk.size) {
                    addView(makeKey("", 0, dp(ROW_H_MAIN), weight = 1f, isSpecial = true) {})
                }
            })
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LINHAS EXTRAS (barras do ToolbarConfig)
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildExtraRow(bar: List<ToolbarButton>): View {
        val inner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (btn in bar) {
            inner.addView(makeKey(btn.label, dp(72), dp(ROW_H_EXTRA), isSpecial = true, textSize = 12f) {
                sendAction(btn.action)
            })
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(BG_EXTRA_ROW)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(inner)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FÁBRICA DE TECLAS
    // ══════════════════════════════════════════════════════════════════════════

    private fun makeKey(label: String, width: Int, height: Int, weight: Float = 0f,
                        isSpecial: Boolean = false, textSize: Float = 16f, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(if (isSpecial) TXT_MUTED else TXT_KEY)
            this.textSize = textSize
            gravity = android.view.Gravity.CENTER
            isSingleLine = true
            val lp = if (weight > 0f) LinearLayout.LayoutParams(0, height, weight)
                     else             LinearLayout.LayoutParams(width, height)
            val m = dp(3); lp.setMargins(m, m, m, m)
            layoutParams = lp
            background = keyBg(if (isSpecial) BG_SPECIAL else BG_KEY)
            setOnClickListener { onClick() }
        }
    }

    private fun keyBg(normal: Int): StateListDrawable {
        fun rd(c: Int) = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(c) }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), rd(BG_KEY_PRESS))
            addState(intArrayOf(), rd(normal))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENVIO
    // ══════════════════════════════════════════════════════════════════════════

    private fun commitChar(s: String) { currentInputConnection?.commitText(s, 1) }

    private fun sendEnter() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_ENTER))
    }

    private fun sendAction(action: String) {
        val bytes = ToolbarCatalog.bytesFor(action) ?: return
        currentInputConnection?.commitText(String(bytes, Charsets.ISO_8859_1), 1)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REBUILD DA PÁGINA ATUAL (após shift/símbolos)
    // ══════════════════════════════════════════════════════════════════════════

    private fun rebuildCurrentPage() {
        val idx = currentPage
        if (idx !in pages.indices) return
        val container = pageContainer ?: return
        val newView = pages[idx].build()
        val oldView = pageViews[idx]
        if (oldView != null) {
            val pos = container.indexOfChild(oldView)
            container.removeView(oldView)
            newView.visibility = View.VISIBLE
            container.addView(newView, pos)
        } else {
            newView.visibility = View.VISIBLE
            container.addView(newView)
        }
        pageViews[idx] = newView
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

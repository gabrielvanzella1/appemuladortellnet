package com.logisticapp.emuladortelnet.terminal

import android.graphics.Color
import timber.log.Timber

/**
 * Emulador de tela VT100/ANSI.
 *
 * Mantém uma grade de caracteres (rows x cols) com atributos de cor por célula.
 * Interpreta escape sequences de posicionamento de cursor, limpeza de tela e cores,
 * permitindo renderizar aplicações de tela cheia (curses-like) como o Protheus/SHARK.
 *
 * O estado de parsing é PERSISTENTE entre chamadas de feed(): as escape sequences
 * podem chegar partidas entre dois read() do socket.
 */
class TerminalEmulator(
    val rows: Int = 24,
    val cols: Int = 80
) {

    private val defaultFg = Color.rgb(0, 255, 0)   // verde terminal
    private val defaultBg = Color.BLACK

    private val chars = Array(rows) { CharArray(cols) { ' ' } }
    private val fg    = Array(rows) { IntArray(cols) { defaultFg } }
    private val bold  = Array(rows) { BooleanArray(cols) { false } }

    private var cursorRow = 0
    private var cursorCol = 0

    private var curFg = defaultFg
    private var curBold = false

    private enum class State { NORMAL, ESC, CSI }
    private var state = State.NORMAL
    private val csiParams = StringBuilder()

    companion object {
        private const val ESC = '\u001B'
        private const val NUL = '\u0000'
        private const val BEL = '\u0007'
    }

    /**
     * Alimenta o emulador com texto recebido do servidor.
     */
    fun feed(text: String) {
        for (ch in text) {
            when (state) {
                State.NORMAL -> handleNormal(ch)
                State.ESC    -> handleEsc(ch)
                State.CSI    -> handleCsi(ch)
            }
        }
    }

    private fun handleNormal(ch: Char) {
        when (ch) {
            ESC -> state = State.ESC
            '\n' -> { cursorRow++; if (cursorRow >= rows) scrollUp() }
            '\r' -> cursorCol = 0
            '\b' -> { if (cursorCol > 0) cursorCol-- }
            '\t' -> { cursorCol = ((cursorCol / 8) + 1) * 8; if (cursorCol >= cols) cursorCol = cols - 1 }
            NUL, BEL -> { /* ignorar */ }
            else -> putChar(ch)
        }
    }

    private fun handleEsc(ch: Char) {
        when (ch) {
            '[' -> { csiParams.setLength(0); state = State.CSI }
            // Sequências ESC simples que não tratamos: descartar e voltar
            else -> state = State.NORMAL
        }
    }

    private fun handleCsi(ch: Char) {
        if (ch in '0'..'9' || ch == ';' || ch == '?') {
            csiParams.append(ch)
            return
        }
        // Letra final → executar comando
        executeCsi(ch, csiParams.toString())
        state = State.NORMAL
    }

    private fun executeCsi(command: Char, paramStr: String) {
        // '?' = private mode (ex: ESC[?25h cursor). Ignorar esses comandos.
        if (paramStr.startsWith("?")) return

        val params = paramStr.split(';').map { it.toIntOrNull() }

        when (command) {
            'H', 'f' -> {
                // CUP — linha;coluna (1-based)
                val row = (params.getOrNull(0) ?: 1).coerceAtLeast(1)
                val col = (params.getOrNull(1) ?: 1).coerceAtLeast(1)
                cursorRow = (row - 1).coerceIn(0, rows - 1)
                cursorCol = (col - 1).coerceIn(0, cols - 1)
            }
            'A' -> cursorRow = (cursorRow - (params.getOrNull(0) ?: 1)).coerceAtLeast(0)
            'B' -> cursorRow = (cursorRow + (params.getOrNull(0) ?: 1)).coerceAtMost(rows - 1)
            'C' -> cursorCol = (cursorCol + (params.getOrNull(0) ?: 1)).coerceAtMost(cols - 1)
            'D' -> cursorCol = (cursorCol - (params.getOrNull(0) ?: 1)).coerceAtLeast(0)
            'J' -> eraseDisplay(params.getOrNull(0) ?: 0)
            'K' -> eraseLine(params.getOrNull(0) ?: 0)
            'm' -> applySgr(params)
            'd' -> cursorRow = ((params.getOrNull(0) ?: 1) - 1).coerceIn(0, rows - 1)  // VPA
            'G' -> cursorCol = ((params.getOrNull(0) ?: 1) - 1).coerceIn(0, cols - 1)  // CHA
            else -> Timber.d("CSI nao tratado: $command ($paramStr)")
        }
    }

    private fun putChar(ch: Char) {
        if (cursorCol >= cols) {
            cursorCol = 0
            cursorRow++
            if (cursorRow >= rows) scrollUp()
        }
        if (cursorRow in 0 until rows && cursorCol in 0 until cols) {
            chars[cursorRow][cursorCol] = ch
            fg[cursorRow][cursorCol] = curFg
            bold[cursorRow][cursorCol] = curBold
        }
        cursorCol++
    }

    private fun scrollUp() {
        // Rola tudo uma linha para cima e limpa a última
        for (r in 0 until rows - 1) {
            chars[r] = chars[r + 1].copyOf()
            fg[r] = fg[r + 1].copyOf()
            bold[r] = bold[r + 1].copyOf()
        }
        val last = rows - 1
        chars[last] = CharArray(cols) { ' ' }
        fg[last] = IntArray(cols) { defaultFg }
        bold[last] = BooleanArray(cols) { false }
        cursorRow = last
    }

    private fun eraseDisplay(mode: Int) {
        when (mode) {
            2, 3 -> {
                // Tela inteira + cursor home
                for (r in 0 until rows) clearRow(r)
                cursorRow = 0
                cursorCol = 0
            }
            0 -> {
                // Do cursor até o fim
                clearRowFrom(cursorRow, cursorCol)
                for (r in cursorRow + 1 until rows) clearRow(r)
            }
            1 -> {
                // Do início até o cursor
                for (r in 0 until cursorRow) clearRow(r)
                clearRowTo(cursorRow, cursorCol)
            }
        }
    }

    private fun eraseLine(mode: Int) {
        when (mode) {
            0 -> clearRowFrom(cursorRow, cursorCol)
            1 -> clearRowTo(cursorRow, cursorCol)
            2 -> clearRow(cursorRow)
        }
    }

    private fun clearRow(r: Int) {
        for (c in 0 until cols) {
            chars[r][c] = ' '
            fg[r][c] = defaultFg
            bold[r][c] = false
        }
    }

    private fun clearRowFrom(r: Int, fromCol: Int) {
        for (c in fromCol until cols) {
            chars[r][c] = ' '
            fg[r][c] = defaultFg
            bold[r][c] = false
        }
    }

    private fun clearRowTo(r: Int, toCol: Int) {
        for (c in 0..toCol.coerceAtMost(cols - 1)) {
            chars[r][c] = ' '
            fg[r][c] = defaultFg
            bold[r][c] = false
        }
    }

    private fun applySgr(params: List<Int?>) {
        // Sem parâmetros = reset
        val list = if (params.isEmpty() || (params.size == 1 && params[0] == null)) listOf(0) else params
        for (p in list) {
            when (p) {
                0, null -> { curFg = defaultFg; curBold = false }
                1 -> curBold = true
                2, 22 -> curBold = false
                7 -> { /* reverse: simplificado, ignorado */ }
                30 -> curFg = Color.rgb(80, 80, 80)
                31 -> curFg = Color.rgb(255, 0, 0)
                32 -> curFg = Color.rgb(0, 255, 0)
                33 -> curFg = Color.rgb(255, 255, 0)
                34 -> curFg = Color.rgb(80, 120, 255)
                35 -> curFg = Color.rgb(255, 0, 255)
                36 -> curFg = Color.rgb(0, 255, 255)
                37 -> curFg = Color.WHITE
                90 -> curFg = Color.rgb(128, 128, 128)
                91 -> curFg = Color.rgb(255, 128, 128)
                92 -> curFg = Color.rgb(128, 255, 128)
                93 -> curFg = Color.rgb(255, 255, 128)
                94 -> curFg = Color.rgb(128, 160, 255)
                95 -> curFg = Color.rgb(255, 128, 255)
                96 -> curFg = Color.rgb(128, 255, 255)
                97 -> curFg = Color.WHITE
            }
        }
    }

    /**
     * Renderiza a grade inteira como HTML monoespaçado para exibir em TextView.
     */
    fun renderHtml(): String {
        val sb = StringBuilder()
        sb.append("<html><body>")

        // Última linha com conteúdo (evita altura excessiva de linhas vazias no fim)
        var lastContentRow = rows - 1
        while (lastContentRow > 0 && isRowBlank(lastContentRow)) lastContentRow--

        for (r in 0..lastContentRow) {
            var c = 0
            while (c < cols) {
                val color = fg[r][c]
                val isBold = bold[r][c]
                // Agrupar células consecutivas com mesmo estilo
                val segStart = c
                while (c < cols && fg[r][c] == color && bold[r][c] == isBold) c++
                val segment = buildString {
                    for (i in segStart until c) append(chars[r][i])
                }
                val colorHex = String.format("#%06X", 0xFFFFFF and color)
                sb.append("<span style=\"color:$colorHex")
                if (isBold) sb.append(";font-weight:bold")
                sb.append("\">")
                sb.append(escapeHtml(segment))
                sb.append("</span>")
            }
            if (r < lastContentRow) sb.append("<br/>")
        }

        sb.append("</body></html>")
        return sb.toString()
    }

    private fun isRowBlank(r: Int): Boolean {
        for (c in 0 until cols) if (chars[r][c] != ' ') return false
        return true
    }

    private fun escapeHtml(s: String): String {
        val out = StringBuilder()
        for (ch in s) {
            when (ch) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                ' ' -> out.append("&nbsp;")    // preservar alinhamento (Html colapsa espaços)
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    fun reset() {
        for (r in 0 until rows) clearRow(r)
        cursorRow = 0
        cursorCol = 0
        curFg = defaultFg
        curBold = false
        state = State.NORMAL
        csiParams.setLength(0)
    }
}

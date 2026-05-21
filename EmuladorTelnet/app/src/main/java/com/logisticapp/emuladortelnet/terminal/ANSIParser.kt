package com.logisticapp.emuladortelnet.terminal

import android.graphics.Color
import timber.log.Timber

/**
 * Parser de ANSI/VT100 escape sequences
 * Converte texto com cores ANSI em Spannable HTML para renderizar
 */
class ANSIParser {

    companion object {
        // ANSI Color Codes
        const val RESET = "\u001B[0m"
        const val BOLD = "\u001B[1m"
        const val FAINT = "\u001B[2m"
        const val ITALIC = "\u001B[3m"
        const val UNDERLINE = "\u001B[4m"
        const val BLINK = "\u001B[5m"
        const val REVERSE = "\u001B[7m"

        // Foreground Colors
        const val BLACK_FG = "\u001B[30m"
        const val RED_FG = "\u001B[31m"
        const val GREEN_FG = "\u001B[32m"
        const val YELLOW_FG = "\u001B[33m"
        const val BLUE_FG = "\u001B[34m"
        const val MAGENTA_FG = "\u001B[35m"
        const val CYAN_FG = "\u001B[36m"
        const val WHITE_FG = "\u001B[37m"

        // Bright Foreground Colors
        const val BRIGHT_BLACK_FG = "\u001B[90m"
        const val BRIGHT_RED_FG = "\u001B[91m"
        const val BRIGHT_GREEN_FG = "\u001B[92m"
        const val BRIGHT_YELLOW_FG = "\u001B[93m"
        const val BRIGHT_BLUE_FG = "\u001B[94m"
        const val BRIGHT_MAGENTA_FG = "\u001B[95m"
        const val BRIGHT_CYAN_FG = "\u001B[96m"
        const val BRIGHT_WHITE_FG = "\u001B[97m"

        // Background Colors
        const val BLACK_BG = "\u001B[40m"
        const val RED_BG = "\u001B[41m"
        const val GREEN_BG = "\u001B[42m"
        const val YELLOW_BG = "\u001B[43m"
        const val BLUE_BG = "\u001B[44m"
        const val MAGENTA_BG = "\u001B[45m"
        const val CYAN_BG = "\u001B[46m"
        const val WHITE_BG = "\u001B[47m"
    }

    /**
     * Data class para representar texto com estilos
     */
    data class StyledText(
        val text: String,
        val color: Int = Color.GREEN,      // Cor padrão terminal
        val backgroundColor: Int = Color.BLACK,
        val isBold: Boolean = false,
        val isUnderline: Boolean = false,
        val isItalic: Boolean = false
    )

    /**
     * Parse texto ANSI e retorna lista de StyledText
     */
    fun parse(input: String): List<StyledText> {
        val result = mutableListOf<StyledText>()
        val segments = mutableListOf<Segment>()

        // Regex para encontrar escape sequences
        val escapeRegex = Regex("\u001B\\[[0-9;]*m")
        var lastIndex = 0
        var currentColor = Color.GREEN
        var currentBgColor = Color.BLACK
        var isBold = false
        var isUnderline = false
        var isItalic = false

        escapeRegex.findAll(input).forEach { match ->
            // Adicionar texto antes do escape
            if (match.range.first > lastIndex) {
                val text = input.substring(lastIndex, match.range.first)
                if (text.isNotEmpty()) {
                    segments.add(
                        Segment(
                            text = text,
                            color = currentColor,
                            backgroundColor = currentBgColor,
                            isBold = isBold,
                            isUnderline = isUnderline,
                            isItalic = isItalic
                        )
                    )
                }
            }

            // Processar escape sequence
            val code = match.value
            val params = code.replace("\u001B[", "").replace("m", "").split(";")

            params.forEach { param ->
                when (param.toIntOrNull()) {
                    0 -> {
                        // Reset
                        currentColor = Color.GREEN
                        currentBgColor = Color.BLACK
                        isBold = false
                        isUnderline = false
                        isItalic = false
                    }
                    1 -> isBold = true
                    2 -> isBold = false
                    3 -> isItalic = true
                    4 -> isUnderline = true
                    7 -> {
                        // Reverse (swap foreground/background)
                        val temp = currentColor
                        currentColor = currentBgColor
                        currentBgColor = temp
                    }
                    // Foreground colors
                    30 -> currentColor = Color.BLACK
                    31 -> currentColor = Color.rgb(255, 0, 0)      // Red
                    32 -> currentColor = Color.rgb(0, 255, 0)      // Green
                    33 -> currentColor = Color.rgb(255, 255, 0)    // Yellow
                    34 -> currentColor = Color.rgb(0, 0, 255)      // Blue
                    35 -> currentColor = Color.rgb(255, 0, 255)    // Magenta
                    36 -> currentColor = Color.rgb(0, 255, 255)    // Cyan
                    37 -> currentColor = Color.WHITE
                    // Bright foreground colors
                    90 -> currentColor = Color.rgb(128, 128, 128)
                    91 -> currentColor = Color.rgb(255, 128, 128)
                    92 -> currentColor = Color.rgb(128, 255, 128)
                    93 -> currentColor = Color.rgb(255, 255, 128)
                    94 -> currentColor = Color.rgb(128, 128, 255)
                    95 -> currentColor = Color.rgb(255, 128, 255)
                    96 -> currentColor = Color.rgb(128, 255, 255)
                    97 -> currentColor = Color.WHITE
                    // Background colors
                    40 -> currentBgColor = Color.BLACK
                    41 -> currentBgColor = Color.rgb(255, 0, 0)
                    42 -> currentBgColor = Color.rgb(0, 255, 0)
                    43 -> currentBgColor = Color.rgb(255, 255, 0)
                    44 -> currentBgColor = Color.rgb(0, 0, 255)
                    45 -> currentBgColor = Color.rgb(255, 0, 255)
                    46 -> currentBgColor = Color.rgb(0, 255, 255)
                    47 -> currentBgColor = Color.WHITE
                }
            }

            lastIndex = match.range.last + 1
        }

        // Adicionar texto restante
        if (lastIndex < input.length) {
            val remainingText = input.substring(lastIndex)
            if (remainingText.isNotEmpty()) {
                segments.add(
                    Segment(
                        text = remainingText,
                        color = currentColor,
                        backgroundColor = currentBgColor,
                        isBold = isBold,
                        isUnderline = isUnderline,
                        isItalic = isItalic
                    )
                )
            }
        }

        // Converter segments em StyledText
        segments.forEach { segment ->
            result.add(
                StyledText(
                    text = segment.text,
                    color = segment.color,
                    backgroundColor = segment.backgroundColor,
                    isBold = segment.isBold,
                    isUnderline = segment.isUnderline,
                    isItalic = segment.isItalic
                )
            )
        }

        Timber.d("Parse ANSI: ${input.take(50)} -> ${segments.size} segments")
        return result
    }

    /**
     * Converter StyledText para HTML span (para exibição em TextView)
     */
    fun toHtmlSpan(styledText: List<StyledText>): String {
        val html = StringBuilder()
        html.append("<html><body style=\"font-family: monospace; background-color: #000000;\">")

        styledText.forEach { styled ->
            val style = mutableListOf<String>()

            // Adicionar cor
            val colorHex = String.format("#%06X", 0xFFFFFF and styled.color)
            style.add("color: $colorHex")

            // Adicionar background se não for preto
            if (styled.backgroundColor != Color.BLACK) {
                val bgHex = String.format("#%06X", 0xFFFFFF and styled.backgroundColor)
                style.add("background-color: $bgHex")
            }

            // Adicionar estilos
            if (styled.isBold) style.add("font-weight: bold")
            if (styled.isItalic) style.add("font-style: italic")
            if (styled.isUnderline) style.add("text-decoration: underline")

            val styleAttr = style.joinToString("; ")
            val text = styled.text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br/>")

            html.append("<span style=\"$styleAttr\">$text</span>")
        }

        html.append("</body></html>")
        return html.toString()
    }

    /**
     * Remover escape sequences (plain text)
     */
    fun stripANSI(input: String): String {
        return input.replace(Regex("\u001B\\[[0-9;]*m"), "")
    }

    /**
     * Classe interna para representar segment durante parsing
     */
    private data class Segment(
        val text: String,
        val color: Int,
        val backgroundColor: Int,
        val isBold: Boolean,
        val isUnderline: Boolean,
        val isItalic: Boolean
    )
}

package com.logisticapp.scanteconfig.config

/**
 * Converte entre o texto amigável que o usuário edita ("F1=SUBMIT", uma
 * tecla por linha) e o mapa keyCode->ação salvo no JSON (mesmos códigos que
 * o KeyMapResolver do ScanTE Browser usa: F1=131 .. F12=142).
 */
object KeyMapText {

    private const val F1_CODE = 131

    private fun codeForLabel(label: String): Int? {
        val n = label.trim().uppercase().removePrefix("F").toIntOrNull() ?: return null
        if (n !in 1..12) return null
        return F1_CODE + (n - 1)
    }

    private fun labelForCode(code: Int): String = "F${code - F1_CODE + 1}"

    fun parse(text: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || !line.contains("=")) return@forEach
            val (label, action) = line.split("=", limit = 2)
            val code = codeForLabel(label) ?: return@forEach
            val actionTrimmed = action.trim()
            if (actionTrimmed.isNotEmpty()) {
                result[code.toString()] = actionTrimmed
            }
        }
        return result
    }

    fun format(keyActions: Map<String, String>): String {
        return keyActions.entries
            .mapNotNull { (codeStr, action) -> codeStr.toIntOrNull()?.let { it to action } }
            .sortedBy { it.first }
            .joinToString("\n") { (code, action) -> "${labelForCode(code)}=$action" }
    }
}

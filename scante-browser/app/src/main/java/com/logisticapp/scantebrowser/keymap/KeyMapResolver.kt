package com.logisticapp.scantebrowser.keymap

/**
 * Resolve qual [KeyAction] corresponde a uma tecla (física ou botão virtual)
 * para um portal específico, a partir do mapeamento vindo da config.
 * Usado tanto pela interceptação de tecla física (Activity.dispatchKeyEvent)
 * quanto pela barra de teclas de função na tela — um único caminho de código
 * para os dois casos.
 */
class KeyMapResolver(private val keyActions: Map<String, Map<String, String>>) {

    fun resolve(portalId: String, keyCode: Int): KeyAction? {
        val raw = keyActions[portalId]?.get(keyCode.toString()) ?: return null
        return KeyAction.parse(raw)
    }

    /** Teclas de função (F1-F12) mapeadas para o portal, na ordem — para montar a barra na tela. */
    fun functionKeysFor(portalId: String): List<Int> {
        val mapped = keyActions[portalId]?.keys?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        return mapped.filter { it in F1..F12 }.sorted()
    }

    companion object {
        const val F1 = 131 // KeyEvent.KEYCODE_F1
        const val F12 = 142 // KeyEvent.KEYCODE_F1 + 11

        fun functionKeyLabel(keyCode: Int): String = "F${keyCode - F1 + 1}"
    }
}

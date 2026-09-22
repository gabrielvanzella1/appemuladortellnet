package com.logisticapp.scantebrowser.keymap

/**
 * Ação disparada por uma tecla (física ou botão virtual na barra de função).
 * Representada no config JSON como string simples (ex.: "SUBMIT", "GO_BACK",
 * "SWITCH:sap", "SCRIPT:document.forms[0].submit()") e convertida via [parse].
 */
sealed class KeyAction {
    object Submit : KeyAction()
    object GoBack : KeyAction()
    object Reload : KeyAction()
    data class SwitchPortal(val portalId: String) : KeyAction()
    data class RunScript(val script: String) : KeyAction()

    companion object {
        fun parse(rawInput: String): KeyAction? {
            // Aceita qualquer caixa (SUBMIT, submit, Submit) — quem edita pelo
            // ScanTE Config não tem por que saber que isso importa.
            val raw = rawInput.trim()
            val upper = raw.uppercase()
            return when {
                upper == "SUBMIT" -> Submit
                upper == "GO_BACK" -> GoBack
                upper == "RELOAD" -> Reload
                upper.startsWith("SWITCH:") -> SwitchPortal(raw.substringAfter(":"))
                upper.startsWith("SCRIPT:") -> RunScript(raw.substringAfter(":"))
                else -> null
            }
        }
    }
}

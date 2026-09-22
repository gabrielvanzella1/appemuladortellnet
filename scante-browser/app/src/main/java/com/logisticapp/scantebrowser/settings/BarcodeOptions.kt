package com.logisticapp.scantebrowser.settings

/**
 * Configuração do leitor de código de barras. Estrutura provisória — vai crescer
 * quando o sistema de configuração por portal (equivalente ao EZConfig da Honeywell)
 * for desenhado.
 */
data class BarcodeOptions(
    var actionAfterScan: String = "Nenhum", // "Nenhum" | "Enter" | "Tab" | "Enter + Tab"
    var removeCharsStart: Int = 0,
    var removeCharsEnd: Int = 0,
    var addTextBefore: String = "",
    var addTextAfter: String = ""
)

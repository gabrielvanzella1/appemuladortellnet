package com.logisticapp.emuladortelnet.settings

/**
 * Configurações do leitor de código de barras (Configurações > Dispositivos > Leitor).
 * Persistido como JSON em AppSettings.
 */
data class BarcodeOptions(
    var deviceType: String = "Honeywell",
    var actionAfterScan: String = "Nenhum",
    var removeCharsStart: Int = 0,
    var removeCharsEnd: Int = 0,
    var addTextBefore: String = "",
    var addTextAfter: String = "",
    var useKeyboardMapping: Boolean = true,
    var showOnStatusBar: Boolean = true
)

package com.logisticapp.emuladortelnet.settings

/**
 * Configurações de impressão (Configurações > Dispositivos > Configuração de impressão).
 * Persistido como JSON em AppSettings.
 */
data class PrintOptions(
    var printerType: String = "Padrão",
    var timeoutSeconds: Int = 5
)

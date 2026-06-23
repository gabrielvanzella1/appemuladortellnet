package com.logisticapp.emuladortelnet.settings

/**
 * Opções de transliteração/charset (Configurações > Emulação > Transliteração).
 * Persistido como JSON em AppSettings.
 */
data class TransliterationOptions(
    // Caracteres de 16 bits
    var utf8Encoding: Boolean = false,

    // Caracteres de 8 bits
    var host8bit: Boolean = true,
    var allowLowercase: Boolean = true,
    var hostCharset: String = "Padrão (Latim 1)",
    var nationalTranslit: String = "Padrão (sem transliteração)",
    var useSiso: Boolean = false
)

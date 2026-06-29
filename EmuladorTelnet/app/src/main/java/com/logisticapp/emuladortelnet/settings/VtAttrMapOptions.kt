package com.logisticapp.emuladortelnet.settings

data class VtAttrMapOptions(
    val boldMode: String = "Negrito",        // "Negrito", "Cor brilhante", "Negrito+Cor", "Nenhum"
    val underlineEnabled: Boolean = true,    // SGR 4 = sublinhado ativo
    val blinkMode: String = "Nenhum"         // "Negrito", "Cor brilhante", "Nenhum"
)

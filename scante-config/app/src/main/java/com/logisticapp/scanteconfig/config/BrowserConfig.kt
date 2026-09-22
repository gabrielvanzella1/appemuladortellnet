package com.logisticapp.scanteconfig.config

/** Espelha o BrowserConfig.kt do ScanTE Browser — mesmo schema de JSON. */
data class BrowserConfig(
    val portals: List<Portal> = emptyList(),
    val keyActions: Map<String, Map<String, String>> = emptyMap()
)

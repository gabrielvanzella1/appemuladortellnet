package com.logisticapp.scanteconfig.config

/**
 * Mesmo formato de dados do Portal.kt do ScanTE Browser — os dois apps
 * trocam configuração por JSON, então os campos precisam bater exatamente.
 */
data class Portal(
    val id: String,
    val name: String,
    val homepage: String,
    val useWideViewport: Boolean = true,
    val textScalingPercent: Int = 100,
    val enableZoomControls: Boolean = false,
    val initialScale: Int = 0,
    val domInjectionScript: String? = null
)

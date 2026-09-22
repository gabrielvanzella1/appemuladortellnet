package com.logisticapp.scantebrowser.portal

/**
 * Um "portal" é uma aba independente do browser corporativo — mesmo conceito
 * usado pelo Honeywell Enterprise Browser (SAP, SEPARAÇÃO, etc no XML real do
 * cliente): nome, URL própria, config de renderização própria.
 */
data class Portal(
    val id: String,
    val name: String,
    val homepage: String,
    val useWideViewport: Boolean = true,
    val textScalingPercent: Int = 100,
    /** Mostra os botões +/- de zoom na tela (fora dos gestos de pinça, que continuam ativos). */
    val enableZoomControls: Boolean = false,
    /** Zoom inicial da página em %; 0 = automático (o WebView decide). */
    val initialScale: Int = 0,
    /** JS injetado via evaluateJavascript() após a página carregar (onPageFinished). Opcional. */
    val domInjectionScript: String? = null
)

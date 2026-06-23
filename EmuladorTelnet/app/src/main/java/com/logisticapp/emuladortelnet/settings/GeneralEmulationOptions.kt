package com.logisticapp.emuladortelnet.settings

/**
 * Opções gerais de emulação (Configurações > Emulação > Geral).
 * Persistido como JSON em AppSettings.
 */
data class GeneralEmulationOptions(
    var destructiveBackspace: Boolean = false,   // BS apaga o char na posição anterior
    var captureOnCr: String = "LF",              // O que adicionar ao receber CR: "Desativado", "LF", "CR+LF"
    var scrollLength: Int = 32,                  // Comprimento do scroll-back em páginas
    var initialWidth: Int = 80,                  // Largura inicial da tela (colunas)
    var initialHeight: Int = 24                  // Altura inicial da tela (linhas)
)

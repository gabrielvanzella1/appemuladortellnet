package com.logisticapp.emuladortelnet.settings

/**
 * Opções de emulação VT (Configurações > Emulação > VT Opções).
 * Persistido como JSON em AppSettings.
 */
data class VtOptions(
    var echoMode: Boolean = false,
    var scrollMode: Boolean = true,                     // Modo ROLO
    var lineMode: String = "Desativado",                // Desativado | Local | Remoto
    var addLfToCr: Boolean = false,                     // Adicionar LFs a CRs
    var noColumn81: Boolean = false,                    // Nenhuma coluna 81
    var backspaceAction: String = "BS",                 // BS | DEL
    var answerbackString: String = "",                  // String de resposta
    var vtDaAlias: String = "VT100",                    // VT52 | VT100 | VT220 | VT320 | ANSI
    var f5PuttySequence: Boolean = false,               // F5 envia sequência PuTTY
    var silenceHostAlarm: Boolean = false,              // Silenciar alarme do host
    var maxConsecutiveAlarms: String = "Max",           // Max | 1 | 2 | 3 | 5 | 10 | 25 | 50
    var ignoreUnknownEscapes: Boolean = false           // Ignorar sequências de escape desconhecidas
)

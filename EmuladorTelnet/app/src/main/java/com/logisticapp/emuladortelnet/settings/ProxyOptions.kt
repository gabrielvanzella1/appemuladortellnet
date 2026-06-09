package com.logisticapp.emuladortelnet.settings

/**
 * Opcoes da area "Servidor proxy" (Configuracoes > Comunicacao > Servidor proxy).
 * Persistido como JSON em AppSettings.
 */
data class ProxyOptions(
    var useServer: Boolean = false,
    var address: String = "",
    var port: String = "30855",
    var secureComm: Boolean = false,
    // Proxy mantem conexao com o host em segundos quando:
    var keepOnUserDisconnect: String = "0",
    var keepOnConnectionLost: String = "300"
)

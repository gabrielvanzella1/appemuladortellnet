package com.logisticapp.emuladortelnet.settings

/**
 * Opcoes da area "Servidor proxy" (Configuracoes > Comunicacao > Servidor proxy).
 * Persistido como JSON em AppSettings.
 */
data class ProxyOptions(
    val useServer: Boolean = false,
    val address: String = "",
    val port: String = "3128",
    val secureComm: Boolean = false,
    val username: String = "",                // usuario para autenticacao Basic no proxy
    val password: String = "",                // senha para autenticacao Basic no proxy
    val keepOnUserDisconnect: String = "0",   // segundos para manter sessao apos desconexao
    val keepOnConnectionLost: String = "300"  // segundos para manter sessao se conexao cair
)

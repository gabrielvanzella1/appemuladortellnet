package com.logisticapp.emuladortelnet.settings

/**
 * Opcoes da area "Telnet Opcoes" (Configuracoes > Comunicacao > Telnet Opcoes).
 * Persistido como JSON em AppSettings.
 */
data class TelnetOptions(
    // Conexao
    var serverAddress: String = "",
    var terminalType: String = "vt220",
    var lineTerminator: String = "CR+LF",   // CR+LF | CR | LF
    var useIpForBrk: Boolean = false,
    var binaryMode: Boolean = false,
    var simulateParity: Boolean = false,
    var keepAliveType: String = "TCP",      // TCP | NVT | Off
    var keepAliveInterval: String = "",

    // Login Telnet (automacao de login)
    var waitLoginPrompt: String = "",
    var loginWith: String = "",
    var waitPasswordPrompt: String = "",
    var password: String = "",
    var waitCommandPrompt: String = "",
    var doCommand: String = "",

    // Sockets seguros (SSL)
    var useSsl: Boolean = false,
    var authServerCert: Boolean = false,
    var clientCertFile: String = "",
    var clientCertPassword: String = "",

    // Conexao SSH
    var useSsh: Boolean = false,
    var sshServer: String = "",
    var sshUsername: String = "",
    var sshPassword: String = "",
    var sshPrivateKey: String = "",
    var sshKeepAlive: String = "0"
)

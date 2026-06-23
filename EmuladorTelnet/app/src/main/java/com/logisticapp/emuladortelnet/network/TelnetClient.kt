package com.logisticapp.emuladortelnet.network

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.nio.charset.Charset
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Cliente Telnet com suporte a TCP Socket real
 * Implementa RFC 854 (Telnet Protocol) com negociação IAC correta
 */
class TelnetClient {

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    private var charset: Charset = Charsets.ISO_8859_1
    private var terminalType = "VT100"   // respondido na negociacao IAC TERMINAL-TYPE
    private var terminalWidth = 80       // largura da tela (NAWS)
    private var terminalHeight = 24      // altura da tela (NAWS)
    private var binaryMode = false       // negociar transmissao binaria 8-bit
    private var simulateParity = false   // mascarar o 8o bit (paridade) dos dados recebidos
    private var keepAliveType = "TCP"    // TCP | NVT | Desligado

    // SSL/TLS
    private var useSsl = false
    private var sslCertBytes: ByteArray? = null   // bytes do .p12/.pfx do certificado cliente
    private var sslCertPassword: String = ""

    // Proxy
    private var useProxy = false
    private var proxyHost = ""
    private var proxyPort = 30855
    private var proxySecure = false   // HTTPS CONNECT em vez de HTTP CONNECT

    // SSH
    private var useSsh = false
    private var sshHost = ""
    private var sshPort = 22
    private var sshUsername = ""
    private var sshPassword = ""
    private var sshPrivateKey: ByteArray? = null   // bytes da chave privada (PEM/OpenSSH)
    private var sshKeepAliveSec = 0
    private var sshSession: Session? = null
    private var sshChannel: ChannelShell? = null

    companion object {
        // IAC = Interpret As Command (255)
        const val IAC  = 255
        const val WILL = 251
        const val WONT = 252
        const val DO   = 253
        const val DONT = 254
        const val SB   = 250  // Subnegotiation Begin
        const val SE   = 240  // Subnegotiation End

        // Telnet Options
        const val OPT_BINARY        = 0   // Transmit Binary (RFC 856)
        const val OPT_ECHO          = 1
        const val OPT_SGA           = 3   // Suppress Go Ahead
        const val OPT_TERMINAL_TYPE = 24
        const val OPT_NAWS          = 31  // Negotiate About Window Size
    }

    fun setCharset(name: String) {
        charset = try { Charset.forName(name) } catch (e: Exception) { Charsets.ISO_8859_1 }
    }

    /** Define o tamanho da tela negociado via NAWS (largura x altura). */
    fun setScreenSize(width: Int, height: Int) {
        terminalWidth = width
        terminalHeight = height
    }

    /** Define o tipo de terminal informado ao servidor (ex: VT100, VT220, ANSI). */
    fun setTerminalType(type: String) {
        if (type.isNotBlank()) terminalType = type.trim().uppercase()
    }

    /** Ativa/desativa a negociacao de transmissao binaria 8-bit. */
    fun setBinaryMode(enabled: Boolean) {
        binaryMode = enabled
    }

    /** Simular paridade: mascara o 8o bit (paridade) dos bytes recebidos (dados 7-bit). */
    fun setSimulateParity(enabled: Boolean) {
        simulateParity = enabled
    }

    /** Tipo de keep-alive: TCP (socket), NVT (IAC NOP periodico) ou Desligado. */
    fun setKeepAliveType(type: String) {
        keepAliveType = type
    }

    /** Configura SSL: ativa TLS e, opcionalmente, certificado cliente (.p12). */
    fun setSsl(enabled: Boolean, certBytes: ByteArray? = null, certPassword: String = "") {
        useSsl = enabled
        sslCertBytes = certBytes
        sslCertPassword = certPassword
    }

    /** Configura SSH: host, porta, credenciais e chave privada. */
    fun setSshConfig(
        enabled: Boolean, host: String, port: Int,
        username: String, password: String,
        privateKeyBytes: ByteArray? = null,
        keepAliveSec: Int = 0
    ) {
        useSsh = enabled
        sshHost = host.ifBlank { sshHost }
        sshPort = if (port > 0) port else 22
        sshUsername = username
        sshPassword = password
        sshPrivateKey = privateKeyBytes
        sshKeepAliveSec = keepAliveSec
    }

    /** Configura proxy HTTP/HTTPS CONNECT para rotear a conexão. */
    fun setProxy(enabled: Boolean, host: String, port: Int, secure: Boolean) {
        useProxy = enabled
        proxyHost = host
        proxyPort = if (port > 0) port else 30855
        proxySecure = secure
    }

    /**
     * Abre socket ao proxy e faz HTTP CONNECT para tunelar a conexão ao destino.
     * Retorna o socket tunelado pronto para uso (streams abertas).
     */
    private fun connectViaProxy(destHost: String, destPort: Int): Socket {
        val rawProxy: Socket = if (proxySecure) {
            val sslCtx = buildSslContext()
            sslCtx.socketFactory.createSocket(proxyHost, proxyPort)
        } else {
            Socket(proxyHost, proxyPort)
        }
        rawProxy.soTimeout = 20_000
        val out = rawProxy.getOutputStream()
        val inp = rawProxy.getInputStream()
        // Envia HTTP CONNECT
        val req = "CONNECT $destHost:$destPort HTTP/1.1\r\nHost: $destHost:$destPort\r\n\r\n"
        out.write(req.toByteArray(Charsets.US_ASCII))
        out.flush()
        // Lê resposta HTTP (até linha em branco)
        val response = StringBuilder()
        var cur: Int
        var lineBreaks = 0
        while (true) {
            cur = inp.read()
            if (cur < 0) break
            response.append(cur.toChar())
            if (cur == '\n'.code) {
                lineBreaks++
                if (lineBreaks >= 2) break   // cabeçalho terminou
            } else if (cur != '\r'.code) {
                lineBreaks = 0
            }
        }
        val respStr = response.toString()
        Timber.d("Proxy CONNECT resposta: ${respStr.take(80)}")
        if (!respStr.contains("200")) {
            rawProxy.close()
            throw Exception("Proxy rejeitou CONNECT: ${respStr.substringBefore('\n')}")
        }
        rawProxy.soTimeout = 0
        return rawProxy
    }

    /** Cria SSLContext aceitando qualquer certificado servidor (sem validação de CA). */
    private fun buildSslContext(): SSLContext {
        // TrustManager que aceita qualquer certificado do servidor
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val ctx = SSLContext.getInstance("TLS")

        // Se há certificado cliente, configura KeyManager (autenticação mútua)
        val kmf = sslCertBytes?.let { certData ->
            try {
                val ks = KeyStore.getInstance("PKCS12")
                ks.load(certData.inputStream(), sslCertPassword.toCharArray())
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).also {
                    it.init(ks, sslCertPassword.toCharArray())
                }
            } catch (e: Exception) {
                Timber.w("Certificado cliente inválido: ${e.message}")
                null
            }
        }

        ctx.init(kmf?.keyManagers, trustAll, null)
        return ctx
    }

    /** Envia IAC NOP (No Operation) para manter a conexao viva (keep-alive NVT). */
    suspend fun sendNop(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val out = outputStream ?: return@withContext Result.failure(Exception("Nao conectado"))
            out.write(byteArrayOf(IAC.toByte(), 241.toByte()))  // IAC NOP
            out.flush()
            Result.success("NOP")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connect(host: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (useSsh) {
                return@withContext connectSsh()
            }
            Timber.d("Conectando a $host:$port (ssl=$useSsl, proxy=$useProxy)")
            // Proxy: abre túnel HTTP CONNECT antes de SSL ou conexão direta
            val baseSocket: Socket = if (useProxy && proxyHost.isNotBlank()) {
                Timber.d("Usando proxy $proxyHost:$proxyPort")
                connectViaProxy(host, port)
            } else {
                Socket(host, port)
            }
            val rawSocket: Socket = if (useSsl) {
                val sslCtx = buildSslContext()
                val sslSocket = sslCtx.socketFactory.createSocket(
                    baseSocket, host, port, true
                ) as SSLSocket
                sslSocket.startHandshake()
                Timber.d("TLS handshake OK: ${sslSocket.session.protocol} ${sslSocket.session.cipherSuite}")
                sslSocket
            } else {
                baseSocket
            }
            socket = rawSocket.also { s ->
                s.soTimeout = 0
                s.keepAlive = keepAliveType.equals("TCP", ignoreCase = true)
                s.tcpNoDelay = true
                inputStream  = s.getInputStream()
                outputStream = s.getOutputStream()
                isConnected  = true
            }
            // Modo binario: negociar proativamente
            if (binaryMode) {
                outputStream?.let { out ->
                    out.write(byteArrayOf(IAC.toByte(), WILL.toByte(), OPT_BINARY.toByte()))
                    out.write(byteArrayOf(IAC.toByte(), DO.toByte(), OPT_BINARY.toByte()))
                    out.flush()
                }
            }
            Timber.d("Conectado com sucesso a $host:$port (binario=$binaryMode, ssl=$useSsl)")
            Result.success("Conectado a $host:$port")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao conectar")
            isConnected = false
            Result.failure(e)
        }
    }

    /** Conecta via SSH usando JSch, abre shell channel e redireciona streams. */
    private fun connectSsh(): Result<String> {
        return try {
            val jsch = JSch()
            // Chave privada (PEM/OpenSSH) se fornecida
            sshPrivateKey?.let { keyBytes ->
                jsch.addIdentity("key", keyBytes, null, sshPassword.toByteArray())
            }
            val session = jsch.getSession(sshUsername, sshHost, sshPort)
            if (sshPrivateKey == null) {
                session.setPassword(sshPassword)
            }
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive")
            if (sshKeepAliveSec > 0) {
                session.setServerAliveInterval(sshKeepAliveSec * 1000)
            }
            session.connect(30_000)
            sshSession = session

            val channel = session.openChannel("shell") as ChannelShell
            channel.setPtyType(terminalType.lowercase().ifBlank { "vt100" })
            channel.setPtySize(80, 24, 0, 0)
            channel.connect(15_000)
            sshChannel = channel

            inputStream  = channel.inputStream
            outputStream = channel.outputStream
            isConnected  = true
            Timber.d("SSH conectado a $sshHost:$sshPort como $sshUsername")
            Result.success("Conectado via SSH a $sshHost:$sshPort")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao conectar SSH")
            isConnected = false
            Result.failure(e)
        }
    }

    /**
     * Leitura bloqueante de dados crus do servidor.
     * Processa bytes IAC inline e retorna apenas texto visível.
     */
    suspend fun readData(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val input  = inputStream  ?: return@withContext Result.failure(Exception("Nao conectado"))
            val output = outputStream ?: return@withContext Result.failure(Exception("Nao conectado"))

            val buf = ByteArray(4096)
            val n = input.read(buf)   // bloqueia até dados chegarem

            if (n < 0) {
                isConnected = false
                return@withContext Result.failure(Exception("Conexao fechada pelo servidor"))
            }

            val textBytes = mutableListOf<Byte>()
            var i = 0

            while (i < n) {
                val b = buf[i].toInt() and 0xFF

                if (b == IAC) {
                    if (i + 1 >= n) { i++; continue }
                    val cmd = buf[i + 1].toInt() and 0xFF

                    when (cmd) {
                        WILL -> {
                            val opt = if (i + 2 < n) buf[i + 2].toInt() and 0xFF else -1
                            if (opt >= 0) {
                                when (opt) {
                                    OPT_SGA  -> sendRaw(output, byteArrayOf(IAC.toByte(), DO.toByte(),   opt.toByte()))
                                    OPT_ECHO -> sendRaw(output, byteArrayOf(IAC.toByte(), DONT.toByte(), opt.toByte()))
                                    OPT_BINARY -> sendRaw(output, byteArrayOf(IAC.toByte(),
                                        (if (binaryMode) DO else DONT).toByte(), opt.toByte()))
                                    else      -> sendRaw(output, byteArrayOf(IAC.toByte(), DONT.toByte(), opt.toByte()))
                                }
                                Timber.d("IAC WILL $opt -> respondido")
                                i += 3
                            } else { i += 2 }
                        }
                        WONT -> {
                            i += if (i + 2 < n) 3 else 2
                        }
                        DO -> {
                            val opt = if (i + 2 < n) buf[i + 2].toInt() and 0xFF else -1
                            if (opt >= 0) {
                                when (opt) {
                                    OPT_TERMINAL_TYPE -> {
                                        sendRaw(output, byteArrayOf(IAC.toByte(), WILL.toByte(), opt.toByte()))
                                    }
                                    OPT_NAWS -> {
                                        sendRaw(output, byteArrayOf(IAC.toByte(), WILL.toByte(), opt.toByte()))
                                        sendRaw(output, byteArrayOf(
                                            IAC.toByte(), SB.toByte(), OPT_NAWS.toByte(),
                                            (terminalWidth shr 8).toByte(), terminalWidth.toByte(),
                                            (terminalHeight shr 8).toByte(), terminalHeight.toByte(),
                                            IAC.toByte(), SE.toByte()
                                        ))
                                    }
                                    OPT_BINARY -> sendRaw(output, byteArrayOf(IAC.toByte(),
                                        (if (binaryMode) WILL else WONT).toByte(), opt.toByte()))
                                    else -> sendRaw(output, byteArrayOf(IAC.toByte(), WONT.toByte(), opt.toByte()))
                                }
                                Timber.d("IAC DO $opt -> respondido")
                                i += 3
                            } else { i += 2 }
                        }
                        DONT -> {
                            i += if (i + 2 < n) 3 else 2
                        }
                        SB -> {
                            // Subnegociação: ler até IAC SE
                            i += 2
                            val subOpt = if (i < n) buf[i].toInt() and 0xFF else -1
                            // Pular até encontrar IAC SE
                            while (i < n) {
                                val cur  = buf[i].toInt() and 0xFF
                                val next = if (i + 1 < n) buf[i + 1].toInt() and 0xFF else -1
                                if (cur == IAC && next == SE) {
                                    i += 2
                                    break
                                }
                                i++
                            }
                            // Se foi requisição de TERMINAL-TYPE, enviar o tipo configurado
                            if (subOpt == OPT_TERMINAL_TYPE) {
                                val response = terminalType.toByteArray(Charsets.US_ASCII)
                                val packet = ByteArray(6 + response.size)
                                packet[0] = IAC.toByte(); packet[1] = SB.toByte()
                                packet[2] = OPT_TERMINAL_TYPE.toByte()
                                packet[3] = 0  // IS
                                response.copyInto(packet, 4)
                                packet[4 + response.size] = IAC.toByte()
                                packet[5 + response.size] = SE.toByte()
                                sendRaw(output, packet)
                                Timber.d("Respondido TERMINAL-TYPE: $terminalType")
                            }
                        }
                        IAC -> {
                            // IAC IAC = byte literal 0xFF
                            textBytes.add(0xFF.toByte())
                            i += 2
                        }
                        else -> { i += 2 }
                    }
                } else {
                    // Byte de texto normal — tratar CR
                    if (b == 13) { // CR
                        val next = if (i + 1 < n) buf[i + 1].toInt() and 0xFF else -1
                        when (next) {
                            10 -> {
                                // CR LF → apenas LF
                                textBytes.add(10)
                                i += 2
                            }
                            0 -> {
                                // CR NUL → nova linha
                                textBytes.add(10)
                                i += 2
                            }
                            else -> {
                                textBytes.add(10)
                                i++
                            }
                        }
                    } else if (b != 0) {
                        // Simular paridade: mascara o 8o bit (mantem so 7 bits de dados)
                        val outByte = if (simulateParity) (b and 0x7F) else b
                        textBytes.add(outByte.toByte())
                        i++
                    } else {
                        i++
                    }
                }
            }

            val text = String(textBytes.toByteArray(), charset)
            if (text.isNotEmpty()) Timber.d("Recebido ${text.length} chars: ${text.take(60).replace("\n","↵")}")
            Result.success(text)

        } catch (e: SocketException) {
            Timber.w("Socket fechado: ${e.message}")
            isConnected = false
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao ler dados")
            Result.failure(e)
        }
    }

    private fun sendRaw(output: OutputStream, bytes: ByteArray) {
        try {
            output.write(bytes)
            output.flush()
        } catch (e: Exception) {
            Timber.w("Erro ao enviar IAC: ${e.message}")
        }
    }

    /**
     * Enviar comando — Telnet usa CRLF obrigatório (RFC 854)
     */
    suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val output = outputStream ?: return@withContext Result.failure(Exception("Nao conectado"))
            val bytes = (command + "\r\n").toByteArray(charset)
            output.write(bytes)
            output.flush()
            Timber.d("Enviado: $command")
            Result.success("Enviado")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao enviar comando")
            isConnected = false
            Result.failure(e)
        }
    }

    /**
     * Enviar bytes brutos (ex: tecla ESC, F1-F12 VT100)
     */
    suspend fun sendRawBytes(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val output = outputStream ?: return@withContext Result.failure(Exception("Nao conectado"))
            output.write(bytes)
            output.flush()
            Result.success("Enviado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect(): Result<String> = withContext(Dispatchers.IO) {
        isConnected = false
        try { inputStream?.close()  } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { sshChannel?.disconnect() } catch (_: Exception) {}
        try { sshSession?.disconnect() } catch (_: Exception) {}
        sshChannel = null; sshSession = null
        try { socket?.close()       } catch (_: Exception) {}
        Timber.d("Desconectado")
        Result.success("Desconectado")
    }

    fun isConnectedStatus(): Boolean {
        if (!isConnected) return false
        // SSH: verifica canal
        if (useSsh) return sshChannel?.isConnected == true
        return socket?.isConnected == true && socket?.isClosed == false
    }
}

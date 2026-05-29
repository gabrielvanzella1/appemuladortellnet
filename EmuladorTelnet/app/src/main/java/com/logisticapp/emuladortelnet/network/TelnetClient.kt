package com.logisticapp.emuladortelnet.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.nio.charset.Charset

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
        const val OPT_ECHO          = 1
        const val OPT_SGA           = 3   // Suppress Go Ahead
        const val OPT_TERMINAL_TYPE = 24
        const val OPT_NAWS          = 31  // Negotiate About Window Size
    }

    fun setCharset(name: String) {
        charset = try { Charset.forName(name) } catch (e: Exception) { Charsets.ISO_8859_1 }
    }

    suspend fun connect(host: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.d("Conectando a $host:$port")
            socket = Socket(host, port).also { s ->
                s.soTimeout = 0       // leitura bloqueante
                s.keepAlive = true
                s.tcpNoDelay = true
                inputStream  = s.getInputStream()
                outputStream = s.getOutputStream()
                isConnected  = true
            }
            Timber.d("Conectado com sucesso a $host:$port")
            Result.success("Conectado a $host:$port")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao conectar")
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
                                        // Informar tamanho de tela 80x24
                                        sendRaw(output, byteArrayOf(IAC.toByte(), WILL.toByte(), opt.toByte()))
                                        sendRaw(output, byteArrayOf(
                                            IAC.toByte(), SB.toByte(), OPT_NAWS.toByte(),
                                            0, 80, 0, 24,
                                            IAC.toByte(), SE.toByte()
                                        ))
                                    }
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
                            // Se foi requisição de TERMINAL-TYPE, enviar VT100
                            if (subOpt == OPT_TERMINAL_TYPE) {
                                val response = "VT100".toByteArray(Charsets.US_ASCII)
                                val packet = ByteArray(6 + response.size)
                                packet[0] = IAC.toByte(); packet[1] = SB.toByte()
                                packet[2] = OPT_TERMINAL_TYPE.toByte()
                                packet[3] = 0  // IS
                                response.copyInto(packet, 4)
                                packet[4 + response.size] = IAC.toByte()
                                packet[5 + response.size] = SE.toByte()
                                sendRaw(output, packet)
                                Timber.d("Respondido TERMINAL-TYPE: VT100")
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
                        // Ignorar NUL
                        textBytes.add(b.toByte())
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
        try { socket?.close()       } catch (_: Exception) {}
        Timber.d("Desconectado")
        Result.success("Desconectado")
    }

    fun isConnectedStatus(): Boolean =
        isConnected && socket?.isConnected == true && socket?.isClosed == false
}

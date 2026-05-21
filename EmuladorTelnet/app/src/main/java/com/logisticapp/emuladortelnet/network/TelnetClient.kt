package com.logisticapp.emuladortelnet.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketException

/**
 * Cliente Telnet com suporte a TCP Socket real
 * Implementa RFC 854 (Telnet Protocol)
 */
class TelnetClient {

    private var socket: Socket? = null
    private var inputReader: BufferedReader? = null
    private var outputWriter: PrintWriter? = null
    private var isConnected = false

    // RFC 854 - Telnet Protocol
    companion object {
        // IAC (Interpret As Command)
        const val IAC: Int = 255

        // Commands
        const val WILL: Int = 251
        const val WONT: Int = 252
        const val DO: Int = 253
        const val DONT: Int = 254

        // Telnet Options
        const val SUPPRESS_GO_AHEAD: Int = 3
        const val ECHO: Int = 1
        const val TERMINAL_TYPE: Int = 24
    }

    /**
     * Conectar a um servidor Telnet
     */
    suspend fun connect(host: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.d("Conectando a $host:$port")

            // Criar socket
            socket = Socket(host, port)
            socket?.let {
                inputReader = BufferedReader(InputStreamReader(it.inputStream, Charsets.UTF_8))
                outputWriter = PrintWriter(it.outputStream, true)
                isConnected = true

                Timber.d("Conectado com sucesso a $host:$port")
                Result.success("Conectado a $host:$port")
            } ?: Result.failure(Exception("Falha ao criar socket"))

        } catch (e: Exception) {
            Timber.e(e, "Erro ao conectar")
            isConnected = false
            Result.failure(e)
        }
    }

    /**
     * Enviar comando/texto
     */
    suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isConnected) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            Timber.d("Enviando: $command")
            outputWriter?.println(command)
            outputWriter?.flush()

            Result.success("Comando enviado")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao enviar comando")
            Result.failure(e)
        }
    }

    /**
     * Receber dados do servidor (blocking)
     */
    suspend fun readData(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isConnected) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            val data = inputReader?.readLine()
            if (data != null) {
                Timber.d("Recebido: $data")
                Result.success(data)
            } else {
                // EOF reached
                isConnected = false
                Result.failure(Exception("Conexão fechada pelo servidor"))
            }
        } catch (e: SocketException) {
            Timber.w("Socket exceção ao ler: ${e.message}")
            isConnected = false
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao ler dados")
            Result.failure(e)
        }
    }

    /**
     * Ler dados disponíveis (non-blocking)
     */
    suspend fun readAvailable(): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isConnected || inputReader == null) {
                return@withContext Result.failure(Exception("Não conectado"))
            }

            val lines = mutableListOf<String>()

            // Tentar ler linhas disponíveis (timeout curto)
            while (inputReader?.ready() == true) {
                val line = inputReader?.readLine()
                if (line != null) {
                    lines.add(line)
                    Timber.d("Linha disponível: $line")
                } else {
                    // EOF
                    isConnected = false
                    break
                }
            }

            Result.success(lines)
        } catch (e: SocketException) {
            Timber.w("Socket exceção: ${e.message}")
            isConnected = false
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Erro ao ler disponível")
            Result.failure(e)
        }
    }

    /**
     * Processar negociação Telnet (RFC 854)
     */
    suspend fun handleTelnetNegotiation(data: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            var i = 0
            val response = StringBuilder()

            while (i < data.size) {
                val byte = data[i].toInt()
                if (byte == IAC && i + 1 < data.size) {
                    val command = data[i + 1].toInt()
                    when (command) {
                        WILL -> {
                            // Server quer fazer algo
                            if (i + 2 < data.size) {
                                val option = data[i + 2].toInt()
                                response.append("Server WILL: $option\n")
                                Timber.d("Recebido WILL $option")
                                i += 3
                            } else {
                                i += 2
                            }
                        }
                        WONT -> {
                            // Server não quer fazer algo
                            if (i + 2 < data.size) {
                                val option = data[i + 2].toInt()
                                response.append("Server WON'T: $option\n")
                                Timber.d("Recebido WON'T $option")
                                i += 3
                            } else {
                                i += 2
                            }
                        }
                        DO -> {
                            // Server quer que façamos algo
                            if (i + 2 < data.size) {
                                val option = data[i + 2].toInt()
                                response.append("Server DO: $option\n")
                                Timber.d("Recebido DO $option")
                                i += 3
                            } else {
                                i += 2
                            }
                        }
                        DONT -> {
                            // Server quer que não façamos algo
                            if (i + 2 < data.size) {
                                val option = data[i + 2].toInt()
                                response.append("Server DON'T: $option\n")
                                Timber.d("Recebido DON'T $option")
                                i += 3
                            } else {
                                i += 2
                            }
                        }
                        else -> {
                            Timber.d("Comando Telnet desconhecido: $command")
                            i += 2
                        }
                    }
                } else {
                    i++
                }
            }

            if (response.isNotEmpty()) {
                Result.success(response.toString())
            } else {
                Result.success("")
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao processar negociação")
            Result.failure(e)
        }
    }

    /**
     * Desconectar do servidor
     */
    suspend fun disconnect(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            isConnected = false
            inputReader?.close()
            outputWriter?.close()
            socket?.close()

            Timber.d("Desconectado")
            Result.success("Desconectado")
        } catch (e: Exception) {
            Timber.e(e, "Erro ao desconectar")
            Result.failure(e)
        }
    }

    /**
     * Verificar se está conectado
     */
    fun isConnectedStatus(): Boolean = isConnected && socket?.isConnected == true
}

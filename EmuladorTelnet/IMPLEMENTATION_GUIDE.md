# Próximas Etapas - Implementação Telnet

## 1. Real Telnet Connection (Fase atual bloqueada)

Uma vez que o APK for compilado com sucesso, as próximas etapas serão:

### 1.1 Socket Telnet Básico
```kotlin
// Em TelnetViewModel
private var socket: Socket? = null

fun connect(host: String, port: String) {
    viewModelScope.launch {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            
            socket = Socket(host, port.toInt())
            val inputStream = socket?.getInputStream()
            val outputStream = socket?.getOutputStream()
            
            _connectionState.value = ConnectionState.CONNECTED
            
            // Thread para ler dados do socket
            launch(Dispatchers.IO) {
                readFromSocket(inputStream)
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            addTerminalOutput("Erro: ${e.message}")
        }
    }
}

fun sendCommand(command: String) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            socket?.getOutputStream()?.write("$command\n".toByteArray())
        } catch (e: Exception) {
            addTerminalOutput("Erro ao enviar: ${e.message}")
        }
    }
}

private suspend fun readFromSocket(inputStream: InputStream?) {
    val buffer = ByteArray(4096)
    while (isActive) {
        try {
            val bytes = inputStream?.read(buffer) ?: break
            if (bytes > 0) {
                val text = String(buffer, 0, bytes)
                addTerminalOutput(text)
            }
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.ERROR
            break
        }
    }
}
```

### 1.2 TELNET Negotiation (RFC 854)
Telnet usa protocol de negociação com IAC (Interpret As Command):

```kotlin
// Constantes Telnet
private const val IAC = 255.toByte()     // Interpret As Command
private const val DONT = 254.toByte()
private const val DO = 253.toByte()
private const val WONT = 252.toByte()
private const val WILL = 251.toByte()
private const val SB = 250.toByte()      // Subnegotiation
private const val SE = 240.toByte()      // End subnegotiation

private const val TELOPT_ECHO = 1.toByte()
private const val TELOPT_SUPPRESS_GA = 3.toByte()
private const val TELOPT_NAWS = 31.toByte()  // Negotiated About Window Size
private const val TELOPT_TERMINAL_TYPE = 24.toByte()

// Processar comandos Telnet
private fun processTelnetCommand(data: ByteArray) {
    var i = 0
    while (i < data.size) {
        if (data[i] == IAC && i + 1 < data.size) {
            val command = data[i + 1]
            
            when (command) {
                DO -> {
                    // Host asking us to do something
                    if (i + 2 < data.size) {
                        respondToDo(data[i + 2])
                        i += 3
                    } else i++
                }
                WILL -> {
                    // Host will do something
                    if (i + 2 < data.size) {
                        i += 3
                    } else i++
                }
                DONT, WONT -> i += 3
                SB -> {
                    // Subnegotiation
                    val seIndex = data.indexOf(SE)
                    if (seIndex != -1) {
                        handleSubnegotiation(data.sliceArray(i+2 until seIndex))
                        i = seIndex + 1
                    } else i++
                }
                else -> i++
            }
        } else i++
    }
}

private fun respondToDo(option: Byte) {
    when (option) {
        TELOPT_SUPPRESS_GA -> {
            // Suppress Go-Ahead
            socket?.getOutputStream()?.write(byteArrayOf(IAC, WILL, TELOPT_SUPPRESS_GA))
        }
        TELOPT_ECHO -> {
            socket?.getOutputStream()?.write(byteArrayOf(IAC, WILL, TELOPT_ECHO))
        }
        TELOPT_NAWS -> {
            // Enviar tamanho da janela
            sendWindowSize()
        }
        TELOPT_TERMINAL_TYPE -> {
            sendTerminalType()
        }
    }
}

private fun sendWindowSize() {
    val width = 80.toByte()
    val height = 24.toByte()
    socket?.getOutputStream()?.write(
        byteArrayOf(
            IAC, SB, TELOPT_NAWS,
            0, width, 0, height,
            IAC, SE
        )
    )
}

private fun sendTerminalType() {
    val termType = "xterm".toByteArray()
    val response = ByteArray(4 + termType.size + 2)
    response[0] = IAC
    response[1] = SB
    response[2] = TELOPT_TERMINAL_TYPE
    response[3] = 0  // IS
    for (i in termType.indices) response[4 + i] = termType[i]
    response[response.size - 2] = IAC
    response[response.size - 1] = SE
    socket?.getOutputStream()?.write(response)
}
```

## 2. Autenticação

```kotlin
// Após conectar, pode ser necessário:
// 1. Aguardar prompt de login
// 2. Enviar username
// 3. Enviar password (pode não ecoar)

fun authenticate(username: String, password: String) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // Aguardar "login:" prompt
            delay(500)
            sendCommand(username)
            
            // Aguardar "password:" prompt  
            delay(500)
            sendCommand(password)
            
            addTerminalOutput("Autenticação enviada...")
        } catch (e: Exception) {
            addTerminalOutput("Erro na autenticação: ${e.message}")
        }
    }
}
```

## 3. Terminal Emulation (ANSI/VT100)

Muitos servidores Telnet enviam sequências de escape ANSI:

```kotlin
// Exemplo: ESC[31m = vermelho, ESC[0m = reset
private fun parseAnsiEscapes(input: String): SpannableString {
    val spannable = SpannableString(input)
    val ansiRegex = Regex("\\u001b\\[(\\d+)m")
    
    var offset = 0
    ansiRegex.findAll(input).forEach { match ->
        val code = match.groupValues[1].toInt()
        val color = when (code) {
            31 -> Color.RED
            32 -> Color.GREEN
            33 -> Color.YELLOW
            34 -> Color.BLUE
            else -> Color.WHITE
        }
        // Aplicar cor ao span...
    }
    
    return spannable
}
```

## 4. Integração com UI

```kotlin
// Modificar MainActivity para enviar comandos
class MainActivity : AppCompatActivity() {
    private lateinit var commandInput: EditText
    
    private fun setupCommandInput() {
        commandInput = findViewById(R.id.commandInput)
        commandInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && 
                event.action == KeyEvent.ACTION_DOWN) {
                val command = commandInput.text.toString()
                viewModel.sendCommand(command)
                commandInput.text.clear()
                return@setOnKeyListener true
            }
            false
        }
    }
}
```

## 5. Tratamento de Conexões Múltiplas (Futuro)

Para suportar múltiplas conexões simultâneas:

```kotlin
data class ActiveConnection(
    val id: String,
    val connection: TelnetConnection,
    val socket: Socket,
    val viewModel: TelnetViewModel
)

class ConnectionManager {
    private val connections = mutableMapOf<String, ActiveConnection>()
    
    fun createConnection(connection: TelnetConnection): String {
        val id = UUID.randomUUID().toString()
        // Setup...
        return id
    }
    
    fun getConnection(id: String): ActiveConnection? = connections[id]
}
```

## 6. Permissões Necessárias (AndroidManifest.xml)

Já configuradas:
- `INTERNET`: Conexões de rede
- `ACCESS_NETWORK_STATE`: Verificar estado da rede
- `READ/WRITE_EXTERNAL_STORAGE`: Logs, configurações

## 7. Testes

```kotlin
class TelnetViewModelTest {
    @Test
    fun testConnection() {
        val viewModel = TelnetViewModel()
        viewModel.connect("localhost", "23")
        
        assertEquals(ConnectionState.CONNECTING, viewModel.connectionState.value)
    }
}
```

---

**Dependências adicionais que podem ser necessárias**:
- `io.netty:netty-all` (para socket async)
- `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`

**Próximo passo após compilar APK**: 
1. Instalar em emulator/device
2. Implementar real Telnet connection
3. Testar com servidor Telnet público (telnet.towel.blinkenlights.nl)

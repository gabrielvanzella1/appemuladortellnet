package com.logisticapp.emuladortelnet.toolbar

/**
 * Um botao de barra de ferramentas: rotulo exibido + acao a executar.
 */
data class ToolbarButton(
    val label: String,
    val action: String
)

/**
 * Catalogo de acoes, barras padrao e botoes disponiveis para adicionar.
 */
object ToolbarCatalog {

    const val NUM_BARS = 4

    /**
     * Bytes a enviar ao servidor para a acao. Retorna null para acoes
     * especiais tratadas no app (CONNECT, DISCONNECT, BREAK, COPY, PASTE).
     */
    fun bytesFor(action: String): ByteArray? {
        val esc = 27.toByte()
        return when (action) {
            "UP"      -> byteArrayOf(esc, '['.code.toByte(), 'A'.code.toByte())
            "DOWN"    -> byteArrayOf(esc, '['.code.toByte(), 'B'.code.toByte())
            "RIGHT"   -> byteArrayOf(esc, '['.code.toByte(), 'C'.code.toByte())
            "LEFT"    -> byteArrayOf(esc, '['.code.toByte(), 'D'.code.toByte())
            "ESC"     -> byteArrayOf(esc)
            "ENTER"   -> byteArrayOf(13)
            "TAB"     -> byteArrayOf(9)
            "BACKTAB" -> byteArrayOf(esc, '['.code.toByte(), 'Z'.code.toByte())
            "HOME"    -> byteArrayOf(esc, '['.code.toByte(), 'H'.code.toByte())
            "END"     -> byteArrayOf(esc, '['.code.toByte(), 'F'.code.toByte())
            "DELCHAR" -> byteArrayOf(127)
            "INSERT"  -> byteArrayOf(esc, '['.code.toByte(), '2'.code.toByte(), '~'.code.toByte())
            "PREVS"   -> byteArrayOf(esc, '['.code.toByte(), '5'.code.toByte(), '~'.code.toByte())
            "NEXTS"   -> byteArrayOf(esc, '['.code.toByte(), '6'.code.toByte(), '~'.code.toByte())
            else -> when {
                action.startsWith("CTRL_") -> {
                    val ch = action.removePrefix("CTRL_").firstOrNull() ?: return null
                    byteArrayOf((ch.uppercaseChar().code - 64).toByte())  // Ctrl+A = 1
                }
                action.startsWith("TEXT:") ->
                    action.removePrefix("TEXT:").toByteArray(Charsets.ISO_8859_1)
                else -> null  // acao especial
            }
        }
    }

    /** Descricao curta da acao (mostrada na tela de configuracao, ao lado do rotulo). */
    fun describe(action: String): String = when {
        action.startsWith("TEXT:") -> "Texto \"${action.removePrefix("TEXT:")}\""
        action.startsWith("CTRL_") -> action.replace("_", "+").replaceFirstChar { 'C' } // CTRL_C -> Ctrl+C
        action == "UP" -> "Up"
        action == "DOWN" -> "Down"
        action == "LEFT" -> "Left"
        action == "RIGHT" -> "Right"
        action == "ESC" -> "Esc"
        action == "ENTER" -> "Enter"
        action == "HOME" -> "Home"
        action == "END" -> "End"
        action == "DELCHAR" -> "DelChar"
        action == "BACKTAB" -> "Backtab"
        action == "INSERT" -> "Insert mode"
        action == "PREVS" -> "PrevS"
        action == "NEXTS" -> "NextS"
        action == "COPY" -> "Copy"
        action == "PASTE" -> "Paste"
        action == "CONNECT" -> "Connect"
        action == "DISCONNECT" -> "Disconnect"
        action == "BREAK" -> "Break"
        else -> action
    }

    /** As 4 barras padrao (conforme telas do GlinkVT). */
    val defaultToolbars: List<List<ToolbarButton>> = listOf(
        listOf(
            ToolbarButton("↑", "UP"),
            ToolbarButton("Voltar", "ESC"),
            ToolbarButton("Sim", "TEXT:S"),
            ToolbarButton("Não", "TEXT:N")
        ),
        listOf(
            ToolbarButton("↓", "DOWN"),
            ToolbarButton("Ctrl+C", "CTRL_C"),
            ToolbarButton("Ctrl+I", "CTRL_I"),
            ToolbarButton("Ctrl+E", "CTRL_E")
        ),
        listOf(
            ToolbarButton("←", "LEFT"),
            ToolbarButton("Ctrl+K", "CTRL_K"),
            ToolbarButton("Ctrl+P", "CTRL_P"),
            ToolbarButton("Ctrl+Y", "CTRL_Y")
        ),
        listOf(
            ToolbarButton("→", "RIGHT"),
            ToolbarButton("Ctrl+W", "CTRL_W"),
            ToolbarButton("Ctrl+Z", "CTRL_Z"),
            ToolbarButton("Enter", "ENTER")
        )
    )

    /** Botoes disponiveis na tela "Adicionar botoes" (print 2). */
    val available: List<ToolbarButton> = listOf(
        ToolbarButton("Find", "HOME"),
        ToolbarButton("Select", "END"),
        ToolbarButton("PrevS", "PREVS"),
        ToolbarButton("NextS", "NEXTS"),
        ToolbarButton("Del", "DELCHAR"),
        ToolbarButton("←", "BACKTAB"),
        ToolbarButton("Copy", "COPY"),
        ToolbarButton("Paste", "PASTE"),
        ToolbarButton("Ins", "INSERT"),
        ToolbarButton("Conne...", "CONNECT"),
        ToolbarButton("Break", "BREAK"),
        ToolbarButton("Disco...", "DISCONNECT")
    )
}

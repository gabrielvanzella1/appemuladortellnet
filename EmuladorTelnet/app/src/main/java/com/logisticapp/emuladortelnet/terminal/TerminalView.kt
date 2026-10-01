package com.logisticapp.emuladortelnet.terminal

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatTextView

/**
 * TextView que tambem captura o teclado virtual e envia o que for digitado
 * diretamente ao servidor (sem campo de input separado), como num emulador real.
 *
 * Defina [onInput] para receber os bytes a enviar (texto, Enter, Backspace...).
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {

    /** Recebe os bytes digitados para enviar ao servidor. */
    var onInput: ((ByteArray) -> Unit)? = null

    /** Bytes enviados ao apertar ENTER (terminador de linha: CR, LF ou CR+LF). */
    var lineTerminator: ByteArray = byteArrayOf(13)

    /** Backspace envia DEL (0x7F) em vez de BS (0x08). */
    var backspaceAsDel: Boolean = false

    /** F5 envia sequência PuTTY (ESC[[E) em vez do padrão (ESC[15~). */
    var f5PuttySequence: Boolean = false

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCheckIsTextEditor(): Boolean = true

    /**
     * Trata uma tecla (Enter/Backspace/Tab/F5/caractere) e manda os bytes correspondentes.
     * Usado tanto pelo teclado virtual (via InputConnection.sendKeyEvent) quanto pelo
     * teclado físico do coletor (via onKeyDown), já que hardware nunca passa pelo IME.
     */
    private fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val bsByte = if (backspaceAsDel) 127.toByte() else 8.toByte()
        when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER -> { onInput?.invoke(lineTerminator); return true }
            KeyEvent.KEYCODE_DEL   -> { onInput?.invoke(byteArrayOf(bsByte)); return true }
            KeyEvent.KEYCODE_TAB   -> { onInput?.invoke(byteArrayOf(9)); return true }
            KeyEvent.KEYCODE_F5    -> {
                val seq = if (f5PuttySequence)
                    byteArrayOf(27, '['.code.toByte(), '['.code.toByte(), 'E'.code.toByte())
                else
                    byteArrayOf(27, '['.code.toByte(), '1'.code.toByte(), '5'.code.toByte(), '~'.code.toByte())
                onInput?.invoke(seq); return true
            }
            KeyEvent.KEYCODE_ESCAPE -> { onInput?.invoke(byteArrayOf(27)); return true }
            // Mesmas sequências que os botões F1-F9 da barra de ferramentas (SS3/CSI).
            KeyEvent.KEYCODE_F1 -> { onInput?.invoke(byteArrayOf(27, 'O'.code.toByte(), 'P'.code.toByte())); return true }
            KeyEvent.KEYCODE_F2 -> { onInput?.invoke(byteArrayOf(27, 'O'.code.toByte(), 'Q'.code.toByte())); return true }
            KeyEvent.KEYCODE_F3 -> { onInput?.invoke(byteArrayOf(27, 'O'.code.toByte(), 'R'.code.toByte())); return true }
            KeyEvent.KEYCODE_F4 -> { onInput?.invoke(byteArrayOf(27, 'O'.code.toByte(), 'S'.code.toByte())); return true }
            KeyEvent.KEYCODE_F6 -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), '1'.code.toByte(), '7'.code.toByte(), '~'.code.toByte())); return true }
            KeyEvent.KEYCODE_F7 -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), '1'.code.toByte(), '8'.code.toByte(), '~'.code.toByte())); return true }
            KeyEvent.KEYCODE_F8 -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), '1'.code.toByte(), '9'.code.toByte(), '~'.code.toByte())); return true }
            KeyEvent.KEYCODE_F9 -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), '2'.code.toByte(), '0'.code.toByte(), '~'.code.toByte())); return true }
            // Mesma sequência ANSI que os botões de seta da barra de ferramentas já enviam.
            KeyEvent.KEYCODE_DPAD_UP    -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), 'A'.code.toByte())); return true }
            KeyEvent.KEYCODE_DPAD_DOWN  -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), 'B'.code.toByte())); return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), 'C'.code.toByte())); return true }
            KeyEvent.KEYCODE_DPAD_LEFT  -> { onInput?.invoke(byteArrayOf(27, '['.code.toByte(), 'D'.code.toByte())); return true }
            else -> {
                val ch = event.unicodeChar
                if (ch != 0) { onInput?.invoke(byteArrayOf(ch.toByte())); return true }
            }
        }
        return false
    }

    /** Teclado físico do coletor (não passa pelo IME — precisa desse caminho separado). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (handleKeyEvent(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        // TYPE_NULL faz o teclado mandar key events (melhor para terminal),
        // mas tratamos commitText tambem (Gboard etc).
        outAttrs.inputType = InputType.TYPE_NULL
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_FLAG_NO_FULLSCREEN or
            EditorInfo.IME_ACTION_NONE

        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.let { sendString(it.toString()) }
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                val bsByte = if (backspaceAsDel) 127.toByte() else 8.toByte()
                repeat(beforeLength.coerceAtLeast(1)) { onInput?.invoke(byteArrayOf(bsByte)) }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (handleKeyEvent(event)) return true
                return super.sendKeyEvent(event)
            }
        }
    }

    private fun sendString(s: String) {
        if (s.isEmpty()) return
        if (!s.contains('\n')) {
            onInput?.invoke(s.toByteArray(Charsets.ISO_8859_1))
            return
        }
        // Cada quebra de linha vira o terminador configurado
        val parts = s.split('\n')
        for ((idx, part) in parts.withIndex()) {
            if (part.isNotEmpty()) onInput?.invoke(part.toByteArray(Charsets.ISO_8859_1))
            if (idx < parts.size - 1) onInput?.invoke(lineTerminator)
        }
    }
}

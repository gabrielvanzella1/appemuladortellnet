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
                if (event.action == KeyEvent.ACTION_DOWN) {
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
                        else -> {
                            val ch = event.unicodeChar
                            if (ch != 0) { onInput?.invoke(byteArrayOf(ch.toByte())); return true }
                        }
                    }
                }
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

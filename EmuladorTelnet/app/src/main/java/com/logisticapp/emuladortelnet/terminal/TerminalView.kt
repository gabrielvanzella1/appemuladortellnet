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
                // Backspace em alguns teclados chega por aqui
                repeat(beforeLength.coerceAtLeast(1)) { onInput?.invoke(byteArrayOf(8)) }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_ENTER -> { onInput?.invoke(byteArrayOf(13)); return true }
                        KeyEvent.KEYCODE_DEL   -> { onInput?.invoke(byteArrayOf(8)); return true }
                        KeyEvent.KEYCODE_TAB   -> { onInput?.invoke(byteArrayOf(9)); return true }
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
        val bytes = s.replace("\n", "\r").toByteArray(Charsets.ISO_8859_1)
        onInput?.invoke(bytes)
    }
}

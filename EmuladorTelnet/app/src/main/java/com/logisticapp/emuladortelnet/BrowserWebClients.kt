package com.logisticapp.emuladortelnet

import android.app.Activity
import android.net.http.SslError
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import timber.log.Timber

/**
 * WebViewClient da sessão do modo Browser. Mantém a navegação dentro do app
 * (não libera esquemas externos como mailto:/tel:/intent: na Fase 1) e rejeita
 * sempre certificados SSL inválidos por padrão (comportamento seguro; ver nota
 * no plano sobre um toggle de "confiar neste certificado" em fase futura).
 */
class BrowserSessionWebViewClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return request.url.scheme !in ALLOWED_SCHEMES
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (!request.isForMainFrame) return
        Timber.w("BrowserActivity: erro ao carregar ${request.url}: ${error.description}")
        Toast.makeText(view.context.applicationContext, "Erro ao carregar a página: ${error.description}", Toast.LENGTH_LONG).show()
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        Timber.w("BrowserActivity: certificado inválido, bloqueando: ${error.url}")
        Toast.makeText(view.context.applicationContext, "Certificado inválido: conexão bloqueada.", Toast.LENGTH_LONG).show()
        handler.cancel()
    }

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}

/**
 * WebChromeClient da sessão do modo Browser. A WebView vive em
 * applicationContext (para sobreviver à troca de Activity), que não tem
 * window token válido para abrir AlertDialog — por isso [hostActivity] é
 * setado pela Activity atualmente exibindo a WebView (onResume/onPause) e
 * usado somente nesses callbacks.
 */
class BrowserSessionWebChromeClient : WebChromeClient() {

    var hostActivity: Activity? = null
    var onProgressChanged: ((Int) -> Unit)? = null

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        val act = hostActivity ?: run { result.cancel(); return true }
        AlertDialog.Builder(act)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> result.confirm() }
            .setOnCancelListener { result.cancel() }
            .show()
        return true
    }

    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        val act = hostActivity ?: run { result.cancel(); return true }
        AlertDialog.Builder(act)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> result.confirm() }
            .setNegativeButton("Cancelar") { _, _ -> result.cancel() }
            .setOnCancelListener { result.cancel() }
            .show()
        return true
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        onProgressChanged?.invoke(newProgress)
    }
}

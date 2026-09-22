package com.logisticapp.scantebrowser.web

import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import timber.log.Timber

/**
 * WebViewClient de um portal. Mantém navegação dentro do app (só libera
 * http/https), rejeita certificado SSL inválido por padrão (mesmo critério
 * usado no modo Browser do ScanTE) e injeta o script de DOM customizado do
 * portal (se houver) assim que a página termina de carregar.
 *
 * [onRenderProcessGone] é tratado explicitamente — sem isso, o app derruba
 * quando o SO mata o processo renderer do Chromium sob pressão de memória
 * (mais provável aqui por manter várias WebViews vivas simultaneamente).
 */
class PortalWebViewClient(
    private val domInjectionScript: String?,
    private val onRenderProcessGoneCallback: (WebView) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return request.url.scheme !in ALLOWED_SCHEMES
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (!domInjectionScript.isNullOrBlank()) {
            view.evaluateJavascript(domInjectionScript, null)
        }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (!request.isForMainFrame) return
        Timber.w("PortalWebViewClient: erro ao carregar ${request.url}: ${error.description}")
        Toast.makeText(view.context.applicationContext, "Erro ao carregar a página: ${error.description}", Toast.LENGTH_LONG).show()
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        Timber.w("PortalWebViewClient: certificado inválido, bloqueando: ${error.url}")
        Toast.makeText(view.context.applicationContext, "Certificado inválido: conexão bloqueada.", Toast.LENGTH_LONG).show()
        handler.cancel()
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        Timber.w("PortalWebViewClient: processo renderer encerrado (crash=${detail.didCrash()})")
        onRenderProcessGoneCallback(view)
        return true // indica que tratamos — evita o crash do app
    }

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }
}

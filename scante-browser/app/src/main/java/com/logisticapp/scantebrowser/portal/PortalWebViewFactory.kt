package com.logisticapp.scantebrowser.portal

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.logisticapp.scantebrowser.BuildConfig
import com.logisticapp.scantebrowser.web.PortalWebChromeClient
import com.logisticapp.scantebrowser.web.PortalWebViewClient

/**
 * Cria e configura uma WebView para um [Portal]. Usa applicationContext —
 * a WebView deve sobreviver a trocas de Activity (mesmo padrão validado no
 * modo Browser do ScanTE).
 */
object PortalWebViewFactory {

    fun create(
        appContext: Context,
        portal: Portal,
        onRenderProcessGone: (WebView) -> Unit
    ): WebView {
        val webView = WebView(appContext.applicationContext)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = portal.useWideViewport
            loadWithOverviewMode = true
            textZoom = portal.textScalingPercent
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // Gesto de pinça continua ativo sempre; isso aqui só liga/desliga os botões +/- na tela.
            builtInZoomControls = true
            displayZoomControls = portal.enableZoomControls
        }
        if (portal.initialScale > 0) {
            webView.setInitialScale(portal.initialScale)
        }
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

        webView.webViewClient = PortalWebViewClient(
            domInjectionScript = portal.domInjectionScript,
            onRenderProcessGoneCallback = onRenderProcessGone
        )
        webView.webChromeClient = PortalWebChromeClient()
        webView.loadUrl(portal.homepage)
        return webView
    }
}

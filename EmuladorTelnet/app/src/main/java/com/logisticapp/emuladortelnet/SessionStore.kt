package com.logisticapp.emuladortelnet

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory

/**
 * Mantém até MAX sessões (Telnet e/ou Browser) simultâneas vivas independente
 * do ciclo de vida das Activities. O ViewModel/WebView é criado aqui (não via
 * ViewModelProvider/Activity) para que sobreviva ao finish() da Activity.
 */
object SessionStore {

    const val MAX = 2

    sealed class ActiveSession {
        abstract val slotId: Int
        abstract val hostId: Int
        abstract val hostName: String

        data class Telnet(
            override val slotId: Int,
            override val hostId: Int,
            override val hostName: String,
            val host: String,
            val port: Int,
            val viewModel: TelnetViewModel
        ) : ActiveSession()

        data class Browser(
            override val slotId: Int,
            override val hostId: Int,
            override val hostName: String,
            val url: String,
            val webView: WebView
        ) : ActiveSession()
    }

    private val slots = arrayOfNulls<ActiveSession>(MAX)

    /**
     * Abre ou retoma uma sessão Telnet.
     * Retorna (slotId, viewModel) se há vaga, ou null se as duas vagas estão ocupadas.
     */
    fun openOrResume(
        context: Context,
        hostId: Int,
        hostName: String,
        host: String,
        port: Int
    ): Pair<Int, TelnetViewModel>? {
        val existingIdx = slots.indexOfFirst { it?.hostId == hostId }
        if (existingIdx >= 0) {
            val existing = slots[existingIdx]
            if (existing is ActiveSession.Telnet) return Pair(existing.slotId, existing.viewModel)
            closeInternal(existingIdx) // era Browser (host mudou de tipo) — descarta antes de recriar
        }
        val free = slots.indexOfFirst { it == null }
        if (free < 0) return null // todas as vagas ocupadas

        val repo = TelnetRepository.getInstance(context)
        val vm   = TelnetViewModelFactory(repo).create(TelnetViewModel::class.java)
        slots[free] = ActiveSession.Telnet(free, hostId, hostName, host, port, vm)
        return Pair(free, vm)
    }

    /**
     * Abre ou retoma uma sessão Browser (WebView).
     * Deve ser chamada a partir da UI thread (criação de WebView exige Looper principal).
     * Retorna (slotId, webView) se há vaga, ou null se as duas vagas estão ocupadas.
     */
    fun openOrResumeBrowser(
        context: Context,
        hostId: Int,
        hostName: String,
        url: String
    ): Pair<Int, WebView>? {
        val existingIdx = slots.indexOfFirst { it?.hostId == hostId }
        if (existingIdx >= 0) {
            val existing = slots[existingIdx]
            if (existing is ActiveSession.Browser) return Pair(existing.slotId, existing.webView)
            closeInternal(existingIdx) // era Telnet (host mudou de tipo) — descarta antes de recriar
        }
        val free = slots.indexOfFirst { it == null }
        if (free < 0) return null // todas as vagas ocupadas

        val webView = createBrowserWebView(context.applicationContext, url)
        slots[free] = ActiveSession.Browser(free, hostId, hostName, url, webView)
        return Pair(free, webView)
    }

    private fun createBrowserWebView(appContext: Context, url: String): WebView {
        val webView = WebView(appContext)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = BrowserSessionWebViewClient()
        webView.webChromeClient = BrowserSessionWebChromeClient()
        webView.loadUrl(url)
        return webView
    }

    /** Desconecta/destrói e libera o slot. */
    fun close(slotId: Int) = closeInternal(slotId)

    private fun closeInternal(slotId: Int) {
        when (val s = slots.getOrNull(slotId)) {
            is ActiveSession.Telnet  -> s.viewModel.disconnect()
            is ActiveSession.Browser -> {
                s.webView.stopLoading()
                s.webView.loadUrl("about:blank")
                s.webView.clearHistory()
                s.webView.destroy()
            }
            null -> {}
        }
        if (slotId in slots.indices) slots[slotId] = null
    }

    fun get(slotId: Int): ActiveSession? = slots.getOrNull(slotId)

    fun getAll(): List<ActiveSession> = slots.filterNotNull()

    fun isActive(hostId: Int): Boolean = slots.any { it?.hostId == hostId }

    fun activeCount(): Int = slots.count { it != null }

    /** Retorna a outra sessão ativa (se houver), útil para o botão ⇄. */
    fun otherSession(slotId: Int): ActiveSession? =
        slots.firstOrNull { it != null && it.slotId != slotId }
}

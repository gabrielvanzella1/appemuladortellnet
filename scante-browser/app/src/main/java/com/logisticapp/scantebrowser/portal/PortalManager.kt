package com.logisticapp.scantebrowser.portal

import android.content.Context
import android.webkit.WebView
import com.logisticapp.scantebrowser.config.BrowserConfig
import timber.log.Timber

/**
 * Dono do estado de todos os portais: cria uma WebView por portal na
 * inicialização e mantém todas vivas (poucos portais — até 4, como no caso
 * real do cliente — cabe tranquilo em hardware de coletor moderno; ver nota
 * de arquitetura no plano). Troca de portal só esconde/mostra a WebView
 * correspondente, todas já carregadas.
 */
class PortalManager(private val appContext: Context, config: BrowserConfig) {

    val portals: List<Portal> = config.portals
    val keyActions: Map<String, Map<String, String>> = config.keyActions

    private val webViews = LinkedHashMap<String, WebView>()

    var activePortalId: String? = portals.firstOrNull()?.id
        private set

    init {
        portals.forEach { portal ->
            webViews[portal.id] = PortalWebViewFactory.create(appContext, portal) { crashedView ->
                handleRenderProcessGone(portal.id, crashedView)
            }
        }
        Timber.d("PortalManager: ${portals.size} portal(is) inicializado(s)")
    }

    fun activePortal(): Portal? = portals.firstOrNull { it.id == activePortalId }

    fun activeWebView(): WebView? = activePortalId?.let { webViews[it] }

    fun webViewFor(portalId: String): WebView? = webViews[portalId]

    fun switchTo(portalId: String) {
        if (portals.none { it.id == portalId }) {
            Timber.w("PortalManager: tentativa de trocar para portal desconhecido: $portalId")
            return
        }
        activePortalId = portalId
    }

    /** Recria a WebView do portal cujo processo renderer foi encerrado (evita crash do app). */
    private fun handleRenderProcessGone(portalId: String, crashedView: WebView) {
        Timber.w("PortalManager: recriando WebView do portal '$portalId' após onRenderProcessGone")
        (crashedView.parent as? android.view.ViewGroup)?.removeView(crashedView)
        crashedView.destroy()

        val portal = portals.firstOrNull { it.id == portalId } ?: return
        webViews[portalId] = PortalWebViewFactory.create(appContext, portal) { view ->
            handleRenderProcessGone(portalId, view)
        }
    }

    fun destroyAll() {
        webViews.values.forEach { wv ->
            (wv.parent as? android.view.ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.destroy()
        }
        webViews.clear()
    }
}

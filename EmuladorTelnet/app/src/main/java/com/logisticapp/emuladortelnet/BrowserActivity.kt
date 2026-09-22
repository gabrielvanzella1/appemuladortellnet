package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.KeyCharacterMap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.logisticapp.emuladortelnet.databinding.ActivityBrowserBinding
import com.logisticapp.emuladortelnet.settings.AppSettings
import org.json.JSONTokener
import timber.log.Timber

/**
 * Tela do modo Browser: exibe a WebView de uma sessão mantida viva em
 * [SessionStore] (sobrevive ao finish() desta Activity, igual ao MainActivity
 * com TelnetViewModel). Esta Activity só anexa/desanexa a WebView do seu
 * layout — nunca a destrói, exceto ao fechar a sessão explicitamente.
 */
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private lateinit var settings: AppSettings
    private lateinit var barcodeManager: BarcodeScannerManager

    private var slotId = -1
    private var webView: WebView? = null

    companion object {
        const val EXTRA_SLOT_ID = "extra_slot_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)

        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializado incondicionalmente: mesmo no caminho de erro abaixo, finish() em
        // onCreate() nao impede onResume()/onPause() de rodarem antes da Activity encerrar
        // de fato, e ambos usam barcodeManager.
        barcodeManager = BarcodeScannerManager(this) { barcode, action ->
            injectBarcodeAsKeystrokes(barcode, action)
        }

        slotId = intent.getIntExtra(EXTRA_SLOT_ID, -1)
        val slot = if (slotId >= 0) SessionStore.get(slotId) as? SessionStore.ActiveSession.Browser else null
        if (slot == null) {
            Toast.makeText(this, "Sessão de navegador não encontrada", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        webView = slot.webView
        binding.statusText.text = slot.hostName
        attachWebView()

        setupToolbarButtons()

        onBackPressedDispatcher.addCallback(this) {
            val wv = webView
            if (wv != null && wv.canGoBack()) wv.goBack() else finish()
        }
    }

    private fun attachWebView() {
        val wv = webView ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        binding.webViewContainer.addView(
            wv,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun setupToolbarButtons() {
        binding.btnNavBack.setOnClickListener { if (webView?.canGoBack() == true) webView?.goBack() }
        binding.btnNavForward.setOnClickListener { if (webView?.canGoForward() == true) webView?.goForward() }
        binding.btnReload.setOnClickListener { webView?.reload() }
        binding.btnPrint.setOnClickListener { printVisiblePage() }
        binding.btnClose.setOnClickListener {
            (webView?.parent as? ViewGroup)?.removeView(webView)
            SessionStore.close(slotId)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val chromeClient = webView?.webChromeClient as? BrowserSessionWebChromeClient
        chromeClient?.hostActivity = this
        chromeClient?.onProgressChanged = { progress -> updateProgress(progress) }

        barcodeManager.updateOptions(settings.barcodeOptions)
        barcodeManager.register()
    }

    override fun onPause() {
        super.onPause()
        val chromeClient = webView?.webChromeClient as? BrowserSessionWebChromeClient
        chromeClient?.hostActivity = null
        chromeClient?.onProgressChanged = null

        barcodeManager.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desanexa sem destruir — a WebView continua viva em SessionStore para a proxima retomada.
        (webView?.parent as? ViewGroup)?.removeView(webView)
    }

    private fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
        binding.progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
    }

    /**
     * Digita o código de barras já lido (BarcodeScannerManager já aplicou
     * prefixo/sufixo/remoção configurados antes de chamar este callback) no
     * campo com foco na página, simulando um leitor USB/Bluetooth fisico.
     * Se nada estiver focado, o scan se perde (limitacao aceita na Fase 1).
     */
    private fun injectBarcodeAsKeystrokes(barcode: String, action: String) {
        val wv = webView ?: return
        val suffix = when (action) {
            "Enter"       -> "\n"
            "Tab"         -> "\t"
            "Enter + Tab" -> "\n\t"
            else          -> ""
        }
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents((barcode + suffix).toCharArray())
        if (events == null) {
            Timber.w("BrowserActivity: não foi possível converter \"$barcode\" em eventos de teclado")
            Toast.makeText(this, "Não foi possível digitar o código nesta página", Toast.LENGTH_SHORT).show()
            return
        }
        events.forEach { wv.dispatchKeyEvent(it) }
        Timber.d("BrowserActivity: barcode digitado via teclado simulado: $barcode (ação=$action)")
    }

    /** Extrai o texto visivel da pagina atual e envia para a impressora termica configurada. */
    private fun printVisiblePage() {
        val wv = webView ?: return
        wv.evaluateJavascript("document.body ? document.body.innerText : ''") { raw ->
            val text = try {
                JSONTokener(raw).nextValue() as? String
            } catch (e: Exception) {
                Timber.w(e, "BrowserActivity: falha ao decodificar texto da pagina para impressao")
                null
            } ?: ""
            PrintHelper.printLinesAsync(this, text.split("\n"), settings.printOptions)
        }
    }
}

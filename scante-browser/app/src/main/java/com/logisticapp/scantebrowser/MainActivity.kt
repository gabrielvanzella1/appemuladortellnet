package com.logisticapp.scantebrowser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.logisticapp.scantebrowser.config.ConfigRepository
import com.logisticapp.scantebrowser.keymap.KeyAction
import com.logisticapp.scantebrowser.keymap.KeyMapResolver
import com.logisticapp.scantebrowser.portal.Portal
import com.logisticapp.scantebrowser.portal.PortalManager
import com.logisticapp.scantebrowser.settings.BarcodeOptions
import com.logisticapp.scantebrowser.web.PortalWebChromeClient
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var portalManager: PortalManager
    private lateinit var keyMapResolver: KeyMapResolver
    private lateinit var barcodeManager: BarcodeScannerManager

    private lateinit var spinnerPortal: Spinner
    private lateinit var webViewContainer: FrameLayout
    private lateinit var functionKeyBar: LinearLayout
    private lateinit var functionKeyScroll: View
    private lateinit var progressBar: ProgressBar
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("scante_browser_prefs", MODE_PRIVATE)

        spinnerPortal = findViewById(R.id.spinner_portal)
        webViewContainer = findViewById(R.id.web_view_container)
        functionKeyBar = findViewById(R.id.function_key_bar)
        functionKeyScroll = findViewById(R.id.function_key_scroll)
        progressBar = findViewById(R.id.progress_bar)

        requestAllFilesAccessIfNeeded()

        val config = ConfigRepository(this).load()
        portalManager = PortalManager(this, config)
        keyMapResolver = KeyMapResolver(config.keyActions)

        barcodeManager = BarcodeScannerManager(this) { barcode, action ->
            injectBarcodeAsKeystrokes(barcode, action)
        }

        setupPortalSpinner()
        setupReloadButton()
        setupKeyboardToggleButton()

        if (portalManager.portals.isEmpty()) {
            Toast.makeText(this, "Nenhum portal configurado", Toast.LENGTH_LONG).show()
            return
        }
        showActivePortal()
    }

    private fun setupPortalSpinner() {
        val names = portalManager.portals.map { it.name }
        spinnerPortal.adapter = ArrayAdapter(this, R.layout.spinner_item_white, names).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerPortal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val portal = portalManager.portals.getOrNull(position) ?: return
                if (portal.id != portalManager.activePortalId) {
                    portalManager.switchTo(portal.id)
                    showActivePortal()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupReloadButton() {
        findViewById<View>(R.id.btn_reload).setOnClickListener {
            portalManager.activeWebView()?.reload()
        }
    }

    /** Botão da barra verde que mostra/oculta a fileira de teclas F1-F12 — estado lembrado entre reinícios do app. */
    private fun setupKeyboardToggleButton() {
        functionKeyScroll.visibility = if (prefs.getBoolean(PREF_FUNCTION_BAR_VISIBLE, true)) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btn_toggle_keyboard).setOnClickListener {
            val showing = functionKeyScroll.visibility == View.VISIBLE
            functionKeyScroll.visibility = if (showing) View.GONE else View.VISIBLE
            prefs.edit().putBoolean(PREF_FUNCTION_BAR_VISIBLE, !showing).apply()
        }
    }

    /**
     * Config é gravada pelo ScanTE Config (app separado) numa pasta pública
     * do armazenamento — a partir do Android 11 isso exige a permissão
     * "Acesso a todos os arquivos", que não tem diálogo padrão, só a tela de
     * Ajustes. Pedida uma única vez (fica concedida entre reinícios do app).
     */
    private fun requestAllFilesAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    /** Anexa a WebView do portal ativo ao container e monta a barra de função dele. */
    private fun showActivePortal() {
        val webView = portalManager.activeWebView() ?: return
        (webView.parent as? ViewGroup)?.removeView(webView)
        webViewContainer.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val chromeClient = webView.webChromeClient as? PortalWebChromeClient
        chromeClient?.hostActivity = this
        chromeClient?.onProgressChanged = { p -> updateProgress(p) }

        buildFunctionKeyBar()
    }

    private fun buildFunctionKeyBar() {
        functionKeyBar.removeAllViews()
        val portalId = portalManager.activePortalId ?: return
        val keys = keyMapResolver.functionKeysFor(portalId)
        for (keyCode in keys) {
            val btn = Button(this).apply {
                text = KeyMapResolver.functionKeyLabel(keyCode)
                setOnClickListener { executeAction(keyMapResolver.resolve(portalId, keyCode)) }
            }
            functionKeyBar.addView(btn)
        }
    }

    private fun updateProgress(progress: Int) {
        progressBar.progress = progress
        progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        val chromeClient = portalManager.activeWebView()?.webChromeClient as? PortalWebChromeClient
        chromeClient?.hostActivity = this
        barcodeManager.updateOptions(BarcodeOptions())
        barcodeManager.register()
    }

    override fun onPause() {
        super.onPause()
        val chromeClient = portalManager.activeWebView()?.webChromeClient as? PortalWebChromeClient
        chromeClient?.hostActivity = null
        barcodeManager.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Única Activity do app (diferente do ScanTE, aqui não há tela-lista separada que
        // precise reter sessões entre Activities) — ao sair, destrói tudo de fato.
        portalManager.destroyAll()
    }

    /** Tecla física — mesmo resolvedor usado pela barra de função na tela. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val portalId = portalManager.activePortalId
            if (portalId != null) {
                val action = keyMapResolver.resolve(portalId, event.keyCode)
                if (action != null) {
                    executeAction(action)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun executeAction(action: KeyAction?) {
        val webView = portalManager.activeWebView() ?: return
        // Botões nativos (F1-F12 na tela) roubam o foco da WebView ao serem tocados —
        // sem isso, o KeyEvent simulado não chega no elemento HTML focado.
        webView.requestFocus()
        when (action) {
            is KeyAction.Submit -> dispatchEnter(webView)
            is KeyAction.GoBack -> if (webView.canGoBack()) webView.goBack()
            is KeyAction.Reload -> webView.reload()
            is KeyAction.SwitchPortal -> {
                val index = portalManager.portals.indexOfFirst { it.id == action.portalId }
                if (index >= 0) spinnerPortal.setSelection(index)
            }
            is KeyAction.RunScript -> webView.evaluateJavascript(action.script, null)
            null -> {}
        }
    }

    private fun dispatchEnter(webView: WebView) {
        // SUBMIT é acionado por um botão nativo Android (F1-F12 na tela ou tecla física
        // interceptada na Activity) — diferente do scan, aqui não há garantia de que o foco
        // do "documento" dentro do Chromium continue sincronizado com o foco da View Android
        // no momento do dispatch. Disparar o evento direto no elemento focado via JS contorna
        // essa dependência de estado de foco nativo.
        val script = """
            (function() {
                var el = document.activeElement;
                if (!el || el === document.body) return;
                var opts = {key:'Enter', code:'Enter', keyCode:13, which:13, bubbles:true, cancelable:true};
                el.dispatchEvent(new KeyboardEvent('keydown', opts));
                el.dispatchEvent(new KeyboardEvent('keypress', opts));
                el.dispatchEvent(new KeyboardEvent('keyup', opts));
                if (el.form) { if (el.form.requestSubmit) el.form.requestSubmit(); else el.form.submit(); }
            })();
        """.trimIndent()
        webView.evaluateJavascript(script, null)
    }

    /**
     * Digita o código de barras já processado no campo com foco na WebView do
     * portal ativo, simulando um leitor físico (mesma técnica validada no
     * modo Browser do ScanTE) — funciona em qualquer página sem integração.
     */
    private fun injectBarcodeAsKeystrokes(barcode: String, action: String) {
        val webView = portalManager.activeWebView() ?: return
        val suffix = when (action) {
            "Enter" -> "\n"
            "Tab" -> "\t"
            "Enter + Tab" -> "\n\t"
            else -> ""
        }
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents((barcode + suffix).toCharArray())
        if (events == null) {
            Timber.w("MainActivity: não foi possível converter \"$barcode\" em eventos de teclado")
            Toast.makeText(this, "Não foi possível digitar o código nesta página", Toast.LENGTH_SHORT).show()
            return
        }
        events.forEach { webView.dispatchKeyEvent(it) }
    }

    companion object {
        private const val PREF_FUNCTION_BAR_VISIBLE = "function_bar_visible"
    }
}

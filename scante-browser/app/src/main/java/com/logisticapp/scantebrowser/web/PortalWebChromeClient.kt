package com.logisticapp.scantebrowser.web

import android.app.Activity
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog

/**
 * WebChromeClient de um portal. As WebViews são criadas com applicationContext
 * (para sobreviver à troca/fechamento de Activity), que não tem window token
 * válido para abrir AlertDialog — por isso [hostActivity] é setado pela
 * Activity atualmente exibindo essa WebView (onResume/onPause) e usado só
 * nesses callbacks.
 */
class PortalWebChromeClient : WebChromeClient() {

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

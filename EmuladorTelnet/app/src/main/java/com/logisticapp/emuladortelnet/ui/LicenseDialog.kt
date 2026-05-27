package com.logisticapp.emuladortelnet.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.logisticapp.emuladortelnet.license.LicenseManager
import timber.log.Timber

/**
 * Dialog para mostrar informações de licença
 */
class LicenseDialog(private val context: Context) {

    fun show(licenseKey: String?, onGenerateTrial: () -> Unit, onGeneratePremium: () -> Unit) {
        val licenseManager = LicenseManager(context)
        val licenseInfo = licenseManager.getLicenseInfo()
        val isValid = !licenseInfo.isExpired
        val validationMsg = licenseInfo.message

        // Criar layout customizado
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 20, 20, 20)
            }
        }

        // ScrollView para caso texto seja muito longo
        val scrollView = ScrollView(context).apply {
            addView(layout)
        }

        // Titulo
        val titleText = TextView(context).apply {
            text = "Informações de Licença"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        layout.addView(titleText)

        // Status
        val statusColor = if (isValid) android.graphics.Color.GREEN else android.graphics.Color.RED
        val statusText = TextView(context).apply {
            text = if (isValid) "✅ Licença Válida" else "❌ Sem Licença Válida"
            textSize = 14f
            setTextColor(statusColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        layout.addView(statusText)

        // Mensagem de validação
        val msgText = TextView(context).apply {
            text = validationMsg
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        layout.addView(msgText)

        // Informações do Device
        val deviceInfo = TextView(context).apply {
            text = "Device Info:\n${licenseManager.getDeviceInfo()}"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        layout.addView(deviceInfo)

        // Licença atual
        val licenseText = TextView(context).apply {
            text = if (licenseKey != null) {
                try {
                    val parts = licenseKey.split("-")
                    "Tipo: ${parts.getOrNull(0) ?: "Unknown"}\nChave: ${licenseKey.take(20)}..."
                } catch (e: Exception) {
                    "Chave de licença: ${licenseKey.take(20)}..."
                }
            } else {
                "Nenhuma licença ativada"
            }
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        layout.addView(licenseText)

        // Dialog com botões
        AlertDialog.Builder(context)
            .setView(scrollView)
            .setPositiveButton("TRIAL (30 dias)") { _, _ ->
                onGenerateTrial()
            }
            .setNeutralButton("PREMIUM (1 ano)") { _, _ ->
                onGeneratePremium()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }
}

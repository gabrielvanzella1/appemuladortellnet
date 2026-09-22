package com.logisticapp.scantebrowser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.logisticapp.scantebrowser.settings.BarcodeOptions
import timber.log.Timber

/**
 * Recebe dados de scanners de código de barras via broadcast.
 * Suporta: Honeywell, Zebra/Symbol, Newland, Urovo, Datalogic,
 *          Sunmi, Bluebird e dispositivos genéricos.
 *
 * Portado do ScanTE (com o fix de RECEIVER_EXPORTED para Android 13+ já aplicado).
 * Use register() ao conectar e unregister() ao desconectar.
 */
class BarcodeScannerManager(
    private val context: Context,
    private val onBarcode: (barcode: String, action: String) -> Unit
) {
    private var opts = BarcodeOptions()

    fun updateOptions(newOpts: BarcodeOptions) {
        opts = newOpts
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val raw = extractBarcode(intent) ?: return
            Timber.d("Barcode recebido [${intent.action}]: $raw")
            val processed = processBarcode(raw, opts)
            onBarcode(processed, opts.actionAfterScan)
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            // Honeywell (Intermec)
            addAction("com.honeywell.aidc.action.ACTION_BARCODE_DATA")
            // Zebra / Symbol DataWedge
            addAction("com.symbol.datawedge.action.RESULT_ACTION")
            // Newland
            addAction("nlscan.action.SCANNER_RESULT")
            // Urovo
            addAction("android.intent.ACTION_DECODE_DATA")
            addAction("com.urovo.datawedge.action.RESULT_ACTION")
            // Datalogic
            addAction("com.datalogic.decode.action.SCAN")
            // Sunmi
            addAction("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED")
            // Bluebird
            addAction("kr.co.bluebird.android.bbapi.action.BARCODE_CALLBACK_DECODEDATA")
            // Genérico (vários fabricantes)
            addAction("android.intent.action.SCANRESULT")
            addAction("scan.rcv.message")
        }
        try {
            // RECEIVER_EXPORTED: os drivers de scanner (Zebra DataWedge, Honeywell etc.) rodam em
            // apps/processos separados e enviam esses broadcasts de fora deste app — precisa
            // continuar recebendo de outros apps, exigido explicitamente a partir do Android 13
            // (API 33). Preserva o comportamento de antes da API 33 (que já era, na prática, exportado).
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            Timber.d("BarcodeScannerManager: receptor registrado")
        } catch (e: Exception) {
            Timber.w(e, "BarcodeScannerManager: falha ao registrar receptor")
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
            Timber.d("BarcodeScannerManager: receptor removido")
        } catch (e: Exception) {
            // ignorar se não estava registrado
        }
    }

    private fun extractBarcode(intent: Intent): String? {
        val raw = when (intent.action) {
            "com.honeywell.aidc.action.ACTION_BARCODE_DATA" ->
                intent.getStringExtra("data")
                    ?: intent.getStringExtra("EXTRA_BARCODE_DECODEDATA")
            "com.symbol.datawedge.action.RESULT_ACTION" ->
                intent.getStringExtra("com.symbol.datawedge.data_string")
            "nlscan.action.SCANNER_RESULT" ->
                intent.getStringExtra("SCAN_BARCODE1")
                    ?: intent.getStringExtra("barcode_string")
            "android.intent.ACTION_DECODE_DATA",
            "com.urovo.datawedge.action.RESULT_ACTION" ->
                intent.getStringExtra("barcode_string")
                    ?: intent.getStringExtra("com.urovo.datawedge.data_string")
            "com.datalogic.decode.action.SCAN" ->
                intent.getStringExtra("DecodeValue")
                    ?: intent.getStringExtra("DECODE_DATA_STRING")
            "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED" ->
                intent.getStringExtra("data")
                    ?: intent.getStringExtra("EXTRA_BARCODE_DECODEDATA")
            "kr.co.bluebird.android.bbapi.action.BARCODE_CALLBACK_DECODEDATA" -> {
                intent.getByteArrayExtra("EXTRA_BARCODE_DECODEDATA")
                    ?.let { String(it).trimEnd('\u0000') }
                    ?: intent.getStringExtra("EXTRA_BARCODE_DECODEDATA")
            }
            "android.intent.action.SCANRESULT",
            "scan.rcv.message" ->
                intent.getStringExtra("value")
                    ?: intent.getStringExtra("data")
                    ?: intent.getStringExtra("SCAN_BARCODE1")
            else -> null
        }
        return raw?.ifBlank { null }
    }

    companion object {
        fun processBarcode(raw: String, opts: BarcodeOptions): String {
            var text = raw
            if (opts.removeCharsStart > 0 && text.length > opts.removeCharsStart) {
                text = text.substring(opts.removeCharsStart)
            }
            if (opts.removeCharsEnd > 0 && text.length > opts.removeCharsEnd) {
                text = text.substring(0, text.length - opts.removeCharsEnd)
            }
            text = opts.addTextBefore + text + opts.addTextAfter
            return text
        }
    }
}

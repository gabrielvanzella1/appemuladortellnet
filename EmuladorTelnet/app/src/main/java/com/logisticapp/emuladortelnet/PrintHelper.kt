package com.logisticapp.emuladortelnet

import android.app.Activity
import android.widget.Toast
import com.logisticapp.emuladortelnet.settings.PrintOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Envia linhas de texto para a impressora térmica configurada, reaproveitado
 * tanto pelo modo Telnet (MainActivity) quanto pelo modo Browser (BrowserActivity).
 * Roda numa MainScope própria (não presa ao lifecycle da Activity chamadora)
 * para que o job de impressão não seja cancelado se o usuário navegar antes
 * dele terminar — mesmo comportamento de antes da extração.
 */
object PrintHelper {

    fun printLinesAsync(activity: Activity, lines: List<String>, opts: PrintOptions) {
        if (opts.connectionType == "Bluetooth" && opts.bluetoothAddress.isBlank()) {
            Toast.makeText(activity,
                "Configure a impressora em Configurações → Dispositivos → Impressão",
                Toast.LENGTH_LONG).show()
            return
        }
        if (opts.connectionType == "WiFi" && opts.wifiHost.isBlank()) {
            Toast.makeText(activity,
                "Configure o IP da impressora em Configurações → Dispositivos → Impressão",
                Toast.LENGTH_LONG).show()
            return
        }
        val printer = EscPosPrinter()
        Toast.makeText(activity, "Enviando para a impressora…", Toast.LENGTH_SHORT).show()
        MainScope().launch(Dispatchers.IO) {
            try {
                if (printer.connect(opts)) {
                    printer.printLines(lines, opts)
                    printer.disconnect()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Impressão enviada!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Não foi possível conectar na impressora", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao imprimir")
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
                printer.disconnect()
            }
        }
    }
}

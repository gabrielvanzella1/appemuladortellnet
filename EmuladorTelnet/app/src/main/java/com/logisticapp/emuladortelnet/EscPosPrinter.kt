package com.logisticapp.emuladortelnet

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.logisticapp.emuladortelnet.settings.PrintOptions
import timber.log.Timber
import java.io.OutputStream
import java.net.Socket
import java.util.UUID

/**
 * Motor de impressão ESC/POS.
 *
 * Suporta dois meios de conexão:
 *  - Bluetooth (SPP — Serial Port Profile): impressoras térmicas BT clássico
 *  - Wi-Fi / TCP (porta 9100 — JetDirect): impressoras de rede
 *
 * Uso:
 *   val printer = EscPosPrinter()
 *   if (printer.connect(opts)) {
 *       printer.printLines(lines, opts)
 *       printer.disconnect()
 *   }
 */
class EscPosPrinter {

    private var btSocket: BluetoothSocket? = null
    private var wifiSocket: Socket? = null
    private var outputStream: OutputStream? = null

    // UUID padrão do Bluetooth Serial Port Profile (SPP)
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** Conecta à impressora conforme configuração. Retorna true se bem-sucedido. */
    fun connect(opts: PrintOptions): Boolean {
        return try {
            disconnect()
            if (opts.connectionType == "Bluetooth") connectBluetooth(opts)
            else connectWifi(opts)
        } catch (e: Exception) {
            Timber.e(e, "EscPosPrinter: falha ao conectar")
            false
        }
    }

    private fun connectBluetooth(opts: PrintOptions): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: throw IllegalStateException("Bluetooth não disponível neste dispositivo")
        if (opts.bluetoothAddress.isBlank())
            throw IllegalArgumentException("Nenhuma impressora Bluetooth configurada")
        val device: BluetoothDevice = adapter.getRemoteDevice(opts.bluetoothAddress)
        btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        adapter.cancelDiscovery()
        btSocket!!.connect()
        outputStream = btSocket!!.outputStream
        Timber.d("EscPosPrinter: conectado via Bluetooth a ${opts.bluetoothName} (${opts.bluetoothAddress})")
        return true
    }

    private fun connectWifi(opts: PrintOptions): Boolean {
        if (opts.wifiHost.isBlank())
            throw IllegalArgumentException("Endereço IP da impressora não configurado")
        wifiSocket = Socket(opts.wifiHost, opts.wifiPort)
        wifiSocket!!.soTimeout = opts.timeoutSeconds * 1000
        outputStream = wifiSocket!!.getOutputStream()
        Timber.d("EscPosPrinter: conectado via WiFi a ${opts.wifiHost}:${opts.wifiPort}")
        return true
    }

    /** Fecha a conexão com a impressora. */
    fun disconnect() {
        runCatching { outputStream?.close() }
        runCatching { btSocket?.close() }
        runCatching { wifiSocket?.close() }
        outputStream = null
        btSocket = null
        wifiSocket = null
    }

    /**
     * Imprime uma lista de linhas de texto (conteúdo do terminal).
     * Usa Font B (menor) para caber 80 colunas na maioria das impressoras térmicas.
     */
    fun printLines(lines: List<String>, opts: PrintOptions) {
        val out = outputStream ?: run {
            Timber.w("EscPosPrinter: sem conexão ativa")
            return
        }
        try {
            // Inicializar e configurar
            out.write(ESC_INIT)
            out.write(ALIGN_LEFT)
            out.write(FONT_SMALL)    // Font B: cabe mais colunas

            // Cabeçalho separador
            val separator = "-".repeat(42)
            out.write(separator.toByteArray(Charsets.ISO_8859_1))
            out.write(LF)

            // Linhas do terminal (remove somente linhas totalmente vazias no final)
            val trimmed = lines.dropLastWhile { it.isBlank() }
            for (line in trimmed) {
                val encoded = line.toByteArray(Charsets.ISO_8859_1)
                out.write(encoded)
                out.write(LF)
            }

            // Rodapé + alimentação + corte
            out.write(separator.toByteArray(Charsets.ISO_8859_1))
            out.write(LF)
            out.write(FEED_3_LINES)
            out.write(when (opts.printerType) {
                "Zebra ZPL" -> CUT_FULL     // Zebra prefere corte total
                else        -> CUT_PARTIAL  // ESC/POS padrão: corte parcial
            })
            out.flush()
            Timber.d("EscPosPrinter: ${trimmed.size} linhas impressas")
        } catch (e: Exception) {
            Timber.e(e, "EscPosPrinter: erro ao imprimir")
            throw e
        }
    }

    companion object {
        private val ESC_INIT     = byteArrayOf(0x1B, 0x40)           // ESC @ — init
        private val ALIGN_LEFT   = byteArrayOf(0x1B, 0x61, 0x00)     // ESC a 0 — alinhar esquerda
        private val FONT_SMALL   = byteArrayOf(0x1B, 0x4D, 0x01)     // ESC M 1 — Font B (menor)
        private val FONT_NORMAL  = byteArrayOf(0x1B, 0x4D, 0x00)     // ESC M 0 — Font A (normal)
        private val LF           = byteArrayOf(0x0A)                  // Line Feed
        private val FEED_3_LINES = byteArrayOf(0x1B, 0x64, 0x03)     // ESC d 3 — avança 3 linhas
        private val CUT_PARTIAL  = byteArrayOf(0x1D, 0x56, 0x01)     // GS V 1 — corte parcial
        private val CUT_FULL     = byteArrayOf(0x1D, 0x56, 0x00)     // GS V 0 — corte total

        /** Retorna lista de dispositivos Bluetooth pareados disponíveis. */
        fun getPairedDevices(): List<BluetoothDevice> {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            return adapter.bondedDevices?.toList() ?: emptyList()
        }
    }
}

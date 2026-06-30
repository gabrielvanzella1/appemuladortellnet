package com.logisticapp.emuladortelnet.settings

/**
 * Configurações de impressão ESC/POS (Configurações > Dispositivos > Configuração de impressão).
 * Persistido como JSON em AppSettings.
 */
data class PrintOptions(
    val printerType: String = "Padrão",
    val timeoutSeconds: Int = 5,
    val connectionType: String = "Bluetooth",  // "Bluetooth" | "WiFi"
    val bluetoothAddress: String = "",          // MAC address do dispositivo BT pareado
    val bluetoothName: String = "",             // Nome amigável para exibição
    val wifiHost: String = "",                  // IP da impressora na rede
    val wifiPort: Int = 9100                    // Porta JetDirect (padrão 9100)
)

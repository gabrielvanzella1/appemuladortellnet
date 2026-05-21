package com.logisticapp.emuladortelnet.license

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

/**
 * Gerenciador de Licença com Device Fingerprinting
 */
class LicenseManager(private val context: Context) {

    /**
     * Gera fingerprint único do device baseado em hardware
     * Combina: ANDROID_ID + Model + Manufacturer + Serial
     */
    fun generateDeviceFingerprint(): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            val serial = Build.SERIAL
            val fingerprint = Build.FINGERPRINT
            
            val combined = "$androidId|$model|$manufacturer|$serial|$fingerprint"
            
            // SHA-256 hash
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(combined.toByteArray())
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            
            Timber.d("Device Fingerprint gerado: ${sb.toString().take(16)}")
            sb.toString()
            
        } catch (e: Exception) {
            Timber.e(e, "Erro ao gerar device fingerprint")
            "UNKNOWN_DEVICE_${Build.SERIAL}".take(32)
        }
    }

    /**
     * Gera chave de licença de teste (30 dias)
     */
    fun generateTrialLicense(): String {
        val fingerprint = generateDeviceFingerprint()
        val expiryDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 30)
        }.timeInMillis / 1000  // segundos
        
        val licenseKey = "TRIAL-${fingerprint.take(16)}-$expiryDate"
        return licenseKey
    }

    /**
     * Gera chave de licença premium (1 ano)
     */
    fun generatePremiumLicense(): String {
        val fingerprint = generateDeviceFingerprint()
        val expiryDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, 1)
        }.timeInMillis / 1000  // segundos
        
        val licenseKey = "PREMIUM-${fingerprint.take(16)}-$expiryDate"
        return licenseKey
    }

    /**
     * Valida se a licença é válida
     * Retorna: Pair<isValid, mensagem>
     */
    fun validateLicense(licenseKey: String?): Pair<Boolean, String> {
        if (licenseKey == null || licenseKey.isEmpty()) {
            return Pair(false, "Licença não encontrada")
        }

        return try {
            val parts = licenseKey.split("-")
            if (parts.size < 3) {
                return Pair(false, "Formato de licença inválido")
            }

            val licenseType = parts[0]  // TRIAL ou PREMIUM
            val deviceFingerprint = parts[1]
            val expiryTimestamp = parts[2].toLong()

            val currentFingerprint = generateDeviceFingerprint().take(16)
            
            // Verificar se é o mesmo device
            if (deviceFingerprint != currentFingerprint) {
                Timber.w("Device fingerprint não coincide: esperado=$currentFingerprint, recebido=$deviceFingerprint")
                return Pair(false, "Licença registrada em outro device")
            }

            // Verificar expiração
            val currentTime = System.currentTimeMillis() / 1000
            if (currentTime > expiryTimestamp) {
                return Pair(false, "Licença expirada")
            }

            Timber.d("Licença válida: tipo=$licenseType")
            Pair(true, "Licença válida")
            
        } catch (e: Exception) {
            Timber.e(e, "Erro ao validar licença")
            Pair(false, "Erro ao validar licença: ${e.message}")
        }
    }

    /**
     * Formata timestamp para data legível
     */
    fun formatExpiryDate(expiryTimestamp: Long): String {
        return try {
            val date = Date(expiryTimestamp * 1000)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            format.format(date)
        } catch (e: Exception) {
            "Data inválida"
        }
    }

    /**
     * Retorna informações do device em texto
     */
    fun getDeviceInfo(): String {
        return """
            Device: ${Build.MODEL}
            Manufacturer: ${Build.MANUFACTURER}
            Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Fingerprint: ${generateDeviceFingerprint().take(16)}
        """.trimIndent()
    }
}

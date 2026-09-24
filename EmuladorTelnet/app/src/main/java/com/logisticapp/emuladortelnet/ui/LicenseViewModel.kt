package com.logisticapp.emuladortelnet.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.logisticapp.emuladortelnet.BuildConfig
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.license.LicenseApiService
import com.logisticapp.emuladortelnet.license.LicenseManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ViewModel da tela de gate (LicenseActivity): em segundo plano, re-valida a
 * licença salva com o servidor e envia o ping do dispositivo. A ativação é
 * sempre manual, por chave, na tela "Ativação" (ActivationActivity).
 *
 * As duas chamadas rodam em NonCancellable porque o gate costuma fechar logo
 * em seguida (vai direto pra HostsActivity quando o dispositivo já tem acesso).
 */
class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val licenseManager = LicenseManager(application)
    private val apiService = LicenseApiService()

    init {
        licenseManager.initializeLicense()
        syncWithServer()
        pingServidor()
    }

    /**
     * Re-valida a licença salva com o servidor.
     * Se foi revogada/expirada no admin, atualiza o estado local.
     * Se o servidor não responder, mantém o estado local (modo offline).
     */
    private fun syncWithServer() {
        val savedKey = licenseManager.getSavedLicenseKey() ?: return
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    val deviceId = licenseManager.getDeviceId()
                    val deviceNome = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val result = apiService.validarChave(savedKey, deviceId, deviceNome)
                    if (result.isSuccess) {
                        val validacao = result.getOrNull()!!
                        if (validacao.sucesso) {
                            licenseManager.upgradeToPremiumByKey(
                                chave = validacao.chave,
                                tipo = validacao.tipo,
                                diasRestantes = validacao.diasRestantes
                            )
                            // Aplica/atualiza a personalização da empresa (tema + teclas + logo)
                            com.logisticapp.emuladortelnet.settings.CompanyConfigStore
                                .save(getApplication(), validacao.configJson)
                        } else {
                            licenseManager.revokeLicense()
                            Timber.d("Licença inválida no servidor: ${validacao.erro}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Sem conexão com servidor — usando licença local")
                }
            }
        }
    }

    private fun pingServidor() {
        val deviceId   = licenseManager.getDeviceId()
        val deviceNome = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appVersion = BuildConfig.VERSION_NAME
        val licenseKey = licenseManager.getSavedLicenseKey()
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    // A resposta diz se a empresa usa a cerca digital: liga/desliga o envio de sinais
                    apiService.pingServidor(deviceId, deviceNome, appVersion, licenseKey)
                        .onSuccess { rastreamento ->
                            com.logisticapp.emuladortelnet.cerca.CercaReporter.definirAtiva(getApplication(), rastreamento)
                        }
                    Timber.d("Ping enviado com sucesso")
                } catch (e: Exception) {
                    Timber.w(e, "Ping ao servidor falhou: ${e.message}")
                }
            }
        }
    }
}

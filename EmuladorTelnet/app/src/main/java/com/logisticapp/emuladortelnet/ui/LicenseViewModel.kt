package com.logisticapp.emuladortelnet.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.logisticapp.emuladortelnet.BuildConfig
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.license.LicenseApiService
import com.logisticapp.emuladortelnet.license.LicenseManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val licenseManager = LicenseManager(application)
    private val apiService = LicenseApiService()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigateToMain = MutableLiveData(false)
    val navigateToMain: LiveData<Boolean> = _navigateToMain

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Pair(sucesso, mensagem) — consumido uma vez
    private val _activationResult = MutableLiveData<Pair<Boolean, String>?>(null)
    val activationResult: LiveData<Pair<Boolean, String>?> = _activationResult

    init {
        licenseManager.initializeLicense()
        pingServidor()
    }

    /**
     * Chamado no onCreate da Activity.
     * Se já tem licença PREMIUM, verifica com o servidor e navega para o app.
     */
    fun checkOnStartup() {
        if (!licenseManager.hasAccess()) return
        val savedKey = licenseManager.getSavedLicenseKey()
        if (savedKey == null) {
            // PREMIUM mas sem chave de servidor (ativação legada) — permite acesso
            _navigateToMain.value = true
            return
        }
        viewModelScope.launch {
            try {
                val deviceId = licenseManager.getDeviceId()
                val deviceNome = "${Build.MANUFACTURER} ${Build.MODEL}"
                val result = apiService.validarChave(savedKey, deviceId, deviceNome)
                if (result.isSuccess) {
                    val validacao = result.getOrNull()!!
                    if (validacao.sucesso) {
                        licenseManager.upgradeToPremiumByKey(validacao.chave, validacao.tipo, validacao.diasRestantes)
                        _navigateToMain.value = true
                    } else {
                        licenseManager.revokeLicense()
                        Timber.d("Licença revogada no servidor: ${validacao.erro}")
                    }
                } else {
                    // Servidor inacessível — modo offline, permite acesso
                    _navigateToMain.value = true
                }
            } catch (e: Exception) {
                Timber.w(e, "Sync startup falhou — usando licença local")
                _navigateToMain.value = true
            }
        }
    }

    /**
     * Ativa a licença via chave fornecida pelo usuário.
     * deviceName: nome/serial informado pelo usuário para identificar o dispositivo.
     */
    fun activateByKey(chave: String, deviceName: String) {
        if (chave.isBlank()) {
            _activationResult.value = Pair(false, "Digite a chave de licença.")
            return
        }
        if (deviceName.isBlank()) {
            _activationResult.value = Pair(false, "Digite o nome de identificação do dispositivo.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val deviceId = licenseManager.getDeviceId()
                val result = apiService.validarChave(chave.trim().uppercase(), deviceId, deviceName.trim())
                if (result.isSuccess) {
                    val validacao = result.getOrNull()!!
                    if (validacao.sucesso) {
                        licenseManager.upgradeToPremiumByKey(
                            chave = validacao.chave,
                            tipo = validacao.tipo,
                            diasRestantes = validacao.diasRestantes
                        )
                        _activationResult.value = Pair(true, "Licença ativada com sucesso!")
                        _navigateToMain.value = true
                    } else {
                        _activationResult.value = Pair(false, validacao.erro)
                    }
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Erro de conexão com o servidor."
                    _activationResult.value = Pair(false, "Não foi possível validar: $msg")
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao ativar licença por chave")
                _activationResult.value = Pair(false, "Erro ao ativar licença.")
            } finally {
                _isLoading.value = false
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
                    apiService.pingServidor(deviceId, deviceNome, appVersion, licenseKey)
                    Timber.d("Ping enviado com sucesso")
                } catch (e: Exception) {
                    Timber.w(e, "Ping falhou: ${e.message}")
                }
            }
        }
    }

    fun onActivationResultShown() { _activationResult.value = null }
    fun onErrorShown() { _errorMessage.value = null }
    fun onNavigated() { _navigateToMain.value = false }
}

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
import com.logisticapp.emuladortelnet.license.MercadoPagoManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val licenseManager = LicenseManager(application)
    private val mpManager = MercadoPagoManager()
    private val apiService = LicenseApiService()

    private val _licenseStatus = MutableLiveData<String>()
    val licenseStatus: LiveData<String> = _licenseStatus

    private val _licenseMessage = MutableLiveData<String>()
    val licenseMessage: LiveData<String> = _licenseMessage

    private val _daysRemaining = MutableLiveData<Int>()
    val daysRemaining: LiveData<Int> = _daysRemaining

    private val _licenseType = MutableLiveData<String>()
    val licenseType: LiveData<String> = _licenseType

    private val _deviceInfo = MutableLiveData<String>()
    val deviceInfo: LiveData<String> = _deviceInfo

    private val _canContinue = MutableLiveData<Boolean>()
    val canContinue: LiveData<Boolean> = _canContinue

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigateToMain = MutableLiveData(false)
    val navigateToMain: LiveData<Boolean> = _navigateToMain

    // URL do checkout MP para abrir no browser (consumida uma vez)
    private val _checkoutUrl = MutableLiveData<String?>(null)
    val checkoutUrl: LiveData<String?> = _checkoutUrl

    // Mensagem de erro para exibir Toast (consumida uma vez)
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Resultado da ativação por chave: Pair(sucesso, mensagem)
    private val _activationResult = MutableLiveData<Pair<Boolean, String>?>(null)
    val activationResult: LiveData<Pair<Boolean, String>?> = _activationResult

    init {
        licenseManager.initializeLicense()
        loadLicenseInfo()
        _deviceInfo.value = licenseManager.getDeviceInfo()
        syncWithServer()
        pingServidor()
    }

    fun loadLicenseInfo() {
        try {
            val info = licenseManager.getLicenseInfo()
            _licenseStatus.value = info.status
            _licenseMessage.value = info.message
            _daysRemaining.value = info.daysRemaining
            _canContinue.value = !info.isExpired
            _licenseType.value = when {
                info.status == "PREMIUM" && info.daysRemaining == -1 -> "Vitalício"
                info.status == "PREMIUM" -> licenseManager.getLicenseSubtype()
                    .replaceFirstChar { it.uppercaseChar() }
                info.status == "TRIAL" -> "Teste"
                else -> "Expirado"
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao carregar licença")
            _errorMessage.value = "Erro ao carregar informações da licença"
            _canContinue.value = false
        }
    }

    /**
     * Re-valida a licença salva com o servidor.
     * Se foi revogada/expirada no admin, atualiza o estado local.
     * Se o servidor não responder, mantém o estado local (modo offline).
     */
    private fun syncWithServer() {
        val savedKey = licenseManager.getSavedLicenseKey() ?: return
        viewModelScope.launch {
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
                    } else {
                        licenseManager.revokeLicense()
                        Timber.d("Licença inválida no servidor: ${validacao.erro}")
                    }
                    loadLicenseInfo()
                }
            } catch (e: Exception) {
                Timber.w(e, "Sem conexão com servidor — usando licença local")
            }
        }
    }

    private fun pingServidor() {
        val deviceId   = licenseManager.getDeviceId()
        val deviceNome = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appVersion = BuildConfig.VERSION_NAME
        val licenseKey = licenseManager.getSavedLicenseKey()
        viewModelScope.launch {
            // NonCancellable garante que o ping termina mesmo se a Activity for destruída
            withContext(NonCancellable) {
                try {
                    apiService.pingServidor(deviceId, deviceNome, appVersion, licenseKey)
                    Timber.d("Ping enviado com sucesso")
                } catch (e: Exception) {
                    Timber.w(e, "Ping ao servidor falhou: ${e.message}")
                }
            }
        }
    }

    fun continueToApp() {
        if (licenseManager.hasAccess()) {
            _navigateToMain.value = true
        } else {
            _errorMessage.value = "Trial expirado. Adquira a licença para continuar."
        }
    }

    /**
     * Inicia o fluxo de compra: cria preferência no MP e emite a URL do checkout.
     */
    fun startPayment() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val deviceId = licenseManager.getDeviceId()
                val result = mpManager.createPreference(deviceId)
                if (result.isSuccess) {
                    _checkoutUrl.value = result.getOrNull()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message
                        ?: "Erro ao iniciar pagamento. Verifique sua conexão."
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao iniciar pagamento")
                _errorMessage.value = "Erro ao iniciar pagamento."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Chamado quando o deep link de sucesso retorna um payment_id.
     * Verifica o status real no MP antes de ativar.
     */
    fun verifyAndActivateLicense(paymentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _licenseMessage.value = "Verificando pagamento..."
            try {
                val result = mpManager.getPaymentStatus(paymentId)
                if (result.isSuccess) {
                    val info = result.getOrNull()!!
                    if (info.status == "approved") {
                        licenseManager.upgradeToPremium(info.orderId, info.id)
                        loadLicenseInfo()
                        _licenseMessage.value = "Licença ativada com sucesso!"
                        Timber.d("Licença PREMIUM ativada via pagamento $paymentId")
                    } else {
                        _errorMessage.value = "Pagamento com status: ${info.status}. Aguarde a aprovação."
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message
                        ?: "Não foi possível verificar o pagamento."
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao verificar pagamento")
                _errorMessage.value = "Erro ao verificar pagamento."
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fallback: MP redireciona com status=approved mas sem payment_id.
     * Ativa a licença diretamente pelo status do redirect.
     */
    fun activateLicenseByStatus(status: String) {
        if (status == "approved") {
            licenseManager.upgradeToPremium("mp_redirect", "mp_redirect")
            loadLicenseInfo()
            _licenseMessage.value = "Licença ativada com sucesso!"
            Timber.d("Licença PREMIUM ativada via redirect status=approved")
        } else {
            _errorMessage.value = "Pagamento com status: $status."
        }
    }

    /**
     * Ativa a licença via chave do scante-admin.
     * Chama POST /api/licenca/validar com a chave, device_id e device_nome.
     */
    fun activateByKey(chave: String) {
        if (chave.isBlank()) {
            _activationResult.value = Pair(false, "Digite a chave de licença.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val deviceId = licenseManager.getDeviceId()
                val deviceNome = "${Build.MANUFACTURER} ${Build.MODEL}"
                val result = apiService.validarChave(chave, deviceId, deviceNome)
                if (result.isSuccess) {
                    val validacao = result.getOrNull()!!
                    if (validacao.sucesso) {
                        licenseManager.upgradeToPremiumByKey(
                            chave = validacao.chave,
                            tipo = validacao.tipo,
                            diasRestantes = validacao.diasRestantes
                        )
                        loadLicenseInfo()
                        _activationResult.value = Pair(true, "Licença ativada com sucesso!")
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

    fun onActivationResultShown() {
        _activationResult.value = null
    }

    fun onCheckoutOpened() {
        _checkoutUrl.value = null
    }

    fun onErrorShown() {
        _errorMessage.value = null
    }
}

package com.logisticapp.emuladortelnet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.license.LicenseManager
import com.logisticapp.emuladortelnet.license.MercadoPagoManager
import kotlinx.coroutines.launch
import timber.log.Timber

class LicenseViewModel(application: Application) : AndroidViewModel(application) {

    private val licenseManager = LicenseManager(application)
    private val mpManager = MercadoPagoManager()

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

    init {
        licenseManager.initializeLicense()
        loadLicenseInfo()
        _deviceInfo.value = licenseManager.getDeviceInfo()
    }

    fun loadLicenseInfo() {
        try {
            val info = licenseManager.getLicenseInfo()
            _licenseStatus.value = info.status
            _licenseMessage.value = info.message
            _daysRemaining.value = info.daysRemaining
            _canContinue.value = !info.isExpired
            _licenseType.value = when (info.status) {
                "PREMIUM" -> "Vitalício"
                "TRIAL" -> "Teste"
                else -> "Expirado"
            }
        } catch (e: Exception) {
            Timber.e(e, "Erro ao carregar licença")
            _errorMessage.value = "Erro ao carregar informações da licença"
            _canContinue.value = false
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

    fun onCheckoutOpened() {
        _checkoutUrl.value = null
    }

    fun onErrorShown() {
        _errorMessage.value = null
    }
}

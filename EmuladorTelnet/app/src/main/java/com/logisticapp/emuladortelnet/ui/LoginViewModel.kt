package com.logisticapp.emuladortelnet.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.database.User
import com.logisticapp.emuladortelnet.utils.PasswordUtils
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Estados de login
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * ViewModel para tela de login
 */
class LoginViewModel(private val repository: TelnetRepository) : ViewModel() {
    
    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState
    
    private val _email = MutableLiveData<String>("")
    val email: LiveData<String> = _email
    
    private val _password = MutableLiveData<String>("")
    val password: LiveData<String> = _password
    
    private val _errorMessage = MutableLiveData<String>("")
    val errorMessage: LiveData<String> = _errorMessage
    
    /**
     * Atualiza o email
     */
    fun setEmail(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = ""
    }
    
    /**
     * Atualiza a senha
     */
    fun setPassword(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = ""
    }
    
    /**
     * Tenta fazer login com email e senha
     */
    fun login(email: String, password: String) {
        // Validações
        if (email.isBlank()) {
            _errorMessage.value = "Por favor, insira seu email"
            return
        }
        
        if (!PasswordUtils.isValidEmail(email)) {
            _errorMessage.value = "Email inválido"
            return
        }
        
        if (password.isBlank()) {
            _errorMessage.value = "Por favor, insira sua senha"
            return
        }
        
        if (password.length < 6) {
            _errorMessage.value = "Senha deve ter no mínimo 6 caracteres"
            return
        }
        
        // Iniciar login
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                val user = repository.authenticateUser(email, password)
                
                if (user != null) {
                    Timber.d("Login bem-sucedido: ${user.email}")
                    _loginState.value = LoginState.Success(user)
                } else {
                    Timber.w("Falha no login: credenciais inválidas")
                    _errorMessage.value = "Email ou senha incorretos"
                    _loginState.value = LoginState.Error("Email ou senha incorretos")
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro durante o login")
                _errorMessage.value = "Erro ao realizar login: ${e.message}"
                _loginState.value = LoginState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
    
    /**
     * Cria usuário de teste
     */
    fun createTestUser() {
        viewModelScope.launch {
            try {
                repository.createTestUser()
                Timber.d("Usuário de teste criado/verificado")
            } catch (e: Exception) {
                Timber.e(e, "Erro ao criar usuário de teste")
            }
        }
    }
}

/**
 * Factory para LoginViewModel
 */
class LoginViewModelFactory(private val repository: TelnetRepository) : 
    androidx.lifecycle.ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

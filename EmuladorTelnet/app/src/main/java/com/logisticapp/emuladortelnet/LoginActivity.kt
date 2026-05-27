package com.logisticapp.emuladortelnet

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.logisticapp.emuladortelnet.auth.AuthManager
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.ui.LoginState
import com.logisticapp.emuladortelnet.ui.LoginViewModel
import com.logisticapp.emuladortelnet.ui.LoginViewModelFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var errorMessage: TextView
    private lateinit var viewModel: LoginViewModel
    private lateinit var authManager: AuthManager
    private lateinit var repository: TelnetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar componentes
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        errorMessage = findViewById(R.id.errorMessage)

        // Inicializar Repository e ViewModel
        repository = TelnetRepository.getInstance(this)
        val factory = LoginViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)

        // Inicializar AuthManager
        authManager = AuthManager(this)

        // Setup listeners
        setupListeners()

        // Observar mudanças do ViewModel
        observeViewModel()

        // Pré-preencher email se houver
        emailInput.setText("teste@auticode.com.br")
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            // Esconder teclado
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(loginButton.windowToken, 0)

            // Executar login
            viewModel.login(email, password)
        }

        // Enter key para fazer login
        passwordInput.setOnEditorActionListener { _, _, _ ->
            loginButton.performClick()
            true
        }
    }

    private fun observeViewModel() {
        // Observar estado de login
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle -> {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                }
                is LoginState.Loading -> {
                    loginButton.isEnabled = false
                    loginButton.text = "Entrando..."
                }
                is LoginState.Success -> {
                    Timber.d("Login bem-sucedido: ${state.user.email}")
                    // Salvar sessão
                    authManager.saveLoginSession(state.user)
                    // Ir para MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginState.Error -> {
                    loginButton.isEnabled = true
                    loginButton.text = "Entrar"
                    Timber.e("Erro no login: ${state.message}")
                }
            }
        }

        // Observar mensagem de erro
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotBlank()) {
                errorMessage.text = message
                errorMessage.visibility = View.VISIBLE
            } else {
                errorMessage.visibility = View.GONE
            }
        }
    }
}

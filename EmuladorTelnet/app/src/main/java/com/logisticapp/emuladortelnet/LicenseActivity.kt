package com.logisticapp.emuladortelnet

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.logisticapp.emuladortelnet.license.LicenseManager
import com.logisticapp.emuladortelnet.ui.LicenseViewModel
import timber.log.Timber

class LicenseActivity : AppCompatActivity() {

    private lateinit var viewModel: LicenseViewModel

    private lateinit var licenseStatus: TextView
    private lateinit var licenseMessage: TextView
    private lateinit var daysRemaining: TextView
    private lateinit var licenseType: TextView
    private lateinit var deviceInfo: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnBuyLicense: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var inputLicenseKey: EditText
    private lateinit var btnActivateKey: Button
    private lateinit var tvActivationResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.logisticapp.emuladortelnet.settings.AppSettings.get(this).applyOrientation(this)
        setContentView(R.layout.activity_license)

        initializeViews()
        viewModel = ViewModelProvider(this).get(LicenseViewModel::class.java)
        observeLicenseState()
        setupClickListeners()

        // Chegou aqui via deep link (pagamento concluído)?
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Recebe o deep link quando a activity já estava aberta (singleTop)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data == null || data.scheme != "emuladortelnet") return

        Timber.d("Deep link recebido: $data")

        when (data.host) {
            "payment" -> {
                val path      = data.pathSegments.firstOrNull()
                val chave     = data.getQueryParameter("chave")       // retorno do scante-admin
                val paymentId = data.getQueryParameter("payment_id")  // retorno do Mercado Pago
                val status    = data.getQueryParameter("status")

                Timber.d("Path=$path chave=$chave paymentId=$paymentId status=$status")

                when (path) {
                    "sucesso",   // rota do scante-admin
                    "success" -> {
                        when {
                            !chave.isNullOrEmpty()     -> viewModel.activateByKey(chave)
                            !paymentId.isNullOrEmpty() -> viewModel.verifyAndActivateLicense(paymentId)
                            else                       -> viewModel.activateLicenseByStatus(status ?: "approved")
                        }
                    }
                    "failure" -> {
                        Toast.makeText(this, "Pagamento não aprovado. Tente novamente.", Toast.LENGTH_LONG).show()
                    }
                    "pending" -> {
                        Toast.makeText(this, "Pagamento pendente. Aguarde a confirmação.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun initializeViews() {
        licenseStatus = findViewById(R.id.license_status)
        licenseMessage = findViewById(R.id.license_message)
        daysRemaining = findViewById(R.id.days_remaining)
        licenseType = findViewById(R.id.license_type)
        deviceInfo = findViewById(R.id.device_info)
        btnContinue = findViewById(R.id.btn_continue)
        btnBuyLicense = findViewById(R.id.btn_buy_license)
        progressBar = findViewById(R.id.progress_bar)
        inputLicenseKey = findViewById(R.id.input_license_key)
        btnActivateKey = findViewById(R.id.btn_activate_key)
        tvActivationResult = findViewById(R.id.tv_activation_result)
    }

    private fun observeLicenseState() {
        viewModel.licenseStatus.observe(this) { status ->
            licenseStatus.text = "Status: $status"
        }

        viewModel.licenseMessage.observe(this) { message ->
            licenseMessage.text = message
        }

        viewModel.daysRemaining.observe(this) { days ->
            daysRemaining.text = if (days >= 0) days.toString() else "∞"
        }

        viewModel.licenseType.observe(this) { type ->
            licenseType.text = type
        }

        viewModel.deviceInfo.observe(this) { info ->
            deviceInfo.text = info
        }

        viewModel.canContinue.observe(this) { canContinue ->
            btnContinue.isEnabled = canContinue
            btnContinue.alpha = if (canContinue) 1f else 0.5f
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnBuyLicense.isEnabled = !loading
        }

        viewModel.navigateToMain.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, HostsActivity::class.java))
                finish()
            }
        }

        viewModel.checkoutUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                openCheckout(url)
                viewModel.onCheckoutOpened()
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }

        viewModel.activationResult.observe(this) { result ->
            result ?: return@observe
            tvActivationResult.visibility = View.VISIBLE
            tvActivationResult.text = result.second
            tvActivationResult.setTextColor(
                if (result.first) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )
            if (result.first) inputLicenseKey.setText("")
            viewModel.onActivationResultShown()
        }
    }

    private fun setupClickListeners() {
        btnContinue.setOnClickListener {
            viewModel.continueToApp()
        }

        btnBuyLicense.setOnClickListener {
            viewModel.startPayment()
        }

        btnActivateKey.setOnClickListener {
            val chave = inputLicenseKey.text.toString()
            tvActivationResult.visibility = View.GONE
            viewModel.activateByKey(chave)
        }

        // Painel de debug — só aparece em builds DEBUG
        if (BuildConfig.DEBUG) {
            val debugPanel = findViewById<LinearLayout>(R.id.debug_panel)
            debugPanel.visibility = View.VISIBLE

            val licenseManager = LicenseManager(this)

            findViewById<Button>(R.id.btn_debug_expire).setOnClickListener {
                licenseManager.debugExpireTrial()
                viewModel.loadLicenseInfo()
                Toast.makeText(this, "Trial expirado!", Toast.LENGTH_SHORT).show()
            }

            findViewById<Button>(R.id.btn_debug_reset).setOnClickListener {
                licenseManager.debugSetTrialDays(7)
                viewModel.loadLicenseInfo()
                Toast.makeText(this, "Trial resetado para 7 dias", Toast.LENGTH_SHORT).show()
            }

            val debugDaysInput = findViewById<EditText>(R.id.debug_days_input)
            findViewById<Button>(R.id.btn_debug_set_days).setOnClickListener {
                val days = debugDaysInput.text.toString().toIntOrNull() ?: 0
                if (days <= 0) {
                    Toast.makeText(this, "Informe um número de dias válido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                licenseManager.debugSetTrialDays(days)
                viewModel.loadLicenseInfo()
                Toast.makeText(this, "Trial definido para $days dias", Toast.LENGTH_SHORT).show()
            }

            findViewById<Button>(R.id.btn_debug_clear).setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Limpar tudo?")
                    .setMessage("Isso apaga a licença e simula uma primeira instalação. Confirma?")
                    .setPositiveButton("Limpar") { _, _ ->
                        licenseManager.debugClearAll()
                        Toast.makeText(this, "Dados apagados — reinicie o app", Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun openCheckout(url: String) {
        try {
            // Abre o checkout do Mercado Pago no browser padrão
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir o navegador.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadLicenseInfo()
    }
}

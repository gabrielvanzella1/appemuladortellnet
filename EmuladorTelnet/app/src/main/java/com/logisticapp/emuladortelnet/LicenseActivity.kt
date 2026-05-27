package com.logisticapp.emuladortelnet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val path = data.pathSegments.firstOrNull()
                val paymentId = data.getQueryParameter("payment_id")
                val status = data.getQueryParameter("status")

                Timber.d("Path=$path paymentId=$paymentId status=$status")

                when (path) {
                    "success" -> {
                        if (!paymentId.isNullOrEmpty()) {
                            viewModel.verifyAndActivateLicense(paymentId)
                        } else {
                            // MP às vezes não envia payment_id no back_url, ativa via status
                            viewModel.activateLicenseByStatus(status ?: "approved")
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
    }

    private fun setupClickListeners() {
        btnContinue.setOnClickListener {
            viewModel.continueToApp()
        }

        btnBuyLicense.setOnClickListener {
            viewModel.startPayment()
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

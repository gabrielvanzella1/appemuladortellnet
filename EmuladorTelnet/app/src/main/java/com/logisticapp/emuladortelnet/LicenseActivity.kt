package com.logisticapp.emuladortelnet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.logisticapp.emuladortelnet.ui.LicenseViewModel
import com.google.android.material.textfield.TextInputEditText

class LicenseActivity : AppCompatActivity() {

    private enum class Screen { NOT_ACTIVATED, FORM, HELP }

    private lateinit var viewModel: LicenseViewModel
    private lateinit var toolbar: Toolbar

    private lateinit var screenNotActivated: LinearLayout
    private lateinit var screenForm: LinearLayout
    private lateinit var screenHelp: ScrollView
    private lateinit var inputLicenseKey: TextInputEditText
    private lateinit var inputDeviceName: TextInputEditText
    private lateinit var btnAtivarAgora: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvActivationResult: TextView

    private var currentScreen = Screen.NOT_ACTIVATED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.logisticapp.emuladortelnet.settings.AppSettings.get(this).applyOrientation(this)
        setContentView(R.layout.activity_license)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        screenNotActivated = findViewById(R.id.screen_not_activated)
        screenForm = findViewById(R.id.screen_form)
        screenHelp = findViewById(R.id.screen_help)
        inputLicenseKey = findViewById(R.id.input_license_key)
        inputDeviceName = findViewById(R.id.input_device_name)
        btnAtivarAgora = findViewById(R.id.btn_ativar_agora)
        progressBar = findViewById(R.id.progress_bar)
        tvActivationResult = findViewById(R.id.tv_activation_result)

        viewModel = ViewModelProvider(this).get(LicenseViewModel::class.java)
        observeViewModel()
        setupClickListeners()

        viewModel.checkOnStartup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_license, menu)
        menu.findItem(R.id.menu_ativar)?.actionView
            ?.findViewById<View>(R.id.btn_toolbar_ativar)
            ?.setOnClickListener { showScreen(Screen.FORM) }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_ativar)?.isVisible = (currentScreen == Screen.NOT_ACTIVATED)
        menu.findItem(R.id.menu_ajuda)?.isVisible = (currentScreen == Screen.FORM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { navigateBack(); true }
            R.id.menu_ajuda -> { showScreen(Screen.HELP); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (!navigateBack()) super.onBackPressed()
    }

    private fun navigateBack(): Boolean {
        return when (currentScreen) {
            Screen.FORM -> { showScreen(Screen.NOT_ACTIVATED); true }
            Screen.HELP -> { showScreen(Screen.FORM); true }
            Screen.NOT_ACTIVATED -> false
        }
    }

    private fun showScreen(screen: Screen) {
        currentScreen = screen
        screenNotActivated.visibility = if (screen == Screen.NOT_ACTIVATED) View.VISIBLE else View.GONE
        screenForm.visibility = if (screen == Screen.FORM) View.VISIBLE else View.GONE
        screenHelp.visibility = if (screen == Screen.HELP) View.VISIBLE else View.GONE

        when (screen) {
            Screen.NOT_ACTIVATED -> {
                supportActionBar?.title = "ScanTE não está ativado"
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
            Screen.FORM -> {
                supportActionBar?.title = "Ativação"
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                tvActivationResult.visibility = View.GONE
            }
            Screen.HELP -> {
                supportActionBar?.title = "AJUDA"
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }
        invalidateOptionsMenu()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnAtivarAgora.isEnabled = !loading
        }

        viewModel.navigateToMain.observe(this) { navigate ->
            if (navigate) {
                viewModel.onNavigated()
                startActivity(Intent(this, HostsActivity::class.java))
                finish()
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
            viewModel.onActivationResultShown()
        }
    }

    private fun setupClickListeners() {
        btnAtivarAgora.setOnClickListener {
            val chave = inputLicenseKey.text?.toString().orEmpty()
            val deviceName = inputDeviceName.text?.toString().orEmpty()
            tvActivationResult.visibility = View.GONE
            viewModel.activateByKey(chave, deviceName)
        }

        if (BuildConfig.DEBUG) {
            setupDebugPanel()
        }
    }

    private fun setupDebugPanel() {
        // Panel de debug acessível via toque longo no título da tela 1
        screenNotActivated.setOnLongClickListener {
            showDebugDialog()
            true
        }
    }

    private fun showDebugDialog() {
        val manager = com.logisticapp.emuladortelnet.license.LicenseManager(this)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("DEBUG — Licença")
            .setItems(arrayOf(
                "Simular licença PREMIUM ativa",
                "Limpar tudo (1ª instalação)",
                "Ver device ID"
            )) { _, which ->
                when (which) {
                    0 -> {
                        manager.upgradeToPremiumByKey("SCTE-DEBUG-000000-000000", "debug", -1)
                        Toast.makeText(this, "PREMIUM simulado", Toast.LENGTH_SHORT).show()
                        viewModel.checkOnStartup()
                    }
                    1 -> {
                        manager.debugClearAll()
                        Toast.makeText(this, "Dados apagados — reinicie o app", Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                    2 -> {
                        Toast.makeText(this, "Device ID: ${manager.getDeviceId()}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        showScreen(currentScreen)
    }
}

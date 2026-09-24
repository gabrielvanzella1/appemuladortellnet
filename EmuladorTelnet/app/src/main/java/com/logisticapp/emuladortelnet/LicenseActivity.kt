package com.logisticapp.emuladortelnet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.logisticapp.emuladortelnet.license.LicenseManager
import com.logisticapp.emuladortelnet.ui.LicenseViewModel

/**
 * Gate de licença. Se o dispositivo já tem acesso, vai direto para
 * HostsActivity. Caso contrário, mostra o aviso "ScanTE não está ativado"
 * com atalho para a tela de Ativação (ativação sempre manual, por chave).
 *
 * Em ambos os casos cria o LicenseViewModel, que re-valida a licença e manda
 * o ping do dispositivo em segundo plano.
 */
class LicenseActivity : AppCompatActivity() {

    private lateinit var licenseManager: LicenseManager
    private var navigatingToHosts = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.logisticapp.emuladortelnet.settings.AppSettings.get(this).applyOrientation(this)

        licenseManager = LicenseManager(this)
        ViewModelProvider(this).get(LicenseViewModel::class.java)

        if (licenseManager.hasAccess()) {
            goToHosts()
            return
        }

        setContentView(R.layout.activity_license)
        setupClickListeners()
    }

    private fun goToHosts() {
        if (navigatingToHosts) return
        navigatingToHosts = true
        startActivity(Intent(this, HostsActivity::class.java))
        finish()
    }

    private fun setupClickListeners() {
        findViewById<TextView>(R.id.btn_activate_now).setOnClickListener {
            startActivity(Intent(this, ActivationActivity::class.java))
        }

        // Painel de debug — desativado por hora (confundia teste de ativação por chave real)
        if (false && BuildConfig.DEBUG) {
            val debugPanel = findViewById<LinearLayout>(R.id.debug_panel)
            debugPanel.visibility = android.view.View.VISIBLE

            findViewById<Button>(R.id.btn_debug_unlock).setOnClickListener {
                licenseManager.debugUnlock()
                Toast.makeText(this, "Liberado (bypass de teste)", Toast.LENGTH_SHORT).show()
                goToHosts()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Se a licença foi ativada em outra tela (ex: Ativação) enquanto essa
        // ficou em segundo plano, reavalia o gate ao voltar pra ela.
        if (::licenseManager.isInitialized && licenseManager.hasAccess()) {
            goToHosts()
        }
    }
}

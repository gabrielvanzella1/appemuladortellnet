package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.ProxyOptions

/**
 * Configuracoes > Comunicacao > Servidor proxy.
 */
class ProxyActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: ProxyOptions

    private lateinit var ckUseServer: CheckBox
    private lateinit var inAddress: EditText
    private lateinit var inPort: EditText
    private lateinit var ckSecure: CheckBox
    private lateinit var inKeepUser: EditText
    private lateinit var inKeepLost: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_proxy)
        opts = settings.proxyOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ckUseServer = findViewById(R.id.ck_use_server)
        inAddress   = findViewById(R.id.in_address)
        inPort      = findViewById(R.id.in_port)
        ckSecure    = findViewById(R.id.ck_secure)
        inKeepUser  = findViewById(R.id.in_keep_user)
        inKeepLost  = findViewById(R.id.in_keep_lost)

        loadValues()

        findViewById<View>(R.id.row_use_server).setOnClickListener { ckUseServer.toggle() }
        findViewById<View>(R.id.row_secure).setOnClickListener { ckSecure.toggle() }
    }

    private fun loadValues() {
        ckUseServer.isChecked = opts.useServer
        inAddress.setText(opts.address)
        inPort.setText(opts.port)
        ckSecure.isChecked = opts.secureComm
        inKeepUser.setText(opts.keepOnUserDisconnect)
        inKeepLost.setText(opts.keepOnConnectionLost)
    }

    override fun onPause() {
        super.onPause()
        settings.proxyOptions = ProxyOptions(
            useServer = ckUseServer.isChecked,
            address = inAddress.text.toString(),
            port = inPort.text.toString(),
            secureComm = ckSecure.isChecked,
            keepOnUserDisconnect = inKeepUser.text.toString(),
            keepOnConnectionLost = inKeepLost.text.toString()
        )
    }
}

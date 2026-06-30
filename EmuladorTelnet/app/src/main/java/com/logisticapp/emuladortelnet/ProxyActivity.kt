package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.ProxyOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.Socket

/**
 * Configuracoes > Comunicacao > Servidor proxy.
 * HTTP CONNECT proxy com autenticacao Basic opcional.
 */
class ProxyActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings

    private lateinit var ckUseServer: CheckBox
    private lateinit var inAddress: EditText
    private lateinit var inPort: EditText
    private lateinit var ckSecure: CheckBox
    private lateinit var inUsername: EditText
    private lateinit var inPassword: EditText
    private lateinit var inKeepUser: EditText
    private lateinit var inKeepLost: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_proxy)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        ckUseServer = findViewById(R.id.ck_use_server)
        inAddress   = findViewById(R.id.in_address)
        inPort      = findViewById(R.id.in_port)
        ckSecure    = findViewById(R.id.ck_secure)
        inUsername  = findViewById(R.id.in_username)
        inPassword  = findViewById(R.id.in_password)
        inKeepUser  = findViewById(R.id.in_keep_user)
        inKeepLost  = findViewById(R.id.in_keep_lost)

        loadValues()

        findViewById<View>(R.id.row_use_server).setOnClickListener { ckUseServer.toggle() }
        findViewById<View>(R.id.row_secure).setOnClickListener { ckSecure.toggle() }
        findViewById<View>(R.id.row_test_proxy).setOnClickListener { testProxy() }
    }

    private fun loadValues() {
        val opts = settings.proxyOptions
        ckUseServer.isChecked = opts.useServer
        inAddress.setText(opts.address)
        inPort.setText(opts.port)
        ckSecure.isChecked = opts.secureComm
        inUsername.setText(opts.username)
        inPassword.setText(opts.password)
        inKeepUser.setText(opts.keepOnUserDisconnect)
        inKeepLost.setText(opts.keepOnConnectionLost)
    }

    private fun saveValues() {
        settings.proxyOptions = ProxyOptions(
            useServer            = ckUseServer.isChecked,
            address              = inAddress.text.toString().trim(),
            port                 = inPort.text.toString().ifBlank { "3128" },
            secureComm           = ckSecure.isChecked,
            username             = inUsername.text.toString().trim(),
            password             = inPassword.text.toString(),
            keepOnUserDisconnect = inKeepUser.text.toString().ifBlank { "0" },
            keepOnConnectionLost = inKeepLost.text.toString().ifBlank { "300" }
        )
    }

    /** Testa a conexão TCP ao proxy (sem fazer CONNECT — apenas verifica alcançabilidade). */
    private fun testProxy() {
        saveValues()
        val host = inAddress.text.toString().trim()
        val port = inPort.text.toString().toIntOrNull() ?: 3128

        if (host.isBlank()) {
            Toast.makeText(this, "Informe o endereço do proxy", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Testando conexão ao proxy $host:$port…", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching {
                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(host, port), 5_000)
                socket.close()
                "OK"
            }
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Toast.makeText(
                        this@ProxyActivity,
                        "✓ Proxy alcançável em $host:$port",
                        Toast.LENGTH_LONG
                    ).show()
                    Timber.d("Teste de proxy OK: $host:$port")
                } else {
                    val msg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    Toast.makeText(
                        this@ProxyActivity,
                        "✗ Proxy inacessível: $msg",
                        Toast.LENGTH_LONG
                    ).show()
                    Timber.w("Teste de proxy FALHOU: $host:$port — $msg")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveValues()
    }
}

package com.logisticapp.emuladortelnet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.logisticapp.emuladortelnet.database.SavedConnection
import com.logisticapp.emuladortelnet.database.TelnetRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class HostConfigActivity : AppCompatActivity() {

    private lateinit var repository: TelnetRepository
    private var existingHost: SavedConnection? = null

    private lateinit var radioConnectionType: RadioGroup
    private lateinit var radioTelnet: android.widget.RadioButton
    private lateinit var radioBrowser: android.widget.RadioButton
    private lateinit var groupTelnetFields: LinearLayout
    private lateinit var groupBrowserFields: LinearLayout
    private lateinit var inputName: EditText
    private lateinit var inputHost: EditText
    private lateinit var inputPort: EditText
    private lateinit var inputUrl: EditText
    private lateinit var btnSave: Button
    private lateinit var btnConnect: Button

    companion object {
        const val EXTRA_HOST_ID = "host_id"
        const val EXTRA_PREFILL_NAME = "prefill_name"
        const val EXTRA_PREFILL_HOST = "prefill_host"
        const val EXTRA_PREFILL_PORT = "prefill_port"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_config)

        repository = TelnetRepository.getInstance(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        radioConnectionType = findViewById(R.id.radio_connection_type)
        radioTelnet = findViewById(R.id.radio_telnet)
        radioBrowser = findViewById(R.id.radio_browser)
        groupTelnetFields = findViewById(R.id.group_telnet_fields)
        groupBrowserFields = findViewById(R.id.group_browser_fields)
        inputName = findViewById(R.id.input_name)
        inputHost = findViewById(R.id.input_host)
        inputPort = findViewById(R.id.input_port)
        inputUrl = findViewById(R.id.input_url)
        btnSave = findViewById(R.id.btn_save)
        btnConnect = findViewById(R.id.btn_connect)

        inputPort.setText("23")

        radioConnectionType.setOnCheckedChangeListener { _, _ -> updateFieldVisibility() }

        val hostId = intent.getIntExtra(EXTRA_HOST_ID, -1)
        if (hostId > 0) {
            loadExistingHost(hostId)
        } else {
            supportActionBar?.title = "Novo Host"
            // Pre-fill from template (if launched from TemplatesActivity)
            intent.getStringExtra(EXTRA_PREFILL_NAME)?.let { inputName.setText(it) }
            intent.getStringExtra(EXTRA_PREFILL_HOST)?.let { inputHost.setText(it) }
            val prefillPort = intent.getIntExtra(EXTRA_PREFILL_PORT, -1)
            if (prefillPort > 0) inputPort.setText(prefillPort.toString())
        }

        updateFieldVisibility()
        btnSave.setOnClickListener { saveHost() }
        btnConnect.setOnClickListener { saveAndConnect() }
    }

    private fun updateFieldVisibility() {
        val isBrowser = radioBrowser.isChecked
        groupTelnetFields.visibility = if (isBrowser) View.GONE else View.VISIBLE
        groupBrowserFields.visibility = if (isBrowser) View.VISIBLE else View.GONE
    }

    private fun loadExistingHost(id: Int) {
        lifecycleScope.launch {
            val host = repository.getConnectionById(id)
            if (host != null) {
                existingHost = host
                inputName.setText(host.name)
                if (host.connectionType == "BROWSER") {
                    radioBrowser.isChecked = true
                    inputUrl.setText(host.url)
                } else {
                    radioTelnet.isChecked = true
                    inputHost.setText(host.host)
                    inputPort.setText(host.port.toString())
                }
                updateFieldVisibility()
                supportActionBar?.title = "Editar Host"
            }
        }
    }

    private fun buildConnection(): SavedConnection? {
        val name = inputName.text.toString().trim()
        if (name.isEmpty()) { showError("Informe o nome do host"); return null }

        return if (radioBrowser.isChecked) {
            var url = inputUrl.text.toString().trim()
            if (url.isEmpty()) { showError("Informe a URL"); return null }
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
            existingHost?.copy(name = name, connectionType = "BROWSER", url = url)
                ?: SavedConnection(name = name, host = "", connectionType = "BROWSER", url = url)
        } else {
            val host = inputHost.text.toString().trim()
            val portStr = inputPort.text.toString().trim()
            if (host.isEmpty()) { showError("Informe o IP ou hostname"); return null }
            if (portStr.isEmpty()) { showError("Informe a porta"); return null }
            val port = portStr.toIntOrNull() ?: run { showError("Porta invalida"); return null }
            existingHost?.copy(name = name, connectionType = "TELNET", host = host, port = port)
                ?: SavedConnection(name = name, connectionType = "TELNET", host = host, port = port)
        }
    }

    private fun saveHost(then: ((SavedConnection) -> Unit)? = null) {
        val connection = buildConnection() ?: return
        lifecycleScope.launch {
            if (existingHost != null) {
                repository.updateConnection(connection)
                Timber.d("Host atualizado: ${connection.name}")
            } else {
                val newId = repository.saveConnection(connection).toInt()
                existingHost = connection.copy(id = newId)
                Timber.d("Host salvo: ${connection.name} id=$newId")
            }
            if (then != null) {
                then.invoke(existingHost ?: connection)
            } else {
                Toast.makeText(this@HostConfigActivity, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveAndConnect() {
        saveHost { saved ->
            if (saved.connectionType == "BROWSER") {
                val result = SessionStore.openOrResumeBrowser(this, saved.id, saved.name, saved.url)
                if (result == null) {
                    Toast.makeText(this, "Máximo de 2 sessões ativas. Desconecte uma para abrir outra.", Toast.LENGTH_LONG).show()
                    return@saveHost
                }
                val (slotId, _) = result
                startActivity(Intent(this, BrowserActivity::class.java).apply {
                    putExtra(BrowserActivity.EXTRA_SLOT_ID, slotId)
                })
            } else {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_HOST, saved.host)
                    putExtra(MainActivity.EXTRA_PORT, saved.port)
                    putExtra(MainActivity.EXTRA_NAME, saved.name)
                    putExtra(MainActivity.EXTRA_HOST_ID, saved.id)
                }
                startActivity(intent)
            }
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

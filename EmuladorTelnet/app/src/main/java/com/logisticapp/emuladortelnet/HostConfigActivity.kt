package com.logisticapp.emuladortelnet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

    private lateinit var inputName: EditText
    private lateinit var inputHost: EditText
    private lateinit var inputPort: EditText
    private lateinit var btnSave: Button
    private lateinit var btnConnect: Button

    companion object {
        const val EXTRA_HOST_ID = "host_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_config)

        repository = TelnetRepository.getInstance(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        inputName = findViewById(R.id.input_name)
        inputHost = findViewById(R.id.input_host)
        inputPort = findViewById(R.id.input_port)
        btnSave = findViewById(R.id.btn_save)
        btnConnect = findViewById(R.id.btn_connect)

        inputPort.setText("23")

        val hostId = intent.getIntExtra(EXTRA_HOST_ID, -1)
        if (hostId > 0) {
            loadExistingHost(hostId)
        } else {
            supportActionBar?.title = "Novo Host"
        }

        btnSave.setOnClickListener { saveHost() }
        btnConnect.setOnClickListener { saveAndConnect() }
    }

    private fun loadExistingHost(id: Int) {
        lifecycleScope.launch {
            val host = repository.getConnectionById(id)
            if (host != null) {
                existingHost = host
                inputName.setText(host.name)
                inputHost.setText(host.host)
                inputPort.setText(host.port.toString())
                supportActionBar?.title = "Editar Host"
            }
        }
    }

    private fun buildConnection(): SavedConnection? {
        val name = inputName.text.toString().trim()
        val host = inputHost.text.toString().trim()
        val portStr = inputPort.text.toString().trim()

        if (name.isEmpty()) { showError("Informe o nome do host"); return null }
        if (host.isEmpty()) { showError("Informe o IP ou hostname"); return null }
        if (portStr.isEmpty()) { showError("Informe a porta"); return null }
        val port = portStr.toIntOrNull() ?: run { showError("Porta invalida"); return null }

        return SavedConnection(
            id = existingHost?.id ?: 0,
            name = name,
            host = host,
            port = port,
            createdAt = existingHost?.createdAt ?: System.currentTimeMillis()
        )
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
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_HOST, saved.host)
                putExtra(MainActivity.EXTRA_PORT, saved.port)
                putExtra(MainActivity.EXTRA_NAME, saved.name)
                putExtra(MainActivity.EXTRA_HOST_ID, saved.id)
            }
            startActivity(intent)
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

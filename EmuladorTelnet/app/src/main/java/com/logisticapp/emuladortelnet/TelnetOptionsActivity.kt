package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings
import com.logisticapp.emuladortelnet.settings.TelnetOptions

/**
 * Configuracoes > Comunicacao > Telnet Opcoes.
 * Tela com todos os parametros de conexao Telnet/SSL/SSH. Salva ao sair.
 */
class TelnetOptionsActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var opts: TelnetOptions

    // Conexao
    private lateinit var inServerAddress: EditText
    private lateinit var inTerminalType: EditText
    private lateinit var valLineTerminator: TextView
    private lateinit var ckUseIpBrk: CheckBox
    private lateinit var ckBinary: CheckBox
    private lateinit var ckParity: CheckBox
    private lateinit var valKeepAliveType: TextView
    private lateinit var inKeepAliveInterval: EditText
    // Login
    private lateinit var inWaitLogin: EditText
    private lateinit var inLoginWith: EditText
    private lateinit var inWaitPassword: EditText
    private lateinit var inPassword: EditText
    private lateinit var inWaitCommand: EditText
    private lateinit var inDoCommand: EditText
    // SSL
    private lateinit var ckSsl: CheckBox
    private lateinit var ckAuthCert: CheckBox
    private lateinit var inCertFile: EditText
    private lateinit var inCertPassword: EditText
    // SSH
    private lateinit var ckSsh: CheckBox
    private lateinit var inSshServer: EditText
    private lateinit var inSshUsername: EditText
    private lateinit var inSshPassword: EditText
    private lateinit var inSshKey: EditText
    private lateinit var inSshKeepAlive: EditText

    private val lineTerminatorOptions = arrayOf("CR+LF", "CR", "LF")
    private val keepAliveTypeOptions = arrayOf("TCP", "NVT", "Desligado")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_telnet_options)
        opts = settings.telnetOptions

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        bindViews()
        loadValues()
        setupSelectors()
    }

    private fun bindViews() {
        inServerAddress     = findViewById(R.id.in_server_address)
        inTerminalType      = findViewById(R.id.in_terminal_type)
        valLineTerminator   = findViewById(R.id.val_line_terminator)
        ckUseIpBrk          = findViewById(R.id.ck_use_ip_brk)
        ckBinary            = findViewById(R.id.ck_binary)
        ckParity            = findViewById(R.id.ck_parity)
        valKeepAliveType    = findViewById(R.id.val_keepalive_type)
        inKeepAliveInterval = findViewById(R.id.in_keepalive_interval)
        inWaitLogin         = findViewById(R.id.in_wait_login)
        inLoginWith         = findViewById(R.id.in_login_with)
        inWaitPassword      = findViewById(R.id.in_wait_password)
        inPassword          = findViewById(R.id.in_password)
        inWaitCommand       = findViewById(R.id.in_wait_command)
        inDoCommand         = findViewById(R.id.in_do_command)
        ckSsl               = findViewById(R.id.ck_ssl)
        ckAuthCert          = findViewById(R.id.ck_auth_cert)
        inCertFile          = findViewById(R.id.in_cert_file)
        inCertPassword      = findViewById(R.id.in_cert_password)
        ckSsh               = findViewById(R.id.ck_ssh)
        inSshServer         = findViewById(R.id.in_ssh_server)
        inSshUsername       = findViewById(R.id.in_ssh_username)
        inSshPassword       = findViewById(R.id.in_ssh_password)
        inSshKey            = findViewById(R.id.in_ssh_key)
        inSshKeepAlive      = findViewById(R.id.in_ssh_keepalive)
    }

    private fun loadValues() {
        inServerAddress.setText(opts.serverAddress)
        inTerminalType.setText(opts.terminalType)
        valLineTerminator.text = opts.lineTerminator
        ckUseIpBrk.isChecked = opts.useIpForBrk
        ckBinary.isChecked = opts.binaryMode
        ckParity.isChecked = opts.simulateParity
        valKeepAliveType.text = opts.keepAliveType
        inKeepAliveInterval.setText(opts.keepAliveInterval)
        inWaitLogin.setText(opts.waitLoginPrompt)
        inLoginWith.setText(opts.loginWith)
        inWaitPassword.setText(opts.waitPasswordPrompt)
        inPassword.setText(opts.password)
        inWaitCommand.setText(opts.waitCommandPrompt)
        inDoCommand.setText(opts.doCommand)
        ckSsl.isChecked = opts.useSsl
        ckAuthCert.isChecked = opts.authServerCert
        inCertFile.setText(opts.clientCertFile)
        inCertPassword.setText(opts.clientCertPassword)
        ckSsh.isChecked = opts.useSsh
        inSshServer.setText(opts.sshServer)
        inSshUsername.setText(opts.sshUsername)
        inSshPassword.setText(opts.sshPassword)
        inSshKey.setText(opts.sshPrivateKey)
        inSshKeepAlive.setText(opts.sshKeepAlive)
    }

    private fun setupSelectors() {
        // Checkboxes alternam clicando na linha
        findViewById<View>(R.id.row_use_ip_brk).setOnClickListener { ckUseIpBrk.toggle() }
        findViewById<View>(R.id.row_binary).setOnClickListener { ckBinary.toggle() }
        findViewById<View>(R.id.row_parity).setOnClickListener { ckParity.toggle() }
        findViewById<View>(R.id.row_ssl).setOnClickListener { ckSsl.toggle() }
        findViewById<View>(R.id.row_auth_cert).setOnClickListener { ckAuthCert.toggle() }
        findViewById<View>(R.id.row_ssh).setOnClickListener { ckSsh.toggle() }

        // Seletores
        findViewById<View>(R.id.row_line_terminator).setOnClickListener {
            pick("Terminador de linha", lineTerminatorOptions, valLineTerminator.text.toString()) {
                valLineTerminator.text = it
            }
        }
        findViewById<View>(R.id.row_keepalive_type).setOnClickListener {
            pick("Mantenha o tipo vivo", keepAliveTypeOptions, valKeepAliveType.text.toString()) {
                valKeepAliveType.text = it
            }
        }
    }

    private fun pick(title: String, options: Array<String>, current: String, onPick: (String) -> Unit) {
        val checked = options.indexOf(current).takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                onPick(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        settings.telnetOptions = TelnetOptions(
            serverAddress = inServerAddress.text.toString(),
            terminalType = inTerminalType.text.toString(),
            lineTerminator = valLineTerminator.text.toString(),
            useIpForBrk = ckUseIpBrk.isChecked,
            binaryMode = ckBinary.isChecked,
            simulateParity = ckParity.isChecked,
            keepAliveType = valKeepAliveType.text.toString(),
            keepAliveInterval = inKeepAliveInterval.text.toString(),
            waitLoginPrompt = inWaitLogin.text.toString(),
            loginWith = inLoginWith.text.toString(),
            waitPasswordPrompt = inWaitPassword.text.toString(),
            password = inPassword.text.toString(),
            waitCommandPrompt = inWaitCommand.text.toString(),
            doCommand = inDoCommand.text.toString(),
            useSsl = ckSsl.isChecked,
            authServerCert = ckAuthCert.isChecked,
            clientCertFile = inCertFile.text.toString(),
            clientCertPassword = inCertPassword.text.toString(),
            useSsh = ckSsh.isChecked,
            sshServer = inSshServer.text.toString(),
            sshUsername = inSshUsername.text.toString(),
            sshPassword = inSshPassword.text.toString(),
            sshPrivateKey = inSshKey.text.toString(),
            sshKeepAlive = inSshKeepAlive.text.toString()
        )
    }
}

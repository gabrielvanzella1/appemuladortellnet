package com.logisticapp.emuladortelnet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings

class GeneralOptionsActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings

    private lateinit var radioOrientation: RadioGroup
    private lateinit var switchAutoConnect: Switch
    private lateinit var switchAutoReconnect: Switch
    private lateinit var switchDisconnectLock: Switch
    private lateinit var switchKeepScreenOn: Switch
    private lateinit var switchKeyboard: Switch
    private lateinit var switchIgnoreBattery: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.get(this)
        settings.applyOrientation(this)
        setContentView(R.layout.activity_general_options)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        radioOrientation     = findViewById(R.id.radio_orientation)
        switchAutoConnect    = findViewById(R.id.switch_auto_connect)
        switchAutoReconnect  = findViewById(R.id.switch_auto_reconnect)
        switchDisconnectLock = findViewById(R.id.switch_disconnect_lock)
        switchKeepScreenOn   = findViewById(R.id.switch_keep_screen_on)
        switchKeyboard       = findViewById(R.id.switch_keyboard)
        switchIgnoreBattery  = findViewById(R.id.switch_ignore_battery)

        loadValues()
        setupListeners()
    }

    private fun loadValues() {
        radioOrientation.check(
            when (settings.orientation) {
                AppSettings.ORIENTATION_PORTRAIT  -> R.id.radio_portrait
                AppSettings.ORIENTATION_LANDSCAPE -> R.id.radio_landscape
                else                              -> R.id.radio_auto
            }
        )
        switchAutoConnect.isChecked    = settings.autoConnect
        switchAutoReconnect.isChecked  = settings.autoReconnect
        switchDisconnectLock.isChecked = settings.disconnectOnLock
        switchKeepScreenOn.isChecked   = settings.keepScreenOn
        switchKeyboard.isChecked       = settings.keyboardEnabled
        switchIgnoreBattery.isChecked  = isIgnoringBattery()
    }

    private fun setupListeners() {
        radioOrientation.setOnCheckedChangeListener { _, checkedId ->
            settings.orientation = when (checkedId) {
                R.id.radio_portrait  -> AppSettings.ORIENTATION_PORTRAIT
                R.id.radio_landscape -> AppSettings.ORIENTATION_LANDSCAPE
                else                 -> AppSettings.ORIENTATION_AUTO
            }
            settings.applyOrientation(this)
        }

        switchAutoConnect.setOnCheckedChangeListener { _, v -> settings.autoConnect = v }
        switchAutoReconnect.setOnCheckedChangeListener { _, v -> settings.autoReconnect = v }
        switchDisconnectLock.setOnCheckedChangeListener { _, v -> settings.disconnectOnLock = v }
        switchKeepScreenOn.setOnCheckedChangeListener { _, v -> settings.keepScreenOn = v }
        switchKeyboard.setOnCheckedChangeListener { _, v -> settings.keyboardEnabled = v }

        switchIgnoreBattery.setOnCheckedChangeListener { _, v ->
            settings.ignoreBattery = v
            if (v && !isIgnoringBattery()) {
                requestIgnoreBattery()
            }
        }
    }

    private fun isIgnoringBattery(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    @android.annotation.SuppressLint("BatteryLife")
    private fun requestIgnoreBattery() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Em alguns aparelhos o intent especifico nao existe; abrir a tela geral
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(this, "Nao foi possivel abrir as configuracoes de bateria",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refletir o estado real da otimizacao de bateria (usuario pode ter mudado no sistema)
        switchIgnoreBattery.setOnCheckedChangeListener(null)
        switchIgnoreBattery.isChecked = isIgnoringBattery()
        settings.ignoreBattery = switchIgnoreBattery.isChecked
        switchIgnoreBattery.setOnCheckedChangeListener { _, v ->
            settings.ignoreBattery = v
            if (v && !isIgnoringBattery()) requestIgnoreBattery()
        }
    }
}

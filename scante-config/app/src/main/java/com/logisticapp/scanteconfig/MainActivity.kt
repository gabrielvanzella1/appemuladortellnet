package com.logisticapp.scanteconfig

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.logisticapp.scanteconfig.config.ConfigRepository
import com.logisticapp.scanteconfig.config.ConfigSession

class MainActivity : AppCompatActivity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var repository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = ConfigRepository(this)
        listContainer = findViewById(R.id.portal_list_container)

        requestAllFilesAccessIfNeeded()

        findViewById<android.view.View>(R.id.btn_add_portal).setOnClickListener {
            if (ConfigSession.portals.size >= MAX_PORTAIS) {
                Toast.makeText(this, "Máximo de $MAX_PORTAIS portais (abas)", Toast.LENGTH_LONG).show()
            } else {
                startActivity(Intent(this, PortalEditActivity::class.java).putExtra(EXTRA_INDEX, -1))
            }
        }
        findViewById<android.view.View>(R.id.btn_save).setOnClickListener { saveConfig(notify = false) }
        findViewById<android.view.View>(R.id.btn_update).setOnClickListener { saveConfig(notify = true) }
    }

    override fun onResume() {
        super.onResume()
        if (!ConfigSession.dirty) {
            ConfigSession.loadFrom(repository.load())
        }
        renderList()
    }

    private fun renderList() {
        listContainer.removeAllViews()
        ConfigSession.portals.forEachIndexed { index, portal ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_portal, listContainer, false)
            row.findViewById<TextView>(R.id.text_portal_name).text = portal.name
            row.findViewById<TextView>(R.id.text_portal_homepage).text = portal.homepage
            val keyCount = ConfigSession.keyActions[portal.id]?.size ?: 0
            row.findViewById<TextView>(R.id.text_portal_keys).text = "$keyCount tecla(s) mapeada(s)"
            row.setOnClickListener {
                startActivity(Intent(this, PortalEditActivity::class.java).putExtra(EXTRA_INDEX, index))
            }
            row.findViewById<TextView>(R.id.btn_delete_portal).setOnClickListener {
                ConfigSession.removePortal(index)
                renderList()
            }
            listContainer.addView(row)
        }
        if (ConfigSession.portals.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Nenhum portal ainda. Toque em \"Adicionar portal\" para criar a primeira aba."
                setTextColor(getColor(R.color.text_secondary))
            }
            listContainer.addView(empty)
        }
    }

    private fun saveConfig(notify: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(this, "Conceda a permissão de acesso a arquivos primeiro.", Toast.LENGTH_LONG).show()
            requestAllFilesAccessIfNeeded()
            return
        }
        repository.save(ConfigSession.toConfig())
        if (notify) {
            repository.notifyBrowserToReload()
            Toast.makeText(this, "Configuração salva e enviada para o ScanTE Browser", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Configuração salva", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestAllFilesAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    companion object {
        const val EXTRA_INDEX = "portal_index"
        const val MAX_PORTAIS = 5
    }
}

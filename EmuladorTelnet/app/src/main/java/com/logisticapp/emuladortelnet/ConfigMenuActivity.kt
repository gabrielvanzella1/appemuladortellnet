package com.logisticapp.emuladortelnet

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.logisticapp.emuladortelnet.settings.AppSettings

/**
 * Menu de Configuracoes, agrupado em tres secoes:
 *   - Comunicacao: Telnet Opcoes, Servidor proxy
 *   - Emulacao:    VT Opcoes, Transliteracao, Geral
 *   - Tela:        Config. da barra de ferramentas, Opcoes de tela, Cores da tela
 *
 * Cada item leva a uma tela propria (a serem implementadas).
 */
class ConfigMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.get(this).applyOrientation(this)
        setContentView(R.layout.activity_config_menu)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Comunicacao
        findViewById<android.view.View>(R.id.config_telnet).setOnClickListener {
            startActivity(android.content.Intent(this, TelnetOptionsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.config_proxy).setOnClickListener {
            startActivity(android.content.Intent(this, ProxyActivity::class.java))
        }

        // Emulacao
        findViewById<android.view.View>(R.id.config_vt).setOnClickListener { emBreve("VT Opções") }
        findViewById<android.view.View>(R.id.config_transliteracao).setOnClickListener { emBreve("Transliteração") }
        findViewById<android.view.View>(R.id.config_geral).setOnClickListener { emBreve("Geral") }

        // Tela
        findViewById<android.view.View>(R.id.config_toolbar).setOnClickListener {
            startActivity(android.content.Intent(this, ToolbarConfigActivity::class.java))
        }
        findViewById<android.view.View>(R.id.config_screen_options).setOnClickListener {
            startActivity(android.content.Intent(this, ScreenOptionsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.config_screen_colors).setOnClickListener {
            startActivity(android.content.Intent(this, ScreenColorsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.config_vt_attr_map).setOnClickListener { emBreve("VT Mapeamento de atributos") }

        // Dispositivos
        findViewById<android.view.View>(R.id.config_print).setOnClickListener { emBreve("Configuração de impressão") }
        findViewById<android.view.View>(R.id.config_barcode).setOnClickListener { emBreve("Configuração do leitor de código de barras") }
    }

    private fun emBreve(nome: String) {
        Toast.makeText(this, "$nome: em breve", Toast.LENGTH_SHORT).show()
    }
}

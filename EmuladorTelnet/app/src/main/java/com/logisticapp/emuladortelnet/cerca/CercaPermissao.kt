package com.logisticapp.emuladortelnet.cerca

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat

/**
 * Explica a cerca digital ao operador e pede a permissão de localização — o Android só
 * mostra qual roteador Wi-Fi está conectado para apps com essa permissão (não usa GPS).
 * Pergunta no máximo uma vez por abertura do app; recusar não impede o uso do terminal.
 */
object CercaPermissao {

    private const val REQ = 4701
    private var perguntouNestaExecucao = false

    fun verificar(activity: Activity) {
        if (perguntouNestaExecucao || !CercaReporter.estaAtiva(activity) || activity.isFinishing) return
        perguntouNestaExecucao = true

        if (!CercaReporter.temPermissao(activity)) {
            AlertDialog.Builder(activity)
                .setTitle("Cerca digital ativada")
                .setMessage(
                    "A sua empresa ativou a cerca digital nos coletores.\n\n" +
                    "O ScanTE vai informar em qual roteador Wi-Fi do CD este coletor está " +
                    "conectado e o nível da bateria. Não usa GPS.\n\n" +
                    "Para isso, o Android pede a permissão de localização."
                )
                .setPositiveButton("Continuar") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQ
                    )
                }
                .setNegativeButton("Agora não", null)
                .show()
            return
        }

        // Com a Localização do Android desligada o sistema esconde o roteador (a cerca fica "sem localização")
        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            AlertDialog.Builder(activity)
                .setTitle("Ligue a Localização")
                .setMessage("A cerca digital precisa da Localização do Android ligada para saber em qual roteador Wi-Fi o coletor está. Não usa GPS.")
                .setPositiveButton("Abrir configurações") { _, _ ->
                    activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Agora não", null)
                .show()
        }
    }
}

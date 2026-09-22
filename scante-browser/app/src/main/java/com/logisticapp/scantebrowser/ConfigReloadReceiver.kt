package com.logisticapp.scantebrowser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Recebe o aviso de "Atualizar Configuração" do app ScanTE Config e reinicia
 * o processo — mais simples e seguro do que tentar trocar as WebViews de
 * todos os portais em memória sem derrubar nada que esteja carregando.
 */
class ConfigReloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("ConfigReloadReceiver: nova configuração recebida, reiniciando o ScanTE Browser")
        val restartIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }
}

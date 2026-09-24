package com.logisticapp.scantemonitor

import java.security.MessageDigest

/**
 * Senha local do coletor: trava o app depois de ativado, para o funcionário não conseguir
 * desativar o monitoramento nem mexer nas configurações. Separada da chave de licença.
 */
object Pin {
    private const val SAL = "scante-monitor-v1"
    const val PADRAO = "1234"

    fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((SAL + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** ponytail: self-check em debug — garante que o hash não quebrou numa alteração futura. */
    fun autoCheck() {
        check(hash("1234") == hash("1234")) { "Pin.hash não é determinístico" }
        check(hash("1234") != hash("4321")) { "Pin.hash colidiu para PINs diferentes" }
        check(hash(PADRAO).length == 64) { "Pin.hash não é SHA-256 (64 hex)" }
    }
}

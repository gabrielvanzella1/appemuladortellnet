package com.logisticapp.emuladortelnet

import android.content.Context
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory

/**
 * Mantém até MAX sessões Telnet simultâneas vivas independente do ciclo de vida das Activities.
 * O ViewModel é criado aqui (não via ViewModelProvider) para que sobreviva ao finish() da Activity.
 */
object SessionStore {

    const val MAX = 2

    data class ActiveSession(
        val slotId: Int,
        val hostId: Int,
        val hostName: String,
        val host: String,
        val port: Int,
        val viewModel: TelnetViewModel
    )

    private val slots = arrayOfNulls<ActiveSession>(MAX)

    /**
     * Abre ou retoma uma sessão.
     * Retorna (slotId, viewModel) se há vaga, ou null se as duas vagas estão ocupadas.
     */
    fun openOrResume(
        context: Context,
        hostId: Int,
        hostName: String,
        host: String,
        port: Int
    ): Pair<Int, TelnetViewModel>? {
        // Sessão já ativa para este host? Retoma o slot existente.
        val existingIdx = slots.indexOfFirst { it?.hostId == hostId }
        if (existingIdx >= 0) {
            val s = slots[existingIdx]!!
            return Pair(s.slotId, s.viewModel)
        }
        // Procura slot livre
        val free = slots.indexOfFirst { it == null }
        if (free < 0) return null   // todas as vagas ocupadas

        val repo = TelnetRepository.getInstance(context)
        val vm   = TelnetViewModelFactory(repo).create(TelnetViewModel::class.java)
        slots[free] = ActiveSession(free, hostId, hostName, host, port, vm)
        return Pair(free, vm)
    }

    /** Desconecta e libera o slot. */
    fun close(slotId: Int) {
        slots[slotId]?.viewModel?.disconnect()
        slots[slotId] = null
    }

    fun get(slotId: Int): ActiveSession? = slots.getOrNull(slotId)

    fun getAll(): List<ActiveSession> = slots.filterNotNull()

    fun isActive(hostId: Int): Boolean = slots.any { it?.hostId == hostId }

    fun activeCount(): Int = slots.count { it != null }

    /** Retorna a outra sessão ativa (se houver), útil para o botão ⇄. */
    fun otherSession(slotId: Int): ActiveSession? =
        slots.firstOrNull { it != null && it.slotId != slotId }
}

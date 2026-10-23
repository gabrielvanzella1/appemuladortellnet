package com.logisticapp.emuladortelnet

import android.content.Context
import com.logisticapp.emuladortelnet.data.ConnectionState
import com.logisticapp.emuladortelnet.database.TelnetRepository
import com.logisticapp.emuladortelnet.ui.TelnetViewModel
import com.logisticapp.emuladortelnet.ui.TelnetViewModelFactory

/** Mantém sessões Telnet independentes enquanto o usuário troca entre as Activities. */
object SessionStore {

    /** Limite prático por coletor para conexões simultâneas. */
    const val MAX = 8

    data class ActiveSession(
        val slotId: Int,
        val hostId: Int,
        val hostName: String,
        val host: String,
        val port: Int,
        val viewModel: TelnetViewModel
    )

    private val slots = arrayOfNulls<ActiveSession>(MAX)

    /** Somente sessões conectadas ou conectando ocupam uma vaga. */
    private fun isAlive(session: ActiveSession): Boolean =
        session.viewModel.connectionState.value.let {
            it == ConnectionState.CONNECTED || it == ConnectionState.CONNECTING
        }

    /** Abre uma sessão ou retoma a sessão ativa existente para este host. */
    fun openOrResume(
        context: Context,
        hostId: Int,
        hostName: String,
        host: String,
        port: Int
    ): Pair<Int, TelnetViewModel>? {
        val existingIdx = slots.indexOfFirst { it?.hostId == hostId }
        if (existingIdx >= 0) {
            val session = slots[existingIdx]!!
            if (isAlive(session)) return Pair(session.slotId, session.viewModel)
            close(session.slotId)
        }

        val free = slots.indices.firstOrNull { index ->
            val session = slots[index]
            session == null || !isAlive(session)
        } ?: return null
        if (slots[free] != null) close(free)

        val repo = TelnetRepository.getInstance(context)
        val viewModel = TelnetViewModelFactory(repo).create(TelnetViewModel::class.java)
        slots[free] = ActiveSession(free, hostId, hostName, host, port, viewModel)
        return Pair(free, viewModel)
    }

    /** Desconecta e libera uma vaga. */
    fun close(slotId: Int) {
        slots.getOrNull(slotId)?.viewModel?.disconnect()
        if (slotId in slots.indices) slots[slotId] = null
    }

    fun get(slotId: Int): ActiveSession? = slots.getOrNull(slotId)

    fun getAll(): List<ActiveSession> = slots.filterNotNull()

    fun activeSessions(): List<ActiveSession> = slots.filterNotNull().filter(::isAlive)

    fun isActive(hostId: Int): Boolean =
        slots.any { session -> session?.let { it.hostId == hostId && isAlive(it) } == true }

    fun activeCount(): Int = activeSessions().size

    /** Retorna a próxima sessão ativa em ordem circular, voltando à primeira no fim. */
    fun nextActiveSession(slotId: Int): ActiveSession? {
        val active = activeSessions()
        if (active.size < 2) return null
        val current = active.indexOfFirst { it.slotId == slotId }
        return active[if (current < 0) 0 else (current + 1) % active.size]
    }
}

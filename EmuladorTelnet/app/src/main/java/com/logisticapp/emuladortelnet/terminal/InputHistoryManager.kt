package com.logisticapp.emuladortelnet.terminal

/**
 * Gerenciador de histórico de comandos digitados
 * Suporta navegação com setas ↑↓
 */
class InputHistoryManager(private val maxSize: Int = 100) {

    private val history = mutableListOf<String>()
    private var currentIndex = -1

    /**
     * Adicionar comando ao histórico
     */
    fun add(command: String) {
        if (command.isNotBlank()) {
            // Remover duplicata anterior se existir
            history.remove(command)
            // Adicionar ao final
            history.add(command)
            // Limitar tamanho
            if (history.size > maxSize) {
                history.removeAt(0)
            }
            // Reset índice
            currentIndex = -1
        }
    }

    /**
     * Ir para comando anterior (seta ↑)
     */
    fun getPrevious(): String? {
        if (history.isEmpty()) return null

        // Se é primeira navegação
        if (currentIndex == -1) {
            currentIndex = history.size - 1
        } else if (currentIndex > 0) {
            currentIndex--
        }
        return history.getOrNull(currentIndex)
    }

    /**
     * Ir para próximo comando (seta ↓)
     */
    fun getNext(): String? {
        if (history.isEmpty()) return null

        return if (currentIndex < history.size - 1) {
            currentIndex++
            history.getOrNull(currentIndex)
        } else {
            currentIndex = -1
            null  // Retorna vazio para voltar ao input atual
        }
    }

    /**
     * Reset do índice (quando começa novo input)
     */
    fun reset() {
        currentIndex = -1
    }

    /**
     * Obter todo histórico
     */
    fun getAll(): List<String> {
        return history.toList()
    }

    /**
     * Limpar histórico
     */
    fun clear() {
        history.clear()
        currentIndex = -1
    }

    /**
     * Tamanho do histórico
     */
    fun size(): Int = history.size
}

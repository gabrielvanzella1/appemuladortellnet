package com.logisticapp.scanteconfig.config

import java.text.Normalizer

/**
 * Estado de edição em memória, compartilhado entre MainActivity e
 * PortalEditActivity — evita ter que serializar portal por portal a cada
 * navegação (app de uso único e sequencial, não precisa de mais que isso).
 */
object ConfigSession {
    var portals: MutableList<Portal> = mutableListOf()
    var keyActions: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    /**
     * true assim que o usuário mexe em algo (adicionar/editar/excluir portal).
     * Enquanto for false, MainActivity pode recarregar do disco à vontade a
     * cada onResume (cobre o caso comum de abrir o app antes de conceder a
     * permissão de armazenamento — sem isso a tela ficaria "vazia" para
     * sempre mesmo depois de conceder a permissão e voltar).
     */
    var dirty: Boolean = false
        private set

    fun loadFrom(config: BrowserConfig) {
        portals = config.portals.toMutableList()
        keyActions = config.keyActions.mapValues { it.value.toMutableMap() }.toMutableMap()
    }

    fun toConfig(): BrowserConfig = BrowserConfig(portals.toList(), keyActions)

    fun upsertPortal(index: Int, portal: Portal, keys: Map<String, String>) {
        dirty = true
        if (index in portals.indices) {
            val oldId = portals[index].id
            if (oldId != portal.id) keyActions.remove(oldId)
            portals[index] = portal
        } else {
            portals.add(portal)
        }
        keyActions[portal.id] = keys.toMutableMap()
    }

    fun removePortal(index: Int) {
        dirty = true
        if (index in portals.indices) {
            keyActions.remove(portals[index].id)
            portals.removeAt(index)
        }
    }

    fun uniqueIdFor(name: String, excludingIndex: Int): String {
        val base = slugify(name).ifBlank { "portal" }
        var candidate = base
        var n = 2
        while (portals.withIndex().any { (i, p) -> i != excludingIndex && p.id == candidate }) {
            candidate = "$base$n"
            n++
        }
        return candidate
    }

    private fun slugify(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return normalized.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}

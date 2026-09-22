package com.logisticapp.scantebrowser.config

import com.logisticapp.scantebrowser.portal.Portal

/**
 * Schema de configuração do browser corporativo. Pensado para depois mapear
 * 1:1 num bundle_array de Android Managed Configurations (cada Portal vira
 * um bundle na lista), sem precisar redesenhar o modelo.
 */
data class BrowserConfig(
    val portals: List<Portal> = emptyList(),
    /** portalId -> (keyCode como String -> ação, ex.: "131" -> "SUBMIT") */
    val keyActions: Map<String, Map<String, String>> = emptyMap()
)

package com.logisticapp.emuladortelnet.settings

import com.logisticapp.emuladortelnet.toolbar.ToolbarButton

data class KeyboardSettings(
    var showQwerty: Boolean = true,
    var showNumeric: Boolean = true,
    var showNavigation: Boolean = true,
    var showFunctionKeys: Boolean = true,
    var symbolsRow: String = "@#\$%&*()[]{}<>/|=_",
    var extraPages: List<List<ToolbarButton>> = emptyList()
)

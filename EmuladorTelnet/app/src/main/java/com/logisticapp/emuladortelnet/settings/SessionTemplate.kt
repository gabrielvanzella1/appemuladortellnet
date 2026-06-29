package com.logisticapp.emuladortelnet.settings

data class SessionTemplate(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val host: String,
    val port: Int = 23,
    val description: String = ""
)

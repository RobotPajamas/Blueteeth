package com.robotpajamas.blueteeth.models

typealias ConnectionHandler = ((Boolean) -> Unit)
//    val isConnected: Boolean

interface Connectable {
    fun connect(timeout: Int? = null, autoReconnect: Boolean = true, block: ConnectionHandler?)
    fun disconnect(autoReconnect: Boolean = false)
}

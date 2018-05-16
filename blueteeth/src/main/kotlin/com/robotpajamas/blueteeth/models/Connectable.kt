package com.robotpajamas.blueteeth.models

typealias ConnectionHandler = ((Boolean) -> Unit)

interface Connectable {
//    val isConnected: Boolean
    fun connect(timeout: Int? = null, autoReconnect: Boolean = true, block: ConnectionHandler?)
    fun disconnect(autoReconnect: Boolean = true)
}

package com.robotpajamas.blueteeth.models

typealias ServiceDiscovery = ((Result<Boolean>) -> Unit)

interface Discoverable {
    fun discoverServices(block: ServiceDiscovery?)
}
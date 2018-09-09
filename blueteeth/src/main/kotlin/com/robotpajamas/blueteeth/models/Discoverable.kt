package com.robotpajamas.blueteeth.models

import com.robotpajamas.dispatcher.Result

typealias ServiceDiscovery = ((Result<Boolean>) -> Unit)

interface Discoverable {
    fun discoverServices(block: ServiceDiscovery?)
}
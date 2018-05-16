package com.robotpajamas.blueteeth.extensions

import android.bluetooth.BluetoothGattCharacteristic

internal inline val BluetoothGattCharacteristic.compositeId: String
    get() = service.uuid.toString() + uuid.toString()
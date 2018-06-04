package com.robotpajamas.blueteeth.extensions

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

internal fun BluetoothGatt.getCharacteristic(characteristic: UUID, service: UUID): BluetoothGattCharacteristic? {
    getService(service)?.let {
        return it.getCharacteristic(characteristic)
    }
    return null
}
package com.robotpajamas.blueteeth.extensions

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager

inline val Context.isBluetoothSupported: Boolean
    get() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        return btAdapter != null
    }
package com.robotpajamas.android.blueteeth.ui.scan

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.listeners.OnScanCompletedListener
import com.robotpajamas.android.blueteeth.BR

class DeviceScanViewModel(private val navigator: Navigator) : BaseObservable() {

    private val DEVICE_SCAN_MILLISECONDS = 10000

    interface Navigator {
        fun navigateNext(macAddress: String)
    }

    @Bindable
    var devices: List<BlueteethDevice> = emptyList()
        private set(value) {
            field = value
            notifyPropertyChanged(BR.devices)
        }

    fun startScan() {
        Blueteeth.scanForPeripherals(DEVICE_SCAN_MILLISECONDS, OnScanCompletedListener { bleDevices ->
            devices = bleDevices.filter { it.bluetoothDevice?.name?.isNotBlank() ?: false }
        })
    }

    fun stopScan() {
        Blueteeth.stopScanForPeripherals()
    }
}
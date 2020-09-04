package com.robotpajamas.android.blueteeth.ui.scan

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.listeners.OnScanCompletedListener
import com.robotpajamas.android.blueteeth.BR

class DeviceScanViewModel(private val stateHandler: StateHandler, private val navigator: Navigator) : BaseObservable() {

    private val DEVICE_SCAN_MILLISECONDS = 2000

    interface StateHandler {
        fun scanning()
        fun notScanning()
    }

    interface Navigator {
        fun navigateNext(macAddress: String)
    }

    @Bindable
    var devices: List<ScannedDeviceViewModel> = emptyList()
        private set(value) {
            field = value
            notifyPropertyChanged(BR.devices)
        }

    fun startScan() {
        stateHandler.scanning()
        Blueteeth.scanForPeripherals(DEVICE_SCAN_MILLISECONDS, OnScanCompletedListener { bleDevices ->
            devices = bleDevices.filter { it.name.isNotBlank() }.distinctBy { it.id }.map { ScannedDeviceViewModel(it.name, it.id) }
            stateHandler.notScanning()
        })
    }

    fun stopScan() {
        Blueteeth.stopScanForPeripherals()
    }

    fun select(position: Int) {
        if (position < devices.size) {
            navigator.navigateNext(devices[position].mac)
        }
    }
}
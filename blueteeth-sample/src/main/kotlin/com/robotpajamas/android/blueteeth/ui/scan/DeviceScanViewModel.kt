package com.robotpajamas.android.blueteeth.ui.scan

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.listeners.OnScanCompletedListener
import com.robotpajamas.android.blueteeth.BR
import timber.log.Timber

class DeviceScanViewModel(private val stateHandler: StateHandler, private val navigator: Navigator) : BaseObservable() {

    private val DEVICE_SCAN_MILLISECONDS = 3000

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
        Blueteeth.scanForPeripherals(DEVICE_SCAN_MILLISECONDS) { bleDevices ->
            Timber.d("Returned ${bleDevices.size} not unique devices after scanning for $DEVICE_SCAN_MILLISECONDS ms")
            devices = bleDevices.filter { it.name.isNotBlank() }.distinctBy { it.id }.map { ScannedDeviceViewModel(it.name, it.id) }
            Timber.d("Filtered to ${devices.size} unique devices with a non-blank name")
            stateHandler.notScanning()
        }
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
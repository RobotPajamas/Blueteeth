package com.robotpajamas.android.blueteeth.ui.device

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.robotpajamas.android.blueteeth.BR
import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.models.Writable
import java.nio.charset.Charset
import java.util.*

class DeviceViewModel(private var macAddress: String,
                      private val navigator: Navigator,
                      private var device: BlueteethDevice = Blueteeth.getPeripheral(macAddress)) : BaseObservable() {

    interface Navigator {
        fun navigateBack()
    }

    @Bindable
    var connected = false
        private set(value) {
            field = value
            notifyPropertyChanged(BR.connected)
        }

    @Bindable
    var text = ""
        private set(value) {
            field = value
            notifyPropertyChanged(BR.text)
        }


    fun connect() {
        device.connect {
            connected = it
            text += "Connection Status: $connected \n"
            if (connected) {
                device.discoverServices {
                    text += "Service Discovery: ${it.value}\n"
                }
            }
        }
    }

    fun disconnect() {
        device.disconnect()
    }

    fun read() {
        device.read(CHARACTERISTIC_COUNTER, SERVICE_COUNTER) { result ->
            text += "Read result: ${result.value?.toString(Charset.defaultCharset())} \n"
        }
    }

    fun write() {
        device.write(byteArrayOf(1), CHARACTERISTIC_COUNTER, SERVICE_COUNTER, Writable.Type.WITH_RESPONSE) {
            text += "Write result: ${it.value} \n"
        }
    }

    fun subscribe() {
        device.subscribeTo(CHARACTERISTIC_COUNTER, SERVICE_COUNTER) {
            text += "Subscription read: ${it.value} \n"
        }
    }

    fun reset() {
        device.write(byteArrayOf(1), CHARACTERISTIC_COUNTER, SERVICE_COUNTER) {
            text += "Write result: ${it.value} \n"
        }
    }

    fun test() {
        clear()
        reset()
        for (i in 1..100) {
            read()
            write()
        }
    }

    fun clear() {
        text = ""
    }

    // TODO: Figure out what to do with these
    private val SERVICE_DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_MANUFACTURER_MODEL = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_SERIAL_NUMBER = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_FIRMWARE_VERSION = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_HARDWARE_VERSION = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_SOFTWARE_VERSION = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb")
    private val CHARACTERISTIC_MANUFACTURER_NAME = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")

    private val SERVICE_COUNTER = UUID.fromString("00726f62-6f74-7061-6a61-6d61732e6361");
    private val CHARACTERISTIC_COUNTER = UUID.fromString("01726f62-6f74-7061-6a61-6d61732e6361");
}

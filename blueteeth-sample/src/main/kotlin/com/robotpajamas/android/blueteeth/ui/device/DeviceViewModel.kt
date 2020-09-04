package com.robotpajamas.android.blueteeth.ui.device

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.robotpajamas.android.blueteeth.BR
import com.robotpajamas.android.blueteeth.extensions.prepend
import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.models.Writable
import java.nio.charset.Charset
import java.util.*

class DeviceViewModel(private var macAddress: String,
                      private val navigator: Navigator,
                      private var device: BlueteethDevice = Blueteeth.getPeripheral(macAddress)) : BaseObservable() {

    private val serviceUuid = UUID.fromString("00726f62-6f74-7061-6a61-6d61732e6361")
    private val txUuid = UUID.fromString("01726f62-6f74-7061-6a61-6d61732e6361")
    private val rxUuid = UUID.fromString("02726f62-6f74-7061-6a61-6d61732e6361")

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
            text = text.prepend("Connection Status: $connected\n")
            if (connected) {
                device.discoverServices {
                    text = text.prepend("Service Discovery: ${it.value}\n")
                }
            }
        }
    }

    fun disconnect() {
        device.disconnect()
    }

    fun read() {
        device.read(rxUuid, serviceUuid) { result ->
            text = text.prepend("Read result: ${result.value?.toString(Charset.defaultCharset())}\n")
            text = text.prepend("Read result: ${result.value?.contentToString()}\n")
        }
    }

    fun write() {
        device.write(byteArrayOf(1), txUuid, serviceUuid, Writable.Type.WITH_RESPONSE) {
            text = text.prepend("Write result: ${it.value}\n")
        }
    }

    fun subscribe() {
        device.subscribeTo(rxUuid, serviceUuid) {
            text = text.prepend("Subscription read: ${it.value?.contentToString()}\n\n")
        }
    }

    fun reset() {
//        device.write(byteArrayOf(1), CHARACTERISTIC_COUNTER, SERVICE_COUNTER) {
//            text += "Write result: ${it.value} \n"
//        }
    }

    fun test() {
        clear()
        reset()
        subscribe()
        for (i in 1..10) {
            read()
            write()
        }
    }

    fun clear() {
        text = ""
    }
}

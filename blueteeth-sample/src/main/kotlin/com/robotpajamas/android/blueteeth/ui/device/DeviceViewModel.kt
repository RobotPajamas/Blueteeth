package com.robotpajamas.android.blueteeth.ui.device

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.robotpajamas.android.blueteeth.BR
import com.robotpajamas.blueteeth.BlueteethDevice
import java.util.*

class DeviceViewModel(private val navigator: Navigator) : BaseObservable() {

    interface Navigator {
        fun navigateBack()
    }

    private var device: BlueteethDevice? = null

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
        device?.connect {
            connected = it
            text += "Connection Status: $connected \n"

        }
    }

    fun disconnect() {
        device?.disconnect()
    }

    fun read() {
        device?.read(UUID.fromString("2a24"), UUID.fromString("180a")) { result ->
            text += "Read result: ${result.value} \n"
        }
    }

    fun write() {
        device?.write(byteArrayOf(1, 2, 3), UUID.fromString("2a24"), UUID.fromString("180a")) {
            text += "Write result: ${it.value} \n"
        }
    }

    fun subscribe() {
        device?.subscribeTo(UUID.fromString("2a24"), UUID.fromString("180a")) {
            text += "Subscription read: ${it.value} \n"
        }
    }

    fun clear() {
        text = ""
    }
}
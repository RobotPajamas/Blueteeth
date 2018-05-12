package com.robotpajamas.android.blueteeth.ui.device

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.robotpajamas.android.blueteeth.BR
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.listeners.OnCharacteristicReadListener
import com.robotpajamas.blueteeth.listeners.OnConnectionChangedListener
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
        device?.connect(false, OnConnectionChangedListener { isConnected ->
            text += "Connection Status: $isConnected \n"
            connected = isConnected
        })
    }

    fun disconnect() {
        device?.disconnect(OnConnectionChangedListener { isConnected ->
            text += "Connection Status: $isConnected \n"
            connected = isConnected
        })
    }

    fun read() {
        device?.readCharacteristic(
                UUID.fromString("2a24"),
                UUID.fromString("180a"),
                OnCharacteristicReadListener { response, data ->
                    text += "Read response: $response, Read value: $data"
                })
    }

    fun write() {
        device?.writeCharacteristic(byteArrayOf(1, 2, 3), UUID.fromString("2a24"), UUID.fromString("180a"))
    }

    fun subscribe() {
        device?.subscribeTo(
                UUID.fromString("2a24"),
                UUID.fromString("180a"),
                OnCharacteristicReadListener { response, data ->
                    text += "Subscription read response: $response, Read value: $data"
                })
    }

    fun clear() {
        text = ""
    }
}
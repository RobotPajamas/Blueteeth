package com.robotpajamas.android.blueteeth.ui.device

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.robotpajamas.android.blueteeth.BR
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.listeners.OnConnectionChangedListener

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
    }

    fun write() {}

    fun subscribe() {}

    fun clear() {
        text = ""
    }
}
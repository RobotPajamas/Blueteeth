package com.robotpajamas.android.ble113_ota.Blueteeth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.robotpajamas.android.ble113_ota.Blueteeth.Callback.ConnectionCallback;
import com.robotpajamas.android.ble113_ota.Blueteeth.Callback.ServicesDiscoveredCallback;

/**
 * Created by sureshjoshi on 15-03-08.
 */
public class BlueteethDevice {
    public BluetoothDevice bluetoothDevice;
    BluetoothGatt gatt;

    BlueteethDevice(BluetoothDevice device) {
        bluetoothDevice = device;
    }

    public void connect(ConnectionCallback callback) {
        BlueteethManager.getInstance().connect(this, callback);
    }

    public void disconnect() {
        BlueteethManager.getInstance().disconnect(this);
    }

    public void discoverServices(ServicesDiscoveredCallback callback) {
        BlueteethManager.getInstance().discoverServices(this, callback);
    }
}

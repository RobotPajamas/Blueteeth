package com.robotpajamas.blueteeth;

import android.support.annotation.NonNull;

import com.robotpajamas.blueteeth.listeners.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicWriteListener;

import java.util.UUID;

// TODO: Handle error response from discovery
// TODO: Handle null callback as a WRITE_NO_RESPONSE?
public class BlueteethUtils {

    /**
     * Convenience method to write data to a peripheral. Automatically attempts to connect to an
     * unconnected peripheral first. Does NOT disconnect automatically.
     *
     * @param data           Array of bytes to send to the peripheral.
     * @param characteristic UUID of the characteristic to write to.
     * @param service        UUID of the service which contains the characteristic to write to.
     * @param device         A BlueteethDevice instance to write to.
     * @param writeListener  Optional callback after a successful write
     */
    public static void writeData(@NonNull byte[] data, @NonNull UUID characteristic, @NonNull UUID service, @NonNull BlueteethDevice device, OnCharacteristicWriteListener writeListener) {
        if (device.isConnected()) {
            device.discoverServices(response -> device.writeCharacteristic(data, characteristic, service, writeListener));
        } else {
            device.connect(false, isConnected -> {
                if (isConnected) {
                    device.discoverServices(response -> device.writeCharacteristic(data, characteristic, service, writeListener));
                } else {
                    writeListener.call(BlueteethResponse.NOT_CONNECTED);
                }
            });
        }
    }

    /**
     * Convenience method to read data from a peripheral. Automatically attempts to connect to an
     * unconnected peripheral first. Does NOT disconnect automatically.
     *
     * @param characteristic UUID of the characteristic to read from.
     * @param service        UUID of the service which contains the characteristic to read from.
     * @param device         A BlueteethDevice instance to read from.
     * @param readListener   Callback containing byte array from a successful read.
     */
    public static void read(@NonNull UUID characteristic, @NonNull UUID service, @NonNull BlueteethDevice device, @NonNull OnCharacteristicReadListener readListener) {
        if (device.isConnected()) {
            device.discoverServices(response -> device.readCharacteristic(characteristic, service, readListener));
        } else {
            device.connect(false, isConnected -> {
                if (isConnected) {
                    device.discoverServices(response -> device.readCharacteristic(characteristic, service, readListener));
                } else {
                    readListener.call(BlueteethResponse.NOT_CONNECTED, new byte[0]);
                }
            });
        }
    }
}

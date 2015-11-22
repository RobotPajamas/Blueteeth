package com.robotpajamas.android.ble113_ota.Blueteeth;

import com.robotpajamas.android.ble113_ota.Blueteeth.Callback.ReadCallback;
import com.robotpajamas.android.ble113_ota.Blueteeth.Callback.WriteCallback;

import java.util.UUID;

/**
 * Created by sureshjoshi on 15-03-08.
 */
public class BlueteethUtils {

    public static void writeData(byte[] data, UUID characteristic, UUID service, BlueteethDevice device, WriteCallback callback) {
        BlueteethManager.getInstance().writeCharacteristic(data, characteristic, service, device, callback);
    }

    public static void readData(UUID characteristic, UUID service, BlueteethDevice device, ReadCallback callback) {
        BlueteethManager.getInstance().readCharacteristic(characteristic, service, device, callback);
    }

}

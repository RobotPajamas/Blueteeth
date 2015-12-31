package com.robotpajamas.blueteeth;

import com.robotpajamas.blueteeth.Callback.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.Callback.OnServicesDiscoveredListener;

import java.util.UUID;

public class BlueteethUtils {

    //    public static void writeData(byte[] data, UUID characteristic, UUID service, BlueteethDevice device, OnCharacteristicWriteListener callback) {
//        BlueteethManager.getInstance().writeCharacteristic(data, characteristic, service, device, callback);
//    }
//
    public static void read(UUID characteristic, UUID service, BlueteethDevice device, OnCharacteristicReadListener callback) {
        if (callback != null) {
            device.discoverServices(() -> device.readCharacteristic(characteristic, service, callback));
        }
    }
}

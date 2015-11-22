package com.robotpajamas.blueteeth;


import com.robotpajamas.blueteeth.Callback.onCharacteristicRead;
import com.robotpajamas.blueteeth.Callback.onCharacteristicWrite;

import java.util.UUID;

/**
 * Created by sureshjoshi on 15-03-08.
 */
public class BlueteethUtils {

    public static void writeData(byte[] data, UUID characteristic, UUID service, BlueteethDevice device, onCharacteristicWrite callback) {
        BlueteethManager.getInstance().writeCharacteristic(data, characteristic, service, device, callback);
    }

    public static void readData(UUID characteristic, UUID service, BlueteethDevice device, onCharacteristicRead callback) {
        BlueteethManager.getInstance().readCharacteristic(characteristic, service, device, callback);
    }

}

package com.robotpajamas.blueteeth.listeners;

import com.robotpajamas.blueteeth.BlueteethResponse;

public interface OnCharacteristicChangedListener {
    void call(BlueteethResponse response, byte[] data);
    UUID getCharacteristicUUID();
    UUID getServiceUUID();
}

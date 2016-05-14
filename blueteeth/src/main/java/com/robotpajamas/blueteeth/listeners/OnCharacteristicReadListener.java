package com.robotpajamas.blueteeth.listeners;

import com.robotpajamas.blueteeth.BlueteethResponse;

public interface OnCharacteristicReadListener {
    void call(BlueteethResponse response, byte[] data);
}

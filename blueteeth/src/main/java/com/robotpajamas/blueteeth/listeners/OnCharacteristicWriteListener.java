package com.robotpajamas.blueteeth.listeners;

import com.robotpajamas.blueteeth.BlueteethResponse;

public interface OnCharacteristicWriteListener {
    void call(BlueteethResponse response);
}

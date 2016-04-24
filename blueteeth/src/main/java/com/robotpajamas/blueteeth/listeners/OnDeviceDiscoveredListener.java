package com.robotpajamas.blueteeth.listeners;

import com.robotpajamas.blueteeth.BlueteethDevice;

public interface OnDeviceDiscoveredListener {
    void call(BlueteethDevice blueteethDevice);
}

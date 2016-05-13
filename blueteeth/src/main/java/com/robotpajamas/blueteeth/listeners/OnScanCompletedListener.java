package com.robotpajamas.blueteeth.listeners;

import com.robotpajamas.blueteeth.BlueteethDevice;

import java.util.List;

public interface OnScanCompletedListener {
    void call(List<BlueteethDevice> blueteethDevices);
}

package com.robotpajamas.android.ble113_ota.Blueteeth.Callback;

import com.robotpajamas.android.ble113_ota.Blueteeth.BlueteethDevice;

import java.util.List;

public interface ScanCallback {
    public void call(List<BlueteethDevice> bleDevices);
}

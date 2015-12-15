package com.robotpajamas.blueteeth.Callback;

import com.robotpajamas.blueteeth.BlueteethDevice;

import java.util.List;

public interface OnScanCompletedListener {
    void onScanCompleted(List<BlueteethDevice> blueteethDevices);
}

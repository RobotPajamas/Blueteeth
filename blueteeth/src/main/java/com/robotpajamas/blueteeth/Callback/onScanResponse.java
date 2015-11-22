package com.robotpajamas.blueteeth.Callback;

import com.robotpajamas.blueteeth.BlueteethDevice;

import java.util.List;

public interface onScanResponse {
    void call(List<BlueteethDevice> bleDevices);
}

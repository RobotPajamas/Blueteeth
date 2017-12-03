package com.robotpajamas.android.blueteeth.peripherals;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicWriteListener;

import java.util.UUID;

public class SamplePeripheral extends BaseBluetoothPeripheral {

    // Custom Service
    private static final UUID SERVICE_TEST = UUID.fromString("00726f62-6f74-7061-6a61-6d61732e6361");

    private static final UUID CHARACTERISTIC_WRITE = UUID.fromString("01726f62-6f74-7061-6a61-6d61732e6361");
    private static final UUID CHARACTERISTIC_WRITE_NO_RESPONSE = UUID.fromString("02726f62-6f74-7061-6a61-6d61732e6361");

    private static final UUID CHARACTERISTIC_READ = UUID.fromString("03726f62-6f74-7061-6a61-6d61732e6361");
    private static final UUID CHARACTERISTIC_NOTIFY = UUID.fromString("04726f62-6f74-7061-6a61-6d61732e6361");
    private static final UUID CHARACTERISTIC_INDICATE = UUID.fromString("05726f62-6f74-7061-6a61-6d61732e6361");

    private static final UUID CHARACTERISTIC_WRITE_ECHO = UUID.fromString("06726f62-6f74-7061-6a61-6d61732e6361");
    private static final UUID CHARACTERISTIC_READ_ECHO = UUID.fromString("07726f62-6f74-7061-6a61-6d61732e6361");

    public SamplePeripheral(BlueteethDevice device) {
        super(device);
    }

    public void writeCounter(byte value, OnCharacteristicWriteListener writeListener) {
        byte[] data = new byte[]{value};
//        BlueteethUtils.writeData(data, CHARACTERISTIC_WRITE, SERVICE_TEST, mPeripheral, writeListener);
    }

    public void writeNoResponseCounter(byte value) {
        byte[] data = new byte[]{value};
//        BlueteethUtils.writeData(data, CHARACTERISTIC_WRITE_NO_RESPONSE, SERVICE_TEST, mPeripheral, null);
    }

    public void readCounter(OnCharacteristicReadListener readListener) {
//        BlueteethUtils.read(CHARACTERISTIC_READ, SERVICE_TEST, mPeripheral, readListener);
    }

    public void toggleNotification(boolean isEnabled, OnCharacteristicReadListener readListener) {
        if (isEnabled) {
//            mPeripheral.addNotification(CHARACTERISTIC_NOTIFY, SERVICE_TEST, readListener);
        } else {
//            mPeripheral.removeNotifications(CHARACTERISTIC_NOTIFY, SERVICE_TEST);
        }
    }

    public void writeEcho(byte[] dataToWrite, OnCharacteristicWriteListener writeListener) {
//        BlueteethUtils.writeData(dataToWrite, CHARACTERISTIC_WRITE_ECHO, SERVICE_TEST, mPeripheral, writeListener);
        mPeripheral.writeCharacteristic(dataToWrite, CHARACTERISTIC_WRITE_ECHO, SERVICE_TEST, writeListener);
    }

//    public void writeNoResponseEcho(byte[] dataToWrite) {
//        mPeripheral.writeCharacteristic(dataToWrite, CHARACTERISTIC_WRITE_ECHO, SERVICE_TEST, null);
//    }

    public void readEcho(OnCharacteristicReadListener readListener) {
//        BlueteethUtils.read(CHARACTERISTIC_READ_ECHO, SERVICE_TEST, mPeripheral, readListener);
    }

//    public void notifyEcho()
//    public void indicateEcho()

}


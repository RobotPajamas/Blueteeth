package com.robotpajamas.android.blueteeth.peripherals;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.listeners.OnBondingChangedListener;
import com.robotpajamas.blueteeth.listeners.OnConnectionChangedListener;

import java.util.UUID;

public class BaseBluetoothPeripheral {

    // Using standard 16bit UUIDs, transformed into the correct 128-bit UUID
    private static final UUID SERVICE_DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");

    private static final UUID CHARACTERISTIC_MANUFACTURER_MODEL = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_SERIAL_NUMBER = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_FIRMWARE_VERSION = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_HARDWARE_VERSION = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_SOFTWARE_VERSION = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_MANUFACTURER_NAME = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");

    protected BlueteethDevice mPeripheral;

    protected String mName;
    protected String mMacAddress;

    protected String mManufacturerModel;
    protected String mSerialNumber;
    protected String mFirmwareRevision;
    protected String mHardwareRevision;
    protected String mSoftwareRevision;
    protected String mManufacturerName;

    BaseBluetoothPeripheral(BlueteethDevice peripheral) {
        mPeripheral = peripheral;
        mName = peripheral.getName();
        mMacAddress = peripheral.getMacAddress();
    }

    public String getName() {
        return mName;
    }

    public String getMacAddress() {
        return mMacAddress;
    }

    /**
     * Determines if this peripheral is currently connected or not
     */
    public boolean isConnected() {
        return mPeripheral.isConnected();
    }


    /**
     * Opens connection with a timeout to this device
     *
     * @param autoReconnect      Determines whether the Bluetooth should auto-reconnect (very slow, in background - should be false)
     * @param connectionCallback Will be called after connection success/failure
     */
    public void connect(boolean autoReconnect, OnConnectionChangedListener connectionCallback) {
        mPeripheral.connect(autoReconnect, connectionCallback);
    }

    /**
     * Opens connection with a timeout to this device
     *
     * @param autoReconnect      Determines whether the Bluetooth should auto-reconnect (very slow, in background - should be false)
     * @param connectionCallback Will be called after connection success/failure
     * @param bondingCallback    Will be called on pairing events
     */
    public void connect(boolean autoReconnect, OnConnectionChangedListener connectionCallback, OnBondingChangedListener bondingCallback) {
        mPeripheral.connect(autoReconnect, connectionCallback, bondingCallback);
    }

    /**
     * Disconnects from device
     *
     * @param callback Will be called after disconnection success/failure
     */
    public void disconnect(OnConnectionChangedListener callback) {
        mPeripheral.disconnect(callback);
    }

    /**
     * Releases all Bluetooth resources
     */
    public void close() {
        mPeripheral.close();
    }
}


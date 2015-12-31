package com.robotpajamas.blueteeth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.robotpajamas.blueteeth.Callback.OnScanCompletedListener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class BlueteethManager {

    private BluetoothAdapter mBLEAdapter;
    private Handler mHandler = new Handler();
    static volatile BlueteethManager singleton = null;

    private boolean mIsScanning;

    public boolean isScanning() {
        return mIsScanning;
    }

    private List<BlueteethDevice> mScannedPeripherals = new ArrayList<>();

    /***
     * Returns a list of the stored peripherals
     *
     * @return List of all the scanned for devices
     */
    @NonNull
    public List<BlueteethDevice> getPeripherals() {
        return mScannedPeripherals;
    }

    /***
     * Returns a BlueteethDevice directly from a non-null macAddress
     *
     * @return List of all the scanned for devices
     */
    @NonNull
    public BlueteethDevice getPeripheral(@NonNull String macAddress) {
        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            throw new IllegalArgumentException("MacAddress is null or ill-formed");
        }
        return new BlueteethDevice(mBLEAdapter.getRemoteDevice(macAddress));
    }

    @Nullable
    private OnScanCompletedListener mScanCompletedCallback;

    public static BlueteethManager with(Context context) {
        if (singleton == null) {
            synchronized (BlueteethManager.class) {
                if (singleton == null) {
                    singleton = new Builder(context).build();
                }
            }
        }
        return singleton;
    }

    /**
     * Controls the level of logging.
     */
    public enum LogLevel {
        None,
        Debug;

        public boolean log() {
            return this != None;
        }
    }

    private LogLevel mLogLevel = LogLevel.None;

    BlueteethManager(Context applicationContext) {
        // Grab the application context in case an activity context was passed in
        Context context = applicationContext.getApplicationContext();

        Timber.d("Initializing BluetoothManager");
        BluetoothManager bleManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bleManager == null) {
            Timber.e("Unable to initialize BluetoothManager.");
            throw new RuntimeException();
        }

        Timber.d("Initializing BLEAdapter");
        mBLEAdapter = bleManager.getAdapter();
        if (mBLEAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter.");
            throw new RuntimeException();
        }

        if (!mBLEAdapter.isEnabled()) {
            Timber.e("Bluetooth is not enabled.");
            throw new RuntimeException();
        }
    }

    /**
     * Scans for nearby peripherals and fills the mScannedPeripherals ArrayList.
     * Scan will be stopped after input timeout.
     *
     * @param scanTimeoutMillis       timeout in milliseconds after which scan will be stoped
     * @param onScanCompletedCallback callback will be called after scanTimeoutMillis,
     *                                filled with nearby peripherals
     */
    public void scanForPeripherals(int scanTimeoutMillis, OnScanCompletedListener onScanCompletedCallback) {
        Timber.d("scanForPeripheralsWithTimeout");
        mScanCompletedCallback = onScanCompletedCallback;
        scanForPeripherals();
        mHandler.postDelayed(this::stopScanForPeripherals, scanTimeoutMillis);
    }

    /**
     * Scans for nearby peripherals (no timeout) and fills the mScannedPeripherals ArrayList.
     */
    public void scanForPeripherals() {
        Timber.d("scanForPeripherals");
        clearPeripherals();
        mIsScanning = true;
        mBLEAdapter.startLeScan(mBLEScanCallback);
    }

    private void clearPeripherals() {
        // TODO: Need to be a bit clever about how these are handled
        // TODO: If this is the last reference, close it, otherwise don't?
        for (BlueteethDevice blueteethDevice : mScannedPeripherals) {
            blueteethDevice.close();
        }

        mScannedPeripherals.clear();
    }

    /**
     * Stops ongoing scan process
     */
    public void stopScanForPeripherals() {
        Timber.d("stopScanForPeripherals");
        mIsScanning = false;
        mBLEAdapter.stopLeScan(mBLEScanCallback);

        if (mScanCompletedCallback != null) {
            mScanCompletedCallback.onScanCompleted(mScannedPeripherals);
            mScanCompletedCallback = null;
        }
    }

    private BluetoothAdapter.LeScanCallback mBLEScanCallback = (device, rssi, scanRecord) -> mScannedPeripherals.add(new BlueteethDevice(device));

    public static BlueteethManager getInstance() {
        return singleton;
    }

    static class Builder {
        private final Context mContext;

        /**
         * Start building a new {@link BlueteethManager} instance.
         */
        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            mContext = context.getApplicationContext();
        }

        /**
         * Create the {@link BlueteethManager} instance.
         */
        public BlueteethManager build() {
            Context context = mContext;
            return new BlueteethManager(context);
        }
    }
}

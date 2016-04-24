package com.robotpajamas.blueteeth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.robotpajamas.blueteeth.listeners.OnDeviceDiscoveredListener;
import com.robotpajamas.blueteeth.listeners.OnScanCompletedListener;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

// TODO: Fix support for pre-Lollipop vs post
public class BlueteethManager {

    private Context mContext;
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
        return new BlueteethDevice(mContext, mBLEAdapter.getRemoteDevice(macAddress));
    }

    @Nullable
    private OnScanCompletedListener mOnScanCompletedListener;
    @Nullable
    private OnDeviceDiscoveredListener mOnDeviceDiscoveredListener;


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
     * Set the global instance returned from {@link #with}.
     * <p>
     * This method must be called before any calls to {@link #with} and may only be called once.
     */
    public static void setSingletonInstance(BlueteethManager blueteethManager) {
        if (blueteethManager == null) {
            throw new IllegalArgumentException("Picasso must not be null.");
        }
        synchronized (BlueteethManager.class) {
            if (singleton != null) {
                throw new IllegalStateException("Singleton instance already exists.");
            }
            singleton = blueteethManager;
        }
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

    /***
     * Explicitly specified, package-protected default constructor
     */
    BlueteethManager() {
    }

    BlueteethManager(Context applicationContext) {
        // Grab the application context in case an activity context was passed in
        mContext = applicationContext.getApplicationContext();

        Timber.d("Initializing BluetoothManager");
        BluetoothManager bleManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
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
     * @param deviceDiscoveredListener callback will be called after each new device discovery
     */
    public void scanForPeripherals(OnDeviceDiscoveredListener deviceDiscoveredListener) {
        Timber.d("scanForPeripherals");
        mOnDeviceDiscoveredListener = deviceDiscoveredListener;
        scanForPeripherals();
    }

    /**
     * Scans for nearby peripherals and fills the mScannedPeripherals ArrayList.
     * Scan will be stopped after input timeout.
     *
     * @param scanTimeoutMillis        timeout in milliseconds after which scan will be stopped
     * @param deviceDiscoveredListener callback will be called after each new device discovery
     * @param scanCompletedListener    callback will be called after scanTimeoutMillis,
     *                                 filled with nearby peripherals
     */
    public void scanForPeripherals(int scanTimeoutMillis, OnDeviceDiscoveredListener deviceDiscoveredListener, OnScanCompletedListener scanCompletedListener) {
        Timber.d("scanForPeripherals");
        mOnDeviceDiscoveredListener = deviceDiscoveredListener;
        scanForPeripherals(scanTimeoutMillis, scanCompletedListener);
    }

    /**
     * Scans for nearby peripherals and fills the mScannedPeripherals ArrayList.
     * Scan will be stopped after input timeout.
     *
     * @param scanTimeoutMillis     timeout in milliseconds after which scan will be stoped
     * @param scanCompletedListener callback will be called after scanTimeoutMillis,
     *                              filled with nearby peripherals
     */
    public void scanForPeripherals(int scanTimeoutMillis, OnScanCompletedListener scanCompletedListener) {
        Timber.d("scanForPeripheralsWithTimeout");
        mOnScanCompletedListener = scanCompletedListener;
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

        if (mOnDeviceDiscoveredListener != null) {
            mOnDeviceDiscoveredListener = null;
        }

        if (mOnScanCompletedListener != null) {
            mOnScanCompletedListener.onScanCompleted(mScannedPeripherals);
            mOnScanCompletedListener = null;
        }
    }

    private BluetoothAdapter.LeScanCallback mBLEScanCallback = (device, rssi, scanRecord) -> {
        BlueteethDevice blueteethDevice = new BlueteethDevice(mContext, device, rssi, scanRecord);
        mScannedPeripherals.add(blueteethDevice);
        if (mOnDeviceDiscoveredListener != null) {
            mOnDeviceDiscoveredListener.call(blueteethDevice);
        }
    };

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

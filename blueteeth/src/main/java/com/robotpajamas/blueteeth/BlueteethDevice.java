package com.robotpajamas.blueteeth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.robotpajamas.blueteeth.listeners.OnBondingChangedListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicChangedListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicWriteListener;
import com.robotpajamas.blueteeth.listeners.OnConnectionChangedListener;
import com.robotpajamas.blueteeth.listeners.OnServicesDiscoveredListener;

import java.util.Arrays;
import java.util.UUID;

import timber.log.Timber;

// TODO: Make this object threadsafe and async-safe (called twice in a row, should return a failure?)
public class BlueteethDevice {
    private final BluetoothDevice mBluetoothDevice;
    private final Handler mHandler;
    private Context mContext;

    private int mRssi;
    private byte[] mScanRecord;

    @Nullable
    private BluetoothGatt mBluetoothGatt;
    @Nullable
    private OnConnectionChangedListener mConnectionChangedListener;
    @Nullable
    private OnServicesDiscoveredListener mServicesDiscoveredListener;
    @Nullable
    private OnCharacteristicReadListener mCharacteristicReadListener;
    @Nullable
    private OnCharacteristicWriteListener mCharacteristicWriteListener;
    @Nullable
    private OnCharacteristicChangedListener mCharacteristicChangedListener;
    private UUID notifyCharacteristicUUID;
    private UUID serviceNotifyUUID;

    @Nullable
    private OnBondingChangedListener mBondingChangedListener;

    private final String mName;

    public String getName() {
        return mName;
    }

    private final String mMacAddress;

    public String getMacAddress() {
        return mMacAddress;
    }

    public int getRssi() {
        return mRssi;
    }

    void setRssi(int rssi) {
        mRssi = rssi;
    }

    public byte[] getScanRecord() {
        return mScanRecord;
    }

    void setScanRecord(byte[] scanRecord) {
        mScanRecord = mScanRecord;
    }

    public enum BondState {
        Unknown,
        UnBonded,
        Bonding,
        Bonded;

        public static BondState fromInteger(int x) {
            switch (x) {
                case 10:
                    return UnBonded;
                case 11:
                    return Bonding;
                case 12:
                    return Bonded;
            }
            return Unknown;
        }
    }

    private BondState mBondState = BondState.Unknown;

    public BondState getBondState() {
        return mBondState;
    }

    private boolean mIsConnected;

    public boolean isConnected() {
        return mIsConnected;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    /***
     * Default constructor with invalid values - only here to help
     * with testability and Mock extensions
     */
    BlueteethDevice() {
        mName = "Invalid";
        mMacAddress = "00:00:00:00:00:00";
        mBluetoothDevice = null;
        // TODO: Does this need to be dependency injected for testing?
        mHandler = new Handler(Looper.getMainLooper());
    }


    BlueteethDevice(Context context, BluetoothDevice device) {
        mBluetoothDevice = device;
        mName = device.getName();
        mMacAddress = device.getAddress();
        // TODO: Need this for registering to the bonding process - ugly...
        mContext = context;
        // TODO: Does this need to be dependency injected for testing?
        mHandler = new Handler(Looper.getMainLooper());
    }

    BlueteethDevice(Context context, BluetoothDevice device, int rssi, byte[] scanRecord) {
        this(context, device);
        mRssi = rssi;
        mScanRecord = scanRecord;
    }

    // Autoreconnect == true is a slow connection, false is fast
    // https://stackoverflow.com/questions/22214254/android-ble-connect-slowly
    public boolean connect(boolean autoReconnect) {
        if (mIsConnected) {
            Timber.d("connect: Already connected, returning - disregarding autoReconnect");
            if (mConnectionChangedListener != null) {
                mConnectionChangedListener.call(mIsConnected);
            }
            return false;
        }

        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        mHandler.post(() -> mBluetoothGatt = mBluetoothDevice.connectGatt(null, autoReconnect, mGattCallback));
        return true;
    }

    public boolean connect(boolean autoReconnect, OnConnectionChangedListener onConnectionChangedListener, OnCharacteristicChangedListener mCharacteristicChangedListener, UUID serviceNotifyUUID, UUID notifyCharacteristicUUID) {
        this.mConnectionChangedListener = onConnectionChangedListener;
        this.mCharacteristicChangedListener = mCharacteristicChangedListener;
        this.notifyCharacteristicUUID = notifyCharacteristicUUID;
        this.serviceNotifyUUID = serviceNotifyUUID;
        return connect(autoReconnect);
    }

    public boolean connect(boolean autoReconnect, OnConnectionChangedListener onConnectionChangedListener) {
        mConnectionChangedListener = onConnectionChangedListener;
        return connect(autoReconnect);
    }

    /***
     * This connect call is only useful if the user is interested in Pairing/Bonding
     *
     * @param autoReconnect
     * @param onConnectionChangedListener
     * @param onBondingChangedListener
     * @return
     */
    public boolean connect(boolean autoReconnect, OnConnectionChangedListener onConnectionChangedListener, OnBondingChangedListener onBondingChangedListener) {
        mBondingChangedListener = onBondingChangedListener;
        return connect(autoReconnect, onConnectionChangedListener);
    }

    public boolean disconnect() {
        if (mBluetoothGatt == null) {
            Timber.e("disconnect: Cannot disconnect - GATT is null");
            return false;
        }

        mHandler.post(mBluetoothGatt::disconnect);
        return true;
    }

    public boolean disconnect(OnConnectionChangedListener onConnectionChangedListener) {
        if (mBluetoothGatt == null) {
            Timber.e("disconnect: Cannot disconnect - GATT is null");
            return false;
        }

        mConnectionChangedListener = onConnectionChangedListener;
        return disconnect();
    }

    public boolean discoverServices(OnServicesDiscoveredListener onServicesDiscoveredListener) {
        Timber.d("discoverServices: Attempting to discover services");
        if (!mIsConnected || mBluetoothGatt == null) {
            Timber.e("discoverServices: Device is not connected, or GATT is null");
            return false;
        }

        mServicesDiscoveredListener = onServicesDiscoveredListener;
        mHandler.post(mBluetoothGatt::discoverServices);
        return true;
    }

    public boolean readCharacteristic(@NonNull UUID characteristic, @NonNull UUID service, OnCharacteristicReadListener onCharacteristicReadListener) {
        Timber.d("readCharacteristic: Attempting to read %s", characteristic.toString());

        if (!mIsConnected || mBluetoothGatt == null) {
            Timber.e("readCharacteristic: Device is not connected, or GATT is null");
            return false;
        }

        mCharacteristicReadListener = onCharacteristicReadListener;
        BluetoothGattService gattService = mBluetoothGatt.getService(service);
        if (gattService == null) {
            Timber.e("readCharacteristic: Service not available - %s", service.toString());
            return false;
        }

        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
        if (gattCharacteristic == null) {
            Timber.e("readCharacteristic: Characteristic not available - %s", characteristic.toString());
            return false;
        }

        mHandler.post(() -> mBluetoothGatt.readCharacteristic(gattCharacteristic));
        return true;
    }

    public boolean writeCharacteristic(@NonNull byte[] data, @NonNull UUID characteristic, @NonNull UUID service, OnCharacteristicWriteListener onCharacteristicWriteListener) {
        Timber.d("writeCharacteristic: Attempting to write %s to %s", Arrays.toString(data), characteristic.toString());

        if (!mIsConnected || mBluetoothGatt == null) {
            Timber.e("writeCharacteristic: Device is not connected, or GATT is null");
            return false;
        }

        mCharacteristicWriteListener = onCharacteristicWriteListener;
        BluetoothGattService gattService = mBluetoothGatt.getService(service);
        if (gattService == null) {
            Timber.e("writeCharacteristic: Service not available - %s", service.toString());
            return false;
        }

        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
        if (gattCharacteristic == null) {
            Timber.e("writeCharacteristic: Characteristic not available - %s", characteristic.toString());
            return false;
        }

        gattCharacteristic.setValue(data);
        mHandler.post(() -> mBluetoothGatt.writeCharacteristic(gattCharacteristic));
        return true;
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    /***
     * Should never really get here, only if someone forgets to call close() explicitly
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }


    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Timber.d("onConnectionStateChange - gatt: %s, status: %s, newState: %s ", gatt.toString(), status, newState);

            // Removed check for GATT_SUCCESS - do we care? I think the current state is all that matters...

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
//                    mBondState = BondState.fromInteger(mBluetoothDevice.getBondState());
                    Timber.d("onConnectionStateChange - Connected - Bonding=" + mBondState);
                    mIsConnected = true;

                    // Register for Bonding notifications
                    mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

                    if (mConnectionChangedListener != null) {
                        mConnectionChangedListener.call(true);
//                            mConnectionChangedListener = null;
                    }

                    if (mBondingChangedListener != null && mBondState == BondState.Unknown) {
                        mBondingChangedListener.call(BondState.fromInteger(mBluetoothDevice.getBondState()) == BondState.Bonded);
                    }

                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Timber.d("onConnectionStateChange - Disconnected");
//                    mBondState = BondState.Unknown;
                    mIsConnected = false;

                    // Unregister for Bonding notifications
                    try {
                        mContext.unregisterReceiver(mBroadcastReceiver);
                    } catch (Exception e) {
                        Timber.e(e.toString());
                    }

                    if (mConnectionChangedListener != null) {
                        mConnectionChangedListener.call(false);
//                            mConnectionChangedListener = null;
                    }
//                    close();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Timber.d("onServicesDiscovered - gatt: %s, status: %s", gatt.toString(), status);

            if (mServicesDiscoveredListener != null) {
                BlueteethResponse response = BlueteethResponse.NO_ERROR;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("onServicesDiscovered - Success");
                } else {
                    Timber.e("onServicesDiscovered - Failed with status: " + status);
                    response = BlueteethResponse.ERROR;
                }
                mServicesDiscoveredListener.call(response);
                mServicesDiscoveredListener = null;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Timber.d("onCharacteristicRead - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString());

            if (mCharacteristicReadListener != null) {
                BlueteethResponse response = BlueteethResponse.NO_ERROR;
                byte[] readData = new byte[0];

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("onCharacteristicRead - Success");
                    readData = characteristic.getValue();
                } else {
                    Timber.e("onCharacteristicRead - Failed with status: " + status);
                    response = BlueteethResponse.ERROR;
                }

                mCharacteristicReadListener.call(response, readData);
                mCharacteristicReadListener = null;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Timber.d("onCharacteristicWrite - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString());

            if (mCharacteristicWriteListener != null) {
                BlueteethResponse response = BlueteethResponse.NO_ERROR;

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("onCharacteristicWrite - Success");
                } else {
                    Timber.e("onCharacteristicWrite - Failed with status: " + status);
                    response = BlueteethResponse.ERROR;
                }

                mCharacteristicWriteListener.call(response);
                mCharacteristicWriteListener = null;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Timber.d("OnCharacteristicChanged - gatt: %s, characteristic: %s ", gatt.toString(), characteristic.toString());
            if (mCharacteristicChangedListener != null) {
                BlueteethResponse response = BlueteethResponse.NO_ERROR;
                mCharacteristicChangedListener.call(response, characteristic.getValue());
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                switch (state) {
                    case BluetoothDevice.BOND_BONDING:
                        Timber.d("onReceive - BONDING");
                        mBondState = BondState.Bonding;
                        break;

                    case BluetoothDevice.BOND_BONDED:
                        Timber.d("onReceive - BONDED");
                        mBondState = BondState.Bonded;
                        if (mBondingChangedListener != null) {
                            mBondingChangedListener.call(true);
                        }
                        break;

                    case BluetoothDevice.BOND_NONE:
                        Timber.d("onReceive - NONE");
                        mBondState = BondState.Unknown;
                        break;
                }
            }
        }
    };

    @Override
    public String toString() {
        return "BluetoothDevice - Name: " + getName() + ", macAddress: " + mBluetoothDevice.toString() + "\n"
                + "RSSI: " + getRssi() + "\n"
                + "ScanRecord: " + Arrays.toString(mScanRecord) + "\n";
    }

    // TODO: Override these correctly
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public boolean setCharacteristicNotification(BluetoothDevice device, UUID serviceUuid, UUID characteristicUuid,
                                                 boolean enable) {
        Timber.d("setCharacteristicNotification(device=" + device.getName() + device.getAddress() + ", UUID="
                + characteristicUuid + ", enable=" + enable + " )");
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(serviceUuid).getCharacteristic(characteristicUuid);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        return mBluetoothGatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
    }
    public void registerNotifyOnCharacteristicChanged(){
        if (mCharacteristicChangedListener != null) {
            boolean successRegisterNotify = setCharacteristicNotification(mBluetoothDevice, serviceNotifyUUID, notifyCharacteristicUUID, true);
            Timber.e("onSetCharacteristicNotification - success: " + successRegisterNotify);
        }
    }
}

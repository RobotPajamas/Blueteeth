package com.robotpajamas.blueteeth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.robotpajamas.blueteeth.Callback.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.Callback.OnCharacteristicWriteListener;
import com.robotpajamas.blueteeth.Callback.OnConnectionChangedListener;
import com.robotpajamas.blueteeth.Callback.OnServicesDiscoveredListener;

import java.util.UUID;

import timber.log.Timber;

public class BlueteethDevice {
    private final BluetoothDevice mBluetoothDevice;

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

    private final String mName;

    public String getName() {
        return mName;
    }

    private final String mMacAddress;

    public String getMacAddress() {
        return mMacAddress;
    }

    private Context mContext;

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

    private BondState mBondState;

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
    }


    BlueteethDevice(BluetoothDevice device) {
        mBluetoothDevice = device;
        mName = device.getName();
        mMacAddress = device.getAddress();
    }

    // Autoreconnect == true is a slow connection, false is fast
    // https://stackoverflow.com/questions/22214254/android-ble-connect-slowly
    public boolean connect(boolean autoReconnect) {
        if (mIsConnected) {
            Timber.d("connect: Already connected, returning - disregarding autoReconnect");
            if (mConnectionChangedListener != null) {
                mConnectionChangedListener.onConnectionChanged(mIsConnected);
            }
            return false;
        }

        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        mBluetoothGatt = mBluetoothDevice.connectGatt(null, autoReconnect, mGattCallback);
        return true;
    }

    public boolean connect(boolean autoReconnect, OnConnectionChangedListener onConnectionChangedListener) {
        mConnectionChangedListener = onConnectionChangedListener;
        return connect(autoReconnect);
    }

    public boolean disconnect() {
        if (mBluetoothGatt == null) {
            Timber.e("disconnect: Cannot disconnect - GATT is null");
            return false;
        }
        mBluetoothGatt.disconnect();
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
        if (!mIsConnected || mBluetoothGatt == null) {
            Timber.e("discoverServices: Device is not connected, or GATT is null");
            return false;
        }

        mServicesDiscoveredListener = onServicesDiscoveredListener;
        mBluetoothGatt.discoverServices();
        return true;
    }

    public boolean readCharacteristic(@NonNull UUID characteristic, @NonNull UUID service, OnCharacteristicReadListener onCharacteristicReadListener) {
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

        return mBluetoothGatt.readCharacteristic(gattCharacteristic);
    }

    public boolean writeCharacteristic(@NonNull byte[] data, @NonNull UUID characteristic, @NonNull UUID service, OnCharacteristicWriteListener onCharacteristicWriteListener) {
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
        return mBluetoothGatt.writeCharacteristic(gattCharacteristic);
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
                    mBondState = BondState.fromInteger(mBluetoothDevice.getBondState());
                    Timber.d("onConnectionStateChange - Connected - Bonding=" + mBondState);
                    mIsConnected = true;
                    if (mConnectionChangedListener != null) {
                        Timber.e("STATE_CONNECTED - Callback Fired");
                        mConnectionChangedListener.onConnectionChanged(true);
//                            mConnectionChangedListener = null;
                    }

                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Timber.d("onConnectionStateChange - Disconnected");
                    mBondState = BondState.Unknown;
                    mIsConnected = false;
                    if (mConnectionChangedListener != null) {
                        Timber.e("STATE_DISCONNECTED - Callback Fired");
                        mConnectionChangedListener.onConnectionChanged(false);
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

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mServicesDiscoveredListener != null) {
                    Timber.e("onServicesDiscovered - Fire Callback");
                    mServicesDiscoveredListener.onServicesDiscovered();
                    mServicesDiscoveredListener = null;
                }
            } else {
                Timber.e("onCharacteristicRead - Failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Timber.d("OnCharacteristicReadListener - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mCharacteristicReadListener != null) {
                    Timber.e("onCharacteristicRead - Fire Callback");
                    mCharacteristicReadListener.onCharacteristicRead(characteristic.getValue());
                    mCharacteristicReadListener = null;
                }
            } else {
                Timber.e("onCharacteristicRead - Failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Timber.d("OnCharacteristicWriteListener - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mCharacteristicWriteListener != null) {
                    Timber.e("OnCharacteristicWriteListener - Fire Callback");
                    mCharacteristicWriteListener.onCharacteristicWritten();
                    mCharacteristicWriteListener = null;
                }
            } else {
                Timber.e("onCharacteristicWrite - Failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Timber.d("OnCharacteristicChanged - gatt: %s, characteristic: %s ", gatt.toString(), characteristic.toString());
        }
    };
}

package com.robotpajamas.blueteeth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.robotpajamas.blueteeth.Callback.OnCharacteristicReadListener;
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

    private final String mMacAddress;
    private boolean mIsConnected;

    public String getMacAddress() {
        return mMacAddress;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    BlueteethDevice(BluetoothDevice device) {
        mBluetoothDevice = device;
        mMacAddress = device.getAddress();
    }

    public void connect(OnConnectionChangedListener onConnectionChangedListener) {
//        if (mBluetoothGatt != null) {
//            Timber.e("Bluetooth GATT is null");
//            return;
//        }

        mConnectionChangedListener = onConnectionChangedListener;

        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        mBluetoothGatt = mBluetoothDevice.connectGatt(null, false, mGattCallback);
    }

    public void disconnect(OnConnectionChangedListener onConnectionChangedListener) {
        if (mBluetoothGatt == null) {
            return;
        }
        mConnectionChangedListener = onConnectionChangedListener;
        mBluetoothGatt.disconnect();
    }

    public boolean discoverServices(OnServicesDiscoveredListener onServicesDiscoveredListener) {
        if (!mIsConnected || mBluetoothGatt == null) {
            return false;
        }
        mServicesDiscoveredListener = onServicesDiscoveredListener;
        mBluetoothGatt.discoverServices();
        return true;
    }

    public boolean readCharacteristic(@NonNull UUID characteristic, @NonNull UUID service, OnCharacteristicReadListener onCharacteristicReadListener) {
        if (!mIsConnected || mBluetoothGatt == null) {
            return false;
        }
        mCharacteristicReadListener = onCharacteristicReadListener;
        BluetoothGattService gattService = mBluetoothGatt.getService(service);
        if (gattService == null) {
            Timber.e("Service not available - %s", service.toString());
            return false;
        }

        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
        if (gattCharacteristic == null) {
            Timber.e("Characteristic not available - %s", characteristic.toString());
            return false;
        }

        return mBluetoothGatt.readCharacteristic(gattCharacteristic);
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

            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Timber.d("onConnectionStateChange - Connected");
                        mIsConnected = true;
                        if (mConnectionChangedListener != null) {
                            mConnectionChangedListener.onConnectionChanged(true);
                            mConnectionChangedListener = null;
                        }
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:
                        Timber.d("onConnectionStateChange - Disconnected");
                        mIsConnected = false;
                        if (mConnectionChangedListener != null) {
                            mConnectionChangedListener.onConnectionChanged(false);
                            mConnectionChangedListener = null;
                        }
                        close();
                        break;
                }
            } else {
                //TODO: Handle this case
                close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Timber.d("onServicesDiscovered - gatt: %s, status: %s", gatt.toString(), status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mServicesDiscoveredListener != null) {
                    mServicesDiscoveredListener.onServicesDiscovered();
                    mServicesDiscoveredListener = null;
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Timber.d("OnCharacteristicReadListener - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString());
            if (mCharacteristicReadListener != null) {
                mCharacteristicReadListener.onCharacteristicRead(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Timber.d("OnCharacteristicWriteListener - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString());
//            mWriteCallbackQueue.remove().onServicesDiscovered();
        }
    };


}

package com.robotpajamas.blueteeth

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import com.robotpajamas.blueteeth.listeners.*
import timber.log.Timber
import java.util.*

// TODO: Make this object threadsafe and async-safe (called twice in a row, should return a failure?)
class BlueteethDevice private constructor() {

    // TODO: The handler posts would be better if abstracted away - Does this need to be dependency injected for testing?
    private val mHandler = Handler(Looper.getMainLooper())
    private var mContext: Context? = null

    var bluetoothDevice: BluetoothDevice? = null

    private var mScanRecord: ByteArray = ByteArray(0)
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionChangedListener: OnConnectionChangedListener? = null
    private var mServicesDiscoveredListener: OnServicesDiscoveredListener? = null
    private var mCharacteristicReadListener: OnCharacteristicReadListener? = null
    private var mCharacteristicWriteListener: OnCharacteristicWriteListener? = null
    private var mBondingChangedListener: OnBondingChangedListener? = null

    private val mNotificationMap = HashMap<String, OnCharacteristicReadListener>()

    var rssi: Int = 0
        internal set

    var name: String = ""

    var macAddress: String = "00:00:00:00:00:00"

    var scanRecord: ByteArray
        get() = mScanRecord
        internal set(scanRecord) {
            mScanRecord = mScanRecord
        }

    // TODO: This is done pretty poorly - fix this up...
    enum class BondState {
        UNKNOWN,
        UNBONDED,
        BONDING,
        BONDED;

        companion object {

            fun fromInteger(x: Int?): BondState {
                when (x) {
                    10 -> return UNBONDED
                    11 -> return BONDING
                    12 -> return BONDED
                }
                return UNKNOWN
            }
        }
    }

    var bondState = BondState.UNKNOWN
        private set

    var isConnected: Boolean = false
        private set

    /***
     * Default constructor with invalid values - only here to help
     * with testability and Mock extensions
     */
    //    internal constructor() {
//        name = "Invalid"
//        macAddress = "00:00:00:00:00:00"
//        bluetoothDevice = null
//         TODO: Does this need to be dependency injected for testing?
//        mHandler = Handler(Looper.getMainLooper())
//    }

    internal constructor(context: Context, device: BluetoothDevice) : this() {
        bluetoothDevice = device
        name = device.name ?: ""
        macAddress = device.address ?: ""
        // TODO: Need this for registering to the bonding process - ugly...
        mContext = context
    }

    internal constructor(context: Context,
                         device: BluetoothDevice,
                         rssi: Int,
                         scanRecord: ByteArray) : this(context, device) {
        this.rssi = rssi
        mScanRecord = scanRecord
    }

    // Autoreconnect == true is a slow connection, false is fast
    // https://stackoverflow.com/questions/22214254/android-ble-connect-slowly
    fun connect(autoReconnect: Boolean): Boolean {
        if (isConnected) {
            Timber.d("connect: Already connected, returning - disregarding autoReconnect")
            mConnectionChangedListener?.call(isConnected)
            return false
        }

        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        mHandler.post { mBluetoothGatt = bluetoothDevice?.connectGatt(null, autoReconnect, mGattCallback) }
        return true
    }

    fun connect(autoReconnect: Boolean, onConnectionChangedListener: OnConnectionChangedListener): Boolean {
        mConnectionChangedListener = onConnectionChangedListener
        return connect(autoReconnect)
    }

    /***
     * This connect call is only useful if the user is interested in Pairing/Bonding

     * @param autoReconnect
     * @param onConnectionChangedListener
     * @param onBondingChangedListener
     * @return
     */
    fun connect(autoReconnect: Boolean, onConnectionChangedListener: OnConnectionChangedListener, onBondingChangedListener: OnBondingChangedListener): Boolean {
        mBondingChangedListener = onBondingChangedListener
        return connect(autoReconnect, onConnectionChangedListener)
    }

    fun disconnect(): Boolean {
        if (mBluetoothGatt == null) {
            Timber.e("disconnect: Cannot disconnect - GATT is null")
            return false
        }

        mHandler.post { mBluetoothGatt?.disconnect() }
        return true
    }

    fun disconnect(onConnectionChangedListener: OnConnectionChangedListener): Boolean {
        if (mBluetoothGatt == null) {
            Timber.e("disconnect: Cannot disconnect - GATT is null")
            return false
        }
        mConnectionChangedListener = onConnectionChangedListener
        return disconnect()
    }

    fun discoverServices(onServicesDiscoveredListener: OnServicesDiscoveredListener): Boolean {
        Timber.d("discoverServices: Attempting to discover services")
        if (!isConnected || mBluetoothGatt == null) {
            Timber.e("discoverServices: Device is not connected, or GATT is null")
            return false
        }

        mServicesDiscoveredListener = onServicesDiscoveredListener
        mHandler.post { mBluetoothGatt?.discoverServices() }
        return true
    }

    fun readCharacteristic(characteristic: UUID, service: UUID, characteristicReadListener: OnCharacteristicReadListener): Boolean {
        Timber.d("readCharacteristic: Attempting to read %s", characteristic.toString())

        if (!isConnected || mBluetoothGatt == null) {
            Timber.e("readCharacteristic: Device is not connected, or GATT is null")
            return false
        }

        mCharacteristicReadListener = characteristicReadListener
        val gattService = mBluetoothGatt?.getService(service)
        if (gattService == null) {
            Timber.e("readCharacteristic: Service not available - %s", service.toString())
            return false
        }

        val gattCharacteristic = gattService.getCharacteristic(characteristic)
        if (gattCharacteristic == null) {
            Timber.e("readCharacteristic: Characteristic not available - %s", characteristic.toString())
            return false
        }

        mHandler.post { mBluetoothGatt?.readCharacteristic(gattCharacteristic) }
        return true
    }

    fun writeCharacteristic(data: ByteArray, characteristic: UUID, service: UUID, characteristicWriteListener: OnCharacteristicWriteListener?): Boolean {
        Timber.d("writeCharacteristic: Attempting to write %s to %s", Arrays.toString(data), characteristic.toString())

        if (!isConnected || mBluetoothGatt == null) {
            Timber.e("writeCharacteristic: Device is not connected, or GATT is null")
            return false
        }

        mCharacteristicWriteListener = characteristicWriteListener
        val gattService = mBluetoothGatt?.getService(service)
        if (gattService == null) {
            Timber.e("writeCharacteristic: Service not available - %s", service.toString())
            return false
        }

        val gattCharacteristic = gattService.getCharacteristic(characteristic)
        if (gattCharacteristic == null) {
            Timber.e("writeCharacteristic: Characteristic not available - %s", characteristic.toString())
            return false
        }

        gattCharacteristic.value = data
        if (mCharacteristicWriteListener == null) {
            gattCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        mHandler.post { mBluetoothGatt?.writeCharacteristic(gattCharacteristic) }
        return true
    }

    fun subscribeTo(characteristic: UUID, service: UUID, characteristicReadListener: OnCharacteristicReadListener): Boolean {
        Timber.d("subscribeTo: Adding Notification listener to %s", characteristic.toString())

        guard(mBluetoothGatt != null && isConnected) {
            Timber.e("subscribeTo: GATT is null or not connected")
            return false
        }

        val gattService = mBluetoothGatt?.getService(service)
        guard(gattService != null) {
            Timber.e("subscribeTo: Service not available - %s", service.toString())
            return false
        }

        val gattCharacteristic = gattService?.getCharacteristic(characteristic)
        guard(gattCharacteristic != null) {
            Timber.e("subscribeTo: Characteristic not available - %s", characteristic.toString())
            return false
        }

        val gattDescriptor = gattCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        guard(gattDescriptor != null) {
            Timber.e("subscribeTo: Descriptor not available - %s", characteristic.toString())
            return false
        }

        mNotificationMap.put(characteristic.toString(), characteristicReadListener)
        mBluetoothGatt?.setCharacteristicNotification(gattCharacteristic, true)
        gattDescriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt?.writeDescriptor(gattDescriptor)
        return true
    }

    fun indicateFrom(characteristic: UUID, service: UUID, characteristicReadListener: OnCharacteristicReadListener): Boolean {
        Timber.d("indicateFrom: Adding Notification listener to %s", characteristic.toString())
        guard(mBluetoothGatt != null && isConnected) {
            Timber.e("indicateFrom: GATT is null")
            return false
        }

        val gattService = mBluetoothGatt?.getService(service)
        guard(gattService != null) {
            Timber.e("indicateFrom: Service not available - %s", service.toString())
            return false
        }

        val gattCharacteristic = gattService?.getCharacteristic(characteristic)
        guard(gattCharacteristic != null) {
            Timber.e("indicateFrom: Characteristic not available - %s", characteristic.toString())
            return false
        }

        mBluetoothGatt?.setCharacteristicNotification(gattCharacteristic, true)
        val gattDescriptor = gattCharacteristic?.getDescriptor(characteristic)
        gattDescriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        mBluetoothGatt?.writeDescriptor(gattDescriptor)
        return true
    }

    fun close() {
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    /***
     * Should never really get here, only if someone forgets to call close() explicitly

     * @throws Throwable
     */
    @Throws(Throwable::class)
    private fun finalize() {
        try {
            close()
        } finally {
            Timber.e("Could not close the BlueteethDevice")
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Timber.d("onConnectionStateChange - gatt: %s, status: %s, newState: %s ", gatt.toString(), status, newState)

            // Removed check for GATT_SUCCESS - do we care? I think the current state is all that matters...
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // mBondState = BondState.fromInteger(mBluetoothDevice.getBondState());
                    Timber.d("onConnectionStateChange - Connected - Bonding=" + bondState)
                    isConnected = true

                    // Register for Bonding notifications
                    mContext?.registerReceiver(mBroadcastReceiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))

                    mConnectionChangedListener?.call(true)
                    // mConnectionChangedListener = null;

                    if (bondState == BondState.UNKNOWN) {
                        mBondingChangedListener?.call(BondState.fromInteger(bluetoothDevice?.bondState) == BondState.BONDED)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("onConnectionStateChange - Disconnected")
                    // mBondState = BondState.Unknown;
                    isConnected = false

                    // Unregister for Bonding notifications
                    try {
                        mContext?.unregisterReceiver(mBroadcastReceiver)
                    } catch (e: Exception) {
                        Timber.e(e.toString())
                    }

                    mConnectionChangedListener?.call(false)
                    // mConnectionChangedListener = null;
                }
            }
            // close();
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Timber.d("onServicesDiscovered - gatt: %s, status: %s", gatt.toString(), status)
            guard(mServicesDiscoveredListener != null) {
                return
            }

            var response = BlueteethResponse.NO_ERROR
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("onServicesDiscovered - Success")
            } else {
                Timber.e("onServicesDiscovered - Failed with status: " + status)
                response = BlueteethResponse.ERROR
            }
            mServicesDiscoveredListener?.call(response)
            mServicesDiscoveredListener = null
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.d("onCharacteristicRead - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString())

            guard(mCharacteristicReadListener != null) {
                return
            }

            var response = BlueteethResponse.NO_ERROR
            var readData = ByteArray(0)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("onCharacteristicRead - Success")
                readData = characteristic.value
            } else {
                Timber.e("onCharacteristicRead - Failed with status: " + status)
                response = BlueteethResponse.ERROR
            }

            mCharacteristicReadListener?.call(response, readData)
            mCharacteristicReadListener = null
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite - gatt: %s, status: %s, characteristic: %s ", gatt.toString(), status, characteristic.toString())
            guard(mCharacteristicWriteListener != null) {
                return
            }

            var response = BlueteethResponse.NO_ERROR
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("onCharacteristicWrite - Success")
            } else {
                Timber.e("onCharacteristicWrite - Failed with status: " + status)
                response = BlueteethResponse.ERROR
            }

            mCharacteristicWriteListener?.call(response)
            mCharacteristicWriteListener = null
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("OnCharacteristicChanged - gatt: %s, characteristic: %s ", gatt.toString(), characteristic.toString())
            mNotificationMap[characteristic.uuid.toString()]?.call(BlueteethResponse.NO_ERROR, characteristic.value)
        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

                when (state) {
                    BluetoothDevice.BOND_BONDING -> {
                        Timber.d("onReceive - BONDING")
                        bondState = BondState.BONDING
                    }

                    BluetoothDevice.BOND_BONDED -> {
                        Timber.d("onReceive - BONDED")
                        bondState = BondState.BONDED
                        mBondingChangedListener?.call(true)
                    }

                    BluetoothDevice.BOND_NONE -> {
                        Timber.d("onReceive - NONE")
                        bondState = BondState.UNKNOWN
                    }
                }
            }
        }
    }
}

inline fun guard(condition: Boolean, body: () -> Void) {
    if (!condition) {
        body()
    }
}

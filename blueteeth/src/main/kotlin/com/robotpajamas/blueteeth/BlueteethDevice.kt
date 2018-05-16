package com.robotpajamas.blueteeth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.robotpajamas.blueteeth.models.*
import timber.log.Timber
import java.util.*

// TODO: Make this object threadsafe and async-safe (called twice in a row, should return a failure?)
class BlueteethDevice private constructor() : Device {

    // TODO: The handler posts would be better if abstracted away - Does this need to be dependency injected for testing?
    private var context: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null

    val name: String
        get() = bluetoothDevice?.name ?: ""

    val id: String
        get() = bluetoothDevice?.address ?: ""

    /** Connectable **/

    private var autoReconnect = false
    private var connectionHandler: ConnectionHandler? = null

    var isConnected = false
        private set

    // Autoreconnect == true is a slow connection, false is fast
    // https://stackoverflow.com/questions/22214254/android-ble-connect-slowly
    override fun connect(timeout: Int?, autoReconnect: Boolean, block: ConnectionHandler?) {
        this.autoReconnect = autoReconnect
        connectionHandler = block
        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        handler.post {
            if (isConnected) {
                Timber.d("connect: Already connected, returning - disregarding autoReconnect")
                connectionHandler?.invoke(isConnected)
                return@post
            }
            bluetoothGatt = bluetoothDevice?.connectGatt(null, autoReconnect, mGattCallback)
        }
    }

    override fun disconnect(autoReconnect: Boolean) {
        this.autoReconnect = autoReconnect
        handler.post {
            bluetoothGatt?.disconnect() ?: Timber.d("disconnect: Cannot disconnect - GATT is null")
        }
    }

    /** Discoverable **/

    private var discoveryHandler: ServiceDiscovery? = null

    override fun discoverServices(block: ServiceDiscovery?) {
        Timber.d("discoverServices: Attempting to discover services")
        discoveryHandler = block
        handler.post {
            if (!isConnected || bluetoothGatt == null) {
                // TODO: Need proper exceptions/errors
                discoveryHandler?.invoke(Result.Failure(RuntimeException("discoverServices: Device is not connected, or GATT is null")))
                discoveryHandler = null
                return@post
            }
            bluetoothGatt?.discoverServices()
        }
    }

    /** Readable **/

    private var readHandler: ReadHandler? = null

    override fun read(characteristic: UUID, service: UUID, block: ReadHandler) {
        Timber.d("read: Attempting to read $characteristic")

        readHandler = block
        handler.post {
            if (!isConnected || bluetoothGatt == null) {
                readHandler?.invoke(Result.Failure(RuntimeException("read: Device is not connected, or GATT is null")))
                readHandler = null
                return@post
            }

            val gattService = bluetoothGatt?.getService(service)
            if (gattService == null) {
                readHandler?.invoke(Result.Failure(RuntimeException("read: Service not available - $service")))
                readHandler = null
                return@post
            }

            val gattCharacteristic = gattService.getCharacteristic(characteristic)
            if (gattCharacteristic == null) {
                readHandler?.invoke(Result.Failure(RuntimeException("read: Characteristic not available - $characteristic")))
                readHandler = null
                return@post
            }

            bluetoothGatt?.readCharacteristic(gattCharacteristic)
        }
    }

    private val notifications = HashMap<String, ReadHandler>()

    // TODO: Make this async
    override fun subscribeTo(characteristic: UUID, service: UUID, block: ReadHandler) {
        Timber.d("subscribeTo: Adding Notification listener to %s", characteristic.toString())

        guard(bluetoothGatt != null && isConnected) {
            Timber.e("subscribeTo: GATT is null or not connected")
            return
        }

        val gattService = bluetoothGatt?.getService(service)
        guard(gattService != null) {
            Timber.e("subscribeTo: Service not available - %s", service.toString())
            return
        }

        val gattCharacteristic = gattService?.getCharacteristic(characteristic)
        guard(gattCharacteristic != null) {
            Timber.e("subscribeTo: Characteristic not available - %s", characteristic.toString())
            return
        }

        val gattDescriptor = gattCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        guard(gattDescriptor != null) {
            Timber.e("subscribeTo: Descriptor not available - %s", characteristic.toString())
            return
        }

        notifications[characteristic.toString()] = block
        bluetoothGatt?.setCharacteristicNotification(gattCharacteristic, true)
        gattDescriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(gattDescriptor)
    }

    /** Writable **/

    private var writeHandler: WriteHandler? = null

    override fun write(data: ByteArray, characteristic: UUID, service: UUID, type: Writable.Type, block: WriteHandler?) {
        Timber.d("write: Attempting to write ${Arrays.toString(data)} to $characteristic")

        writeHandler = block
        handler.post {
            if (!isConnected || bluetoothGatt == null) {
                writeHandler?.invoke(Result.Failure(RuntimeException("write: Device is not connected, or GATT is null")))
                writeHandler = null
                return@post
            }

            val gattService = bluetoothGatt?.getService(service)
            if (gattService == null) {
                writeHandler?.invoke(Result.Failure(RuntimeException("write: Service not available - $service")))
                writeHandler = null
                return@post
            }

            val gattCharacteristic = gattService.getCharacteristic(characteristic)
            if (gattCharacteristic == null) {
                writeHandler?.invoke(Result.Failure(RuntimeException("write: Characteristic not available - $characteristic")))
                writeHandler = null
                return@post
            }

            gattCharacteristic.value = data
            gattCharacteristic.writeType = when (type) {
                Writable.Type.WITH_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                Writable.Type.WITHOUT_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }

            bluetoothGatt?.writeCharacteristic(gattCharacteristic)
        }
    }

    //    private var mScanRecord: ByteArray = ByteArray(0)

//    private var mConnectionChangedListener: OnConnectionChangedListener? = null
//    private var mServicesDiscoveredListener: OnServicesDiscoveredListener? = null
//    private var mCharacteristicReadListener: OnCharacteristicReadListener? = null
//    private var mCharacteristicWriteListener: OnCharacteristicWriteListener? = null
//    private var mBondingChangedListener: OnBondingChangedListener? = null

//    var rssi: Int = 0
//        internal set


    var macAddress: String = "00:00:00:00:00:00"

//    var scanRecord: ByteArray
//        get() = mScanRecord
//        internal set(scanRecord) {
//            mScanRecord = mScanRecord
//        }

    /***
     * Default constructor with invalid values - only here to help
     * with testability and Mock extensions
     */
    //    internal constructor() {
//        name = "Invalid"
//        macAddress = "00:00:00:00:00:00"
//        bluetoothDevice = null
//         TODO: Does this need to be dependency injected for testing?
//        handler = Handler(Looper.getMainLooper())
//    }

    internal constructor(context: Context, device: BluetoothDevice) : this() {
        bluetoothDevice = device
        // TODO: Need this for registering to the bonding process - ugly...
        this.context = context
    }

    internal constructor(context: Context,
                         device: BluetoothDevice,
                         rssi: Int,
                         scanRecord: ByteArray) : this(context, device) {
//        this.rssi = rssi
//        mScanRecord = scanRecord
    }

//    fun indicateFrom(characteristic: UUID, service: UUID, characteristicReadListener: OnCharacteristicReadListener): Boolean {
//        Timber.d("indicateFrom: Adding Notification listener to %s", characteristic.toString())
//        guard(bluetoothGatt != null && isConnected) {
//            Timber.e("indicateFrom: GATT is null")
//            return false
//        }
//
//        val gattService = bluetoothGatt?.getService(service)
//        guard(gattService != null) {
//            Timber.e("indicateFrom: Service not available - %s", service.toString())
//            return false
//        }
//
//        val gattCharacteristic = gattService?.getCharacteristic(characteristic)
//        guard(gattCharacteristic != null) {
//            Timber.e("indicateFrom: Characteristic not available - %s", characteristic.toString())
//            return false
//        }
//
//        bluetoothGatt?.setCharacteristicNotification(gattCharacteristic, true)
//        val gattDescriptor = gattCharacteristic?.getDescriptor(characteristic)
//        gattDescriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
//        bluetoothGatt?.writeDescriptor(gattDescriptor)
//        return true
//    }

    fun close() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
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
            Timber.d("onConnectionStateChange - gatt: $gatt, status: $status, newState: $newState")

            // Removed check for GATT_SUCCESS - do we care? I think the current state is all that matters...
            // TODO: When changing isConnected to a ConnectionState - account for STATE_CONNECTING as well
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("onConnectionStateChange - Connected")
                    isConnected = true
                    connectionHandler?.invoke(isConnected)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("onConnectionStateChange - Disconnected")
                    isConnected = false
                    connectionHandler?.invoke(isConnected)
                }
            }

            // close();
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Timber.d("onServicesDiscovered - gatt: $gatt, status: $status")

            val result: Result<Boolean> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(true)
                else -> Result.Failure(RuntimeException("onServicesDiscovered - Failed with status: $status"))
            }
            discoveryHandler?.invoke(result)
            discoveryHandler = null
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Timber.d("onCharacteristicRead - gatt: $gatt, status: $status, characteristic: $characteristic")

            val result: Result<ByteArray> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(characteristic.value)
                else -> Result.Failure(RuntimeException("onCharacteristicRead - Failed with status: $status"))
            }
            readHandler?.invoke(result)
            readHandler = null
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Timber.d("onCharacteristicWrite - gatt: $gatt, status: $status, characteristic: $characteristic")

            val result: Result<Boolean> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(true)
                else -> Result.Failure(RuntimeException("onCharacteristicWrite - Failed with status: $status"))
            }
            writeHandler?.invoke(result)
            writeHandler = null
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            Timber.d("OnCharacteristicChanged - gatt: $gatt, characteristic: $characteristic")
            notifications[characteristic.uuid.toString()]?.invoke(Result.Success(characteristic.value))
        }
    }
}

inline fun guard(condition: Boolean, body: () -> Void) {
    if (!condition) {
        body()
    }
}

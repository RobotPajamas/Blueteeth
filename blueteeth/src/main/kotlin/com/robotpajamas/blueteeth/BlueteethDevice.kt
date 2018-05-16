package com.robotpajamas.blueteeth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.robotpajamas.blueteeth.extensions.compositeId
import com.robotpajamas.blueteeth.internal.Dispatcher
import com.robotpajamas.blueteeth.models.*
import java.util.*

// TODO: Make this object threadsafe and async-safe (called twice in a row, should return a failure?)
class BlueteethDevice private constructor() : Device {

    // TODO: The handler posts would be better if abstracted away - Does this need to be dependency injected for testing?
    private var context: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var dispatcher = Dispatcher()

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
        BLog.d("connect: Attempting to connect: Timeout=$timeout, autoReconnect=$autoReconnect")
        this.autoReconnect = autoReconnect
        connectionHandler = block
        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        handler.post {
            if (isConnected) {
                BLog.d("connect: Already connected, returning - disregarding autoReconnect")
                connectionHandler?.invoke(isConnected)
                return@post
            }
            bluetoothGatt = bluetoothDevice?.connectGatt(null, autoReconnect, mGattCallback)
        }
    }

    override fun disconnect(autoReconnect: Boolean) {
        BLog.d("disconnect: Disconnecting... autoReconnect=$autoReconnect")
        this.autoReconnect = autoReconnect
        handler.post {
            bluetoothGatt?.disconnect() ?: BLog.d("disconnect: Cannot disconnect - GATT is null")
        }
    }

    /** Discoverable **/

    override fun discoverServices(block: ServiceDiscovery?) {
        BLog.d("discoverServices: Attempting to discover services")
        val item = QueueItem<Boolean>("discoverServices", // TODO: Need something better than hardcoded string
                timeout = 60, // Discovery can take a helluva long time depending on phone
                execution = { cb ->
                    if (!isConnected || bluetoothGatt == null) {
                        // TODO: Need proper exceptions/errors
                        cb(Result.Failure(RuntimeException("discoverServices: Device is not connected, or GATT is null")))
                        return@QueueItem
                    }
                    bluetoothGatt?.discoverServices()
                },
                completion = {
                    it.failure {
                        BLog.e("Service discovery failed with $it")
                    }
                    block?.invoke(it)
                })

        dispatcher.enqueue(item)
    }

    /** Readable **/

    override fun read(characteristic: UUID, service: UUID, block: ReadHandler) {
        BLog.d("read: Attempting to read $characteristic")

        val compositeId = service.toString() + characteristic.toString()
        val item = QueueItem<ByteArray>(compositeId,
                execution = { cb ->
                    if (!isConnected || bluetoothGatt == null) {
                        cb(Result.Failure(RuntimeException("read: Device is not connected, or GATT is null")))
                        return@QueueItem
                    }

                    val gattService = bluetoothGatt?.getService(service)
                    if (gattService == null) {
                        cb(Result.Failure(RuntimeException("read: Service not available - $service")))
                        return@QueueItem
                    }

                    val gattCharacteristic = gattService.getCharacteristic(characteristic)
                    if (gattCharacteristic == null) {
                        cb(Result.Failure(RuntimeException("read: Characteristic not available - $characteristic")))
                        return@QueueItem
                    }

                    bluetoothGatt?.readCharacteristic(gattCharacteristic)
                },
                completion = {
                    it.failure {
                        BLog.e("Read completion failed with $it")
                    }
                    block(it)
                })

        dispatcher.enqueue(item)
    }

    private val notifications = HashMap<String, ReadHandler>()

    // TODO: Make this async
    override fun subscribeTo(characteristic: UUID, service: UUID, block: ReadHandler) {
        BLog.d("subscribeTo: Adding Notification listener to %s", characteristic.toString())

        guard(bluetoothGatt != null && isConnected) {
            BLog.e("subscribeTo: GATT is null or not connected")
            return
        }

        val gattService = bluetoothGatt?.getService(service)
        guard(gattService != null) {
            BLog.e("subscribeTo: Service not available - %s", service.toString())
            return
        }

        val gattCharacteristic = gattService?.getCharacteristic(characteristic)
        guard(gattCharacteristic != null) {
            BLog.e("subscribeTo: Characteristic not available - %s", characteristic.toString())
            return
        }

        val gattDescriptor = gattCharacteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        guard(gattDescriptor != null) {
            BLog.e("subscribeTo: Descriptor not available - %s", characteristic.toString())
            return
        }

        notifications[characteristic.toString()] = block
        bluetoothGatt?.setCharacteristicNotification(gattCharacteristic, true)
        gattDescriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(gattDescriptor)
    }

    /** Writable **/

    override fun write(data: ByteArray, characteristic: UUID, service: UUID, type: Writable.Type, block: WriteHandler?) {
        BLog.d("write: Attempting to write ${Arrays.toString(data)} to $characteristic")

        val compositeId = service.toString() + characteristic.toString()
        val item = QueueItem<Boolean>(compositeId,
                execution = { cb ->
                    if (!isConnected || bluetoothGatt == null) {
                        cb(Result.Failure(RuntimeException("write: Device is not connected, or GATT is null")))
                        return@QueueItem
                    }

                    val gattService = bluetoothGatt?.getService(service)
                    if (gattService == null) {
                        cb(Result.Failure(RuntimeException("write: Service not available - $service")))
                        return@QueueItem
                    }

                    val gattCharacteristic = gattService.getCharacteristic(characteristic)
                    if (gattCharacteristic == null) {
                        cb(Result.Failure(RuntimeException("write: Characteristic not available - $characteristic")))
                        return@QueueItem
                    }

                    gattCharacteristic.value = data
                    gattCharacteristic.writeType = when (type) {
                        Writable.Type.WITH_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        Writable.Type.WITHOUT_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    }

                    bluetoothGatt?.writeCharacteristic(gattCharacteristic)
                },
                completion = {
                    it.failure {
                        BLog.e("Write completion failed with $it")
                    }
                    block?.invoke(it)
                })

        dispatcher.enqueue(item)
    }

    //    private var mScanRecord: ByteArray = ByteArray(0)
//    var rssi: Int = 0
//        internal set


    var macAddress: String = "00:00:00:00:00:00"

//    var scanRecord: ByteArray
//        get() = mScanRecord
//        internal set(scanRecord) {
//            mScanRecord = mScanRecord
//        }

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
//        BLog.d("indicateFrom: Adding Notification listener to %s", characteristic.toString())
//        guard(bluetoothGatt != null && isConnected) {
//            BLog.e("indicateFrom: GATT is null")
//            return false
//        }
//
//        val gattService = bluetoothGatt?.getService(service)
//        guard(gattService != null) {
//            BLog.e("indicateFrom: Service not available - %s", service.toString())
//            return false
//        }
//
//        val gattCharacteristic = gattService?.getCharacteristic(characteristic)
//        guard(gattCharacteristic != null) {
//            BLog.e("indicateFrom: Characteristic not available - %s", characteristic.toString())
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
            BLog.e("Could not close the BlueteethDevice")
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            BLog.d("onConnectionStateChange - gatt: $gatt, status: $status, newState: $newState")

            // Removed check for GATT_SUCCESS - do we care? I think the current state is all that matters...
            // TODO: When changing isConnected to a ConnectionState - account for STATE_CONNECTING as well
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    BLog.d("onConnectionStateChange - Connected")
                    isConnected = true
                    connectionHandler?.invoke(isConnected)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    BLog.d("onConnectionStateChange - Disconnected")
                    isConnected = false
                    connectionHandler?.invoke(isConnected)
                }
            }

            // close();
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            BLog.d("onServicesDiscovered - gatt: $gatt, status: $status")

            val result: Result<Boolean> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(true)
                else -> Result.Failure(RuntimeException("onServicesDiscovered - Failed with status: $status"))
            }
            dispatcher.dispatched<Boolean>("discoverServices")?.complete(result)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            BLog.d("onCharacteristicRead - gatt: $gatt, status: $status, characteristic: ${characteristic.compositeId}")

            val result: Result<ByteArray> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(characteristic.value)
                else -> Result.Failure(RuntimeException("onCharacteristicRead - Failed with status: $status"))
            }
            dispatcher.dispatched<ByteArray>(characteristic.compositeId)?.complete(result)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            BLog.d("onCharacteristicWrite - gatt: $gatt, status: $status, characteristic: $characteristic")

            val result: Result<Boolean> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(true)
                else -> Result.Failure(RuntimeException("onCharacteristicWrite - Failed with status: $status"))
            }
            dispatcher.dispatched<Boolean>(characteristic.compositeId)?.complete(result)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            BLog.d("OnCharacteristicChanged - gatt: $gatt, characteristic: $characteristic")
            notifications[characteristic.uuid.toString()]?.invoke(Result.Success(characteristic.value))
        }
    }
}

inline fun guard(condition: Boolean, body: () -> Void) {
    if (!condition) {
        body()
    }
}

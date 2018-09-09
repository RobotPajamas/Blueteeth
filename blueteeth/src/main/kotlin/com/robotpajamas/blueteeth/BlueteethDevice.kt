package com.robotpajamas.blueteeth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.robotpajamas.blueteeth.extensions.compositeId
import com.robotpajamas.blueteeth.extensions.getCharacteristic
import com.robotpajamas.blueteeth.models.*
import com.robotpajamas.dispatcher.Dispatch
import com.robotpajamas.dispatcher.Result
import com.robotpajamas.dispatcher.RetryPolicy
import com.robotpajamas.dispatcher.SerialDispatcher
import java.util.*

// TODO: Make this object threadsafe and async-safe (called twice in a row, should return a failure?)
class BlueteethDevice private constructor() : Device {

    // TODO: The handler posts would be better if abstracted away - Does this need to be dependency injected for testing?
    private var context: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val subscriptionDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var dispatcher = SerialDispatcher()

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
//        if (bluetoothGatt != null) {
//            bluetoothGatt?.close()
//            bluetoothGatt = null
//        }
        // TODO: Passing in a null context seems to work, but what are the consequences?
        // TODO: Should I grab the application context from the BlueteethManager? Seems odd...
        handler.post {
            if (isConnected) {
                BLog.d("connect: Already connected, returning - disregarding autoReconnect")
                connected()
                return@post
            }
            if (bluetoothGatt != null) {
                bluetoothGatt?.connect()
            } else {
                bluetoothGatt = bluetoothDevice?.connectGatt(null, autoReconnect, mGattCallback)
            }
        }
    }

    override fun disconnect(autoReconnect: Boolean) {
        BLog.d("disconnect: Disconnecting... autoReconnect=$autoReconnect")
        this.autoReconnect = autoReconnect
        handler.post {
            bluetoothGatt?.disconnect() ?: BLog.d("disconnect: Cannot disconnect - GATT is null")
        }
    }

    private fun connected() {
        isConnected = true
        connectionHandler?.invoke(isConnected)
    }

//    private fun connecting() {
//
//    }

    private fun disconnected() {
        isConnected = false
        // Clear all pending queue items
        dispatcher.clear()
        // Clear all subscriptions
        notifications.clear()
        // Call appropriate callbacks
        connectionHandler?.invoke(isConnected)
    }

    /** Discoverable **/

    override fun discoverServices(block: ServiceDiscovery?) {
        BLog.d("discoverServices: Attempting to discover services")
        val item = Dispatch<Boolean>(
                id = "discoverServices", // TODO: Need something better than hardcoded string
                timeout = 60, // Discovery can take a helluva long time depending on phone
                retryPolicy = RetryPolicy.RETRY,
                execution = { cb ->
                    if (!isConnected || bluetoothGatt == null) {
                        // TODO: Need proper exceptions/errors
                        cb(Result.Failure(RuntimeException("discoverServices: Device is not connected, or GATT is null")))
                        return@Dispatch
                    }
                    bluetoothGatt?.discoverServices()
                },
                completion = { result ->
                    result.onFailure {
                        BLog.e("Service discovery failed with $it")
                    }
                    block?.invoke(result)
                })

        dispatcher.enqueue(item)
    }

    /** Readable **/

    override fun read(characteristic: UUID, service: UUID, block: ReadHandler) {
        BLog.d("read: Attempting to read $characteristic")

        val compositeId = service.toString() + characteristic.toString()
        val item = Dispatch<ByteArray>(
                id = compositeId,
                timeout = 3,
                retryPolicy = RetryPolicy.RETRY,
                execution = { cb ->
                    if (!isConnected) {
                        cb(Result.Failure(RuntimeException("read: Device is not connected")))
                        return@Dispatch
                    }

                    val gattCharacteristic = bluetoothGatt?.getCharacteristic(characteristic, service)
                            ?: run {
                                val error = "read: Failed to get Gatt Char: $characteristic in $service from $bluetoothGatt"
                                cb(Result.Failure(RuntimeException(error)))
                                return@Dispatch
                            }

                    bluetoothGatt?.readCharacteristic(gattCharacteristic)
                }) { result ->
            result.onFailure {
                BLog.e("Read completion failed with $it")
            }
            block(result)
        }

        dispatcher.enqueue(item)
    }

    private val notifications = HashMap<String, ReadHandler>()

    override fun subscribeTo(characteristic: UUID, service: UUID, block: ReadHandler) {
        BLog.d("subscribeTo: Adding Notification listener to %s", characteristic.toString())

        val compositeId = service.toString() + characteristic.toString()
        val item = Dispatch<Boolean>(
                id = subscriptionDescriptor.toString(),
                timeout = 3,
                retryPolicy = RetryPolicy.RETRY,
                execution = { cb ->
                    if (!isConnected) {
                        cb(Result.Failure(RuntimeException("subscribe: Device is not connected")))
                        return@Dispatch
                    }
                    val gattCharacteristic = bluetoothGatt?.getCharacteristic(characteristic, service)
                            ?: run {
                                val error = "subscribe: Failed to get Gatt Char: $characteristic in $service from $bluetoothGatt"
                                cb(Result.Failure(RuntimeException(error)))
                                return@Dispatch
                            }

                    val gattDescriptor = gattCharacteristic.getDescriptor(subscriptionDescriptor)
                            ?: run {
                                val error = "subscribe: Descriptor not available - $compositeId"
                                cb(Result.Failure(RuntimeException(error)))
                                return@Dispatch
                            }

                    notifications[compositeId] = block
                    bluetoothGatt?.setCharacteristicNotification(gattCharacteristic, true)
                    gattDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    bluetoothGatt?.writeDescriptor(gattDescriptor)
                }) { result ->
            result.onFailure {
                BLog.e("Read completion failed with $it")
            }
        }
        dispatcher.enqueue(item)
    }

    /** Writable **/

    override fun write(data: ByteArray, characteristic: UUID, service: UUID, type: Writable.Type, block: WriteHandler?) {
        BLog.d("write: Attempting to write ${Arrays.toString(data)} to $characteristic")

        val compositeId = service.toString() + characteristic.toString()
        val item = Dispatch<Boolean>(
                id = compositeId,
                timeout = 3,
                retryPolicy = RetryPolicy.RETRY,
                execution = { cb ->
                    if (!isConnected) {
                        cb(Result.Failure(RuntimeException("write: Device is not connected, or GATT is null")))
                        return@Dispatch
                    }

                    val gattCharacteristic = bluetoothGatt?.getCharacteristic(characteristic, service)
                            ?: run {
                                val error = "write: Failed to get Gatt Char: $characteristic in $service from $bluetoothGatt"
                                cb(Result.Failure(RuntimeException(error)))
                                return@Dispatch
                            }

                    gattCharacteristic.value = data
                    gattCharacteristic.writeType = when (type) {
                        Writable.Type.WITH_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        Writable.Type.WITHOUT_RESPONSE -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    }

                    bluetoothGatt?.writeCharacteristic(gattCharacteristic)
                }) { result ->
            result.onFailure {
                BLog.e("Write completion failed with $it")
            }
            block?.invoke(result)
        }

        dispatcher.enqueue(item)
    }

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
                    connected()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    BLog.d("onConnectionStateChange - Connecting")
//                    connecting()
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    BLog.d("onConnectionStateChange - Disconnecting")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    BLog.d("onConnectionStateChange - Disconnected")
                    disconnected()
                }
            }
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
            BLog.d("OnCharacteristicChanged - gatt: $gatt, characteristic: ${characteristic.compositeId}")
            notifications[characteristic.compositeId]?.invoke(Result.Success(characteristic.value))
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            BLog.d("onDescriptorWrite - gatt: $gatt, descriptor: ${descriptor?.uuid?.toString()}, status: $status")
            val result: Result<Boolean> = when (status) {
                BluetoothGatt.GATT_SUCCESS -> Result.Success(true)
                else -> Result.Failure(RuntimeException("onDescriptorWrite - Failed with status: $status"))
            }
            dispatcher.dispatched<Boolean>(descriptor?.uuid.toString())?.complete(result)
        }
    }
}

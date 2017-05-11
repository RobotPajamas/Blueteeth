package com.robotpajamas.blueteeth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler

import com.robotpajamas.blueteeth.listeners.OnDeviceDiscoveredListener
import com.robotpajamas.blueteeth.listeners.OnScanCompletedListener

import java.util.ArrayList

import timber.log.Timber

// TODO: Fix support for pre-Lollipop vs post
// TODO: Make this less depedendent on the Context?

object Blueteeth {
//class Blueteeth private constructor() {
    init {
        println("($this) is in initialization")
    }

//    private object Holder {
//        val INSTANCE by lazy { Blueteeth() }
//    }

    // TODO: This is a horrible pattern - fix this, probably not threadsafe
//    companion object {
//        private var instance: Blueteeth? = null
//
//        // Using this syntax to keep the usage compatible with Blueteeth 0.2.0
//        // TODO: Update this to something closer to Swift or more Kotlin-esque
//        @JvmStatic fun with(context: Context): Blueteeth {
//            Timber.e("in With statement")
//            if (instance == null) {
//                instance = Blueteeth(context)
//            }
//            return instance!!
//        }
//
//    }

    private var mContext: Context? = null
    private var mBLEAdapter: BluetoothAdapter? = null
    private val mHandler = Handler()

    var isScanning: Boolean = false
        private set

    private val mScannedPeripherals = ArrayList<BlueteethDevice>()

    /***
     * Returns a list of the stored peripherals
     * @return List of all the scanned for devices
     */
    val peripherals: List<BlueteethDevice>
        get() = mScannedPeripherals

    /***
     * Returns a BlueteethDevice directly from a non-null macAddress
     * @return List of all the scanned for devices
     */
    @Throws(IllegalArgumentException::class)
    fun getPeripheral(macAddress: String): BlueteethDevice {
        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            // TODO: Relax the constraint on this as a nullable or error?
            throw IllegalArgumentException("MacAddress is null or ill-formed")
        }
        return BlueteethDevice(mContext!!, mBLEAdapter!!.getRemoteDevice(macAddress))
    }

    private var mOnScanCompletedListener: OnScanCompletedListener? = null
    private var mOnDeviceDiscoveredListener: OnDeviceDiscoveredListener? = null

    /**
     * Controls the level of logging.
     */
    enum class LogLevel {
        None,
        Debug;

        fun log(): Boolean {
            return this != None
        }
    }

    private val mLogLevel = LogLevel.None

    @Throws(RuntimeException::class)
    fun init(context: Context) {
        Timber.e("In Constructor")
        // Grab the application context in case an activity context was passed in
        mContext = context.applicationContext

        Timber.d("Initializing BluetoothManager")
        val bleManager: BluetoothManager? = mContext?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bleManager == null) {
            Timber.e("Unable to initialize BluetoothManager.")
            throw RuntimeException()
        }

        Timber.d("Initializing BLEAdapter")
        mBLEAdapter = bleManager.adapter
        if (mBLEAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter or Bluetooth is not enabled")
            // TODO: Relax the constraint on Bluetooth being enabled with a runtime error instead
            throw RuntimeException()
        }
    }

    /**
     * Scans for nearby peripherals and fills the mScannedPeripherals ArrayList.
     * Scan will be stopped after input timeout.

     * @param deviceDiscoveredListener callback will be called after each new device discovery
     */
    fun scanForPeripherals(deviceDiscoveredListener: OnDeviceDiscoveredListener) {
        Timber.d("scanForPeripherals")
        mOnDeviceDiscoveredListener = deviceDiscoveredListener
        scanForPeripherals()
    }

    /**
     * Scans for nearby peripherals and fills the mScannedPeripherals ArrayList.
     * Scan will be stopped after input timeout.

     * @param scanTimeoutMillis        timeout in milliseconds after which scan will be stopped
     * @param deviceDiscoveredListener callback will be called after each new device discovery
     * @param scanCompletedListener    callback will be called after scanTimeoutMillis,
     * *                                 filled with nearby peripherals
     */
    fun scanForPeripherals(scanTimeoutMillis: Int,
                           deviceDiscoveredListener: OnDeviceDiscoveredListener,
                           scanCompletedListener: OnScanCompletedListener) {
        Timber.d("scanForPeripherals")
        mOnDeviceDiscoveredListener = deviceDiscoveredListener
        scanForPeripherals(scanTimeoutMillis, scanCompletedListener)
    }

    /**
     * Scans for nearby peripherals and fills the mScannedPeripherals ArrayList.
     * Scan will be stopped after input timeout.

     * @param scanTimeoutMillis     timeout in milliseconds after which scan will be stoped
     * @param scanCompletedListener callback will be called after scanTimeoutMillis,
     * *                              filled with nearby peripherals
     */
    fun scanForPeripherals(scanTimeoutMillis: Int,
                           scanCompletedListener: OnScanCompletedListener) {
        Timber.d("scanForPeripheralsWithTimeout")
        mOnScanCompletedListener = scanCompletedListener
        scanForPeripherals()
        mHandler.postDelayed({ this.stopScanForPeripherals() }, scanTimeoutMillis.toLong())
    }

    /**
     * Scans for nearby peripherals (no timeout) and fills the mScannedPeripherals ArrayList.
     */
    fun scanForPeripherals() {
        Timber.d("scanForPeripherals")
        clearPeripherals()
        isScanning = true
        mBLEAdapter?.startLeScan(mBLEScanCallback)
    }

    private fun clearPeripherals() {
        // TODO: Need to be a bit clever about how these are handled
        // TODO: If this is the last reference, close it, otherwise don't?
        for (blueteethDevice in mScannedPeripherals) {
            blueteethDevice.close()
        }
        mScannedPeripherals.clear()
    }

    /**
     * Stops ongoing scan process
     */
    fun stopScanForPeripherals() {
        Timber.d("stopScanForPeripherals")
        isScanning = false
        mBLEAdapter?.stopLeScan(mBLEScanCallback)
        mOnScanCompletedListener?.call(mScannedPeripherals)
        mOnScanCompletedListener = null
        mOnDeviceDiscoveredListener = null
    }

    private val mBLEScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        val blueteethDevice = BlueteethDevice(mContext!!, device, rssi, scanRecord)
        mScannedPeripherals.add(blueteethDevice)
        mOnDeviceDiscoveredListener?.call(blueteethDevice)
    }
}

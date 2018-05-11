package com.robotpajamas.android.blueteeth

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.text.TextUtils
import android.view.View
import android.widget.ListView
import android.widget.Toast

import com.robotpajamas.blueteeth.Blueteeth
import com.robotpajamas.blueteeth.BlueteethDevice

//import butterknife.BindView;
//import butterknife.ButterKnife;
import timber.log.Timber

class MainActivity : ListActivity() {

    //    @BindView(R.id.swiperefresh)
    internal var mSwipeRefresh: SwipeRefreshLayout? = null
    private var mDeviceAdapter: DeviceScanListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        //        ButterKnife.bind(this);

        Timber.plant(Timber.DebugTree())

        // If BLE support isn't there, quit the app
        checkBluetoothSupport()
        Blueteeth.init(applicationContext)

        //        mSwipeRefresh.setOnRefreshListener(this::startScanning);
        mDeviceAdapter = DeviceScanListAdapter(this)
        listAdapter = mDeviceAdapter
    }

    override fun onResume() {
        super.onResume()

        mDeviceAdapter!!.clear()

        // Start automatic scan
        mSwipeRefresh!!.isRefreshing = true
        startScanning()
    }

    override fun onPause() {
        super.onPause()
        stopScanning()
    }

    override fun onListItemClick(listView: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(listView, view, position, id)
        stopScanning()

        val blueteethDevice = mDeviceAdapter!!.getItem(position)
        val intent = Intent(this, DeviceActivity::class)
        intent.putExtra(getString(R.string.extra_mac_address), blueteethDevice.macAddress)
        startActivity(intent)
    }

    private fun startScanning() {
        // Clear existing devices (assumes none are connected)
        Timber.d("Start scanning")
        mDeviceAdapter!!.clear()
        //        Blueteeth.INSTANCE.scanForPeripherals(DEVICE_SCAN_MILLISECONDS, bleDevices -> {
        //            Timber.d("On Scan completed");
        //            mSwipeRefresh.setRefreshing(false);
        //            for (BlueteethDevice device : bleDevices) {
        //                if (!TextUtils.isEmpty(device.getBluetoothDevice().getName())) {
        //                    Timber.d("%s - %s", device.getName(), device.getMacAddress());
        //                    mDeviceAdapter.add(device);
        //                }
        //            }
        //        });
    }

    private fun stopScanning() {
        // Update the button, and shut off the progress bar
        mSwipeRefresh!!.isRefreshing = false
        Blueteeth.stopScanForPeripherals()
    }

    private fun checkBluetoothSupport() {
        // Check for BLE support - also checked from Android manifest.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            exitApp("No BLE Support...")
        }

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            exitApp("No BLE Support...")
        }


        if (!btAdapter!!.isEnabled) {
            enableBluetooth()
        }
    }

    private fun exitApp(reason: String) {
        // Something failed, exit the app and send a toast as to why
        Toast.makeText(applicationContext, reason, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun enableBluetooth() {
        // Ask user to enable bluetooth if it is currently disabled
        startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BLUETOOTH_ENABLE)
    }

    companion object {
        private val REQ_BLUETOOTH_ENABLE = 1000
        private val DEVICE_SCAN_MILLISECONDS = 10000
    }
}

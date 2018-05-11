package com.robotpajamas.android.blueteeth.main

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.Toast
import com.robotpajamas.android.blueteeth.DeviceScanListAdapter
import com.robotpajamas.android.blueteeth.R
import com.robotpajamas.android.blueteeth.databinding.ActivityMainBinding
import com.robotpajamas.blueteeth.Blueteeth
import timber.log.Timber

class MainActivity : ListActivity() {

    lateinit var binding: ActivityMainBinding
    private val vm by lazy { MainViewModel() }
    private val deviceAdapter by lazy { DeviceScanListAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.vm = vm

        // TODO: Put this in MainApplication?
        // If BLE support isn't there, quit the app
        checkBluetoothSupport()
        Blueteeth.init(applicationContext)

        binding.swiperefresh.setOnRefreshListener { startScanning() }
        listAdapter = deviceAdapter
    }

    override fun onResume() {
        super.onResume()
        deviceAdapter.clear()

        // Start automatic scan
        binding.swiperefresh.isRefreshing = true
        startScanning()
    }

    override fun onPause() {
        super.onPause()
        stopScanning()
    }

    override fun onListItemClick(listView: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(listView, view, position, id)
        stopScanning()

        val blueteethDevice = deviceAdapter.getItem(position)
//        val intent = Intent(this, DeviceActivity::class)
//        intent.putExtra(getString(R.string.extra_mac_address), blueteethDevice.macAddress)
//        startActivity(intent)
    }

    private fun startScanning() {
        // Clear existing devices (assumes none are connected)
        Timber.d("Start scanning")
        deviceAdapter.clear()
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
        binding.swiperefresh.isRefreshing = false
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

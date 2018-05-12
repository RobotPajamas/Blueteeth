package com.robotpajamas.android.blueteeth.ui.scan

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ListView
import android.widget.Toast
import com.robotpajamas.android.blueteeth.R
import com.robotpajamas.android.blueteeth.databinding.ActivityScanBinding
import com.robotpajamas.blueteeth.Blueteeth

class DeviceScanActivity : ListActivity(),
        DeviceScanViewModel.Navigator {

    private val vm by lazy { DeviceScanViewModel(this) }

    private lateinit var binding: ActivityScanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan)

        // TODO: Put this in MainApplication?
        // If BLE support isn't there, quit the app
        checkBluetoothSupport()
        Blueteeth.init(applicationContext)

        binding.vm = vm
        binding.devices.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.devices.adapter = DeviceScanAdapter()

//        binding.swiperefresh.setOnRefreshListener { startScanning() }
//        listAdapter = deviceAdapter
    }

    override fun onResume() {
        super.onResume()
        vm.startScan()
    }

    override fun onPause() {
        super.onPause()
        vm.stopScan()
    }

    override fun onListItemClick(listView: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(listView, view, position, id)
        stopScanning()
    }


    // Update the button, and shut off the progress bar
    private fun stopScanning() {
        binding.swiperefresh.isRefreshing = false
        Blueteeth.stopScanForPeripherals()
    }

    // Check for BLE support - also checked from Android manifest.
    private fun checkBluetoothSupport() {
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

    // Something failed, exit the app and send a toast as to why
    private fun exitApp(reason: String) {
        Toast.makeText(applicationContext, reason, Toast.LENGTH_LONG).show()
        finish()
    }

    // Ask user to enable bluetooth if it is currently disabled
    private fun enableBluetooth() {
        startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BLUETOOTH_ENABLE)
    }

    companion object {
        private val REQ_BLUETOOTH_ENABLE = 1000

    }

    override fun navigateNext(macAddress: String) {
//        val intent = Intent(this, DeviceActivity::class)
//        intent.putExtra(getString(R.string.extra_mac_address), macAddress)
//        startActivity(intent)
    }
}

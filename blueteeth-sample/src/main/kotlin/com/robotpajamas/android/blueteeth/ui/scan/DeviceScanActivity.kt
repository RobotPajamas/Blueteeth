package com.robotpajamas.android.blueteeth.ui.scan

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.robotpajamas.android.blueteeth.R
import com.robotpajamas.android.blueteeth.databinding.ActivityScanBinding
import com.robotpajamas.android.blueteeth.ui.device.DeviceActivity
import com.robotpajamas.android.blueteeth.ui.widgets.recyclers.RecyclerItemClickListener
import com.robotpajamas.blueteeth.Blueteeth

class DeviceScanActivity : Activity(),
        DeviceScanViewModel.StateHandler,
        DeviceScanViewModel.Navigator {

    private val vm by lazy { DeviceScanViewModel(this, this) }
    private val touchListener by lazy {
        RecyclerItemClickListener(this, binding.devices, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                vm.select(position)
            }
        })
    }

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
        binding.devices.addOnItemTouchListener(touchListener)
        binding.swiperefresh.setOnRefreshListener { vm.startScan() }
    }

    override fun onPause() {
        super.onPause()
        vm.stopScan()
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

    override fun scanning() {
        binding.swiperefresh.isRefreshing = true
    }

    override fun notScanning() {
        binding.swiperefresh.isRefreshing = false
    }

    override fun navigateNext(macAddress: String) {
        val intent = Intent(this, DeviceActivity::class.java)
        intent.putExtra(getString(R.string.extra_mac_address), macAddress)
        startActivity(intent)
    }
}

package com.robotpajamas.android.blueteeth.ui.device

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import com.robotpajamas.android.blueteeth.R
import com.robotpajamas.android.blueteeth.databinding.ActivityDeviceBinding

class DeviceActivity : Activity(),
        DeviceViewModel.Navigator {

    private val vm by lazy { DeviceViewModel(this) }
    private lateinit var binding: ActivityDeviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_device)
        binding.vm = vm

        val macAddress = intent.getStringExtra(getString(R.string.extra_mac_address))
        //        mSamplePeripheral = new SamplePeripheral(Blueteeth.INSTANCE.getPeripheral(macAddress));
    }

    override fun onDestroy() {
        super.onDestroy()
//        mSamplePeripheral?.close()
    }


    override fun navigateBack() {

    }
}

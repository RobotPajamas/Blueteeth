package com.robotpajamas.android.blueteeth

//import com.robotpajamas.android.blueteeth.peripherals.SamplePeripheral;

import android.app.Activity
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.robotpajamas.android.blueteeth.databinding.ActivityDeviceBinding
import com.robotpajamas.blueteeth.BlueteethDevice

class DeviceActivity : Activity() {

    lateinit var binding: ActivityDeviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_device)

        val macAddress = intent.getStringExtra(getString(R.string.extra_mac_address))
        //        mSamplePeripheral = new SamplePeripheral(Blueteeth.INSTANCE.getPeripheral(macAddress));
    }

    override fun onDestroy() {
        super.onDestroy()
        mSamplePeripheral?.close()
    }

    private val mSamplePeripheral: BlueteethDevice? = null
    private val mIsConnected: Boolean = false

    //    @BindView(R.id.scrollview)
    internal var mScrollView: ScrollView? = null

    //    @BindView(R.id.textview_console)
    internal var mConsoleTextView: TextView? = null

    //    @BindView(R.id.button_connect)
    internal var mConnectionButton: Button? = null

    //    @BindView(R.id.button_write)
    internal var mWriteButton: Button? = null

    //    @BindView(R.id.button_write_no_response)
    internal var mWriteNoResponseButton: Button? = null

    //    @BindView(R.id.button_read_counter)
    internal var mReadCounterButton: Button? = null

    //    @BindView(R.id.button_toggle_notify)
    internal var mToggleNotifyButton: Button? = null

    //    @BindView(R.id.button_toggle_indicate)
    internal var mToggleIndicateButton: Button? = null

    internal var mConnectionRunnable: Runnable = Runnable {
        if (mIsConnected) {
            mConnectionButton!!.setText(R.string.disconnect)
            mWriteButton!!.isEnabled = true
            mWriteNoResponseButton!!.isEnabled = true
            mReadCounterButton!!.isEnabled = true
            mToggleNotifyButton!!.isEnabled = true
            mToggleIndicateButton!!.isEnabled = true
        } else {
            mConnectionButton!!.setText(R.string.connect)
            mWriteButton!!.isEnabled = false
            mWriteNoResponseButton!!.isEnabled = false
            mReadCounterButton!!.isEnabled = false
            mToggleNotifyButton!!.isEnabled = false
            mToggleIndicateButton!!.isEnabled = false
        }
    }

    private var mNotifyEnabled = false

    //    @OnClick(R.id.button_clear)
    internal fun clearConsole() {
        mConsoleTextView!!.text = ""
    }

    //    @OnClick(R.id.button_connect)
    internal fun connect() {
        if (mIsConnected) {
            updateReceivedData(String.format("Attempting to disconnect from %s - %s...", mSamplePeripheral!!.name, mSamplePeripheral.macAddress))
            //            mSamplePeripheral.disconnect(isConnected -> {
            //                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + "\n");
            //                mIsConnected = isConnected;
            //                runOnUiThread(mConnectionRunnable);
            //            });
        } else {
            updateReceivedData(String.format("Attempting to connect to  %s - %s...", mSamplePeripheral!!.name, mSamplePeripheral.macAddress))
            //            mSamplePeripheral.connect(false, isConnected -> {
            //                updateReceivedData("Connection Status: " + Boolean.toString(isConnected));
            //                mIsConnected = isConnected;
            //                runOnUiThread(mConnectionRunnable);
            //            });
        }
    }

    //    @OnClick(R.id.button_read_counter)
    internal fun readCounter() {
        updateReceivedData("Attempting to Read Counter ...")
        //        mSamplePeripheral.readCounter((response, data) -> {
        //            if (response != BlueteethResponse.NO_ERROR) {
        //                updateReceivedData("Read error... " + response.name());
        //                return;
        //            }
        //            updateReceivedData(Arrays.toString(data));
        //        });
    }

    //    @OnClick(R.id.button_toggle_notify)
    internal fun toggleNotify() {
        mNotifyEnabled = !mNotifyEnabled
        updateReceivedData("Toggle notifications ...")
        //        mSamplePeripheral.toggleNotification(mNotifyEnabled, (response, data) -> {
        //            if (response != BlueteethResponse.NO_ERROR) {
        //                updateReceivedData("Notification error... " + response.name());
        //                return;
        //            }
        //            updateReceivedData(Arrays.toString(data));
        //        });
    }

    //    @OnClick(R.id.button_write)
    internal fun write() {
        updateReceivedData("Attempting to Reset Counter ...")
        //        mSamplePeripheral.writeCounter((byte) 42, response -> {
        //            if (response != BlueteethResponse.NO_ERROR) {
        //                updateReceivedData("Write error... " + response.name());
        //                return;
        //            }
        //            updateReceivedData("Counter characteristic reset to 42");
        //        });
    }

    //    @OnClick(R.id.button_write_no_response)
    internal fun writeNoResponse() {
        updateReceivedData("Resetting Counter with No Response ...")
        //        mSamplePeripheral.writeNoResponseCounter((byte) 42);
    }


    private fun updateReceivedData(message: String) {
        //        runOnUiThread(() -> {
        //            mConsoleTextView.append(message + "\n");
        //            mScrollView.smoothScrollTo(0, mConsoleTextView.getBottom());
        //        });
    }
}

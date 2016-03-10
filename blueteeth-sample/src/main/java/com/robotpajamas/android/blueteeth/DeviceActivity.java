package com.robotpajamas.android.blueteeth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethManager;
import com.robotpajamas.blueteeth.BlueteethUtils;
import com.robotpajamas.blueteeth.Callback.OnConnectionChangedListener;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends Activity {

    // Using standard 16bit UUIDs, transformed into the correct 128-bit UUID
    private static final UUID DEVICE_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_NAME = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_MANUFACTURER_MODEL = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_MANUFACTURER_HARDWARE = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_MANUFACTURER_FIRMWARE = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_MANUFACTURER_SOFTWARE = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb");

    private static final UUID OTA_SERVICE = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0");
    private static final UUID OTA_CONTROL_CHARACTERISTIC = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063");

    private BlueteethDevice mBlueteethDevice;
    private boolean mIsConnected;

    @Bind(R.id.scrollview)
    ScrollView mScrollView;

    @Bind(R.id.textview_console)
    TextView mConsoleTextView;

    @Bind(R.id.button_connect)
    Button mConnectionButton;

    @Bind(R.id.button_read)
    Button mReadButton;

    @Bind(R.id.button_write)
    Button mWriteButton;

    @OnClick(R.id.button_clear)
    void clearConsole() {
        mConsoleTextView.setText("");
    }

    @OnClick(R.id.button_connect)
    void connect() {
        if (mIsConnected) {
            updateReceivedData(String.format("Attempting to disconnect from %s - %s...", mBlueteethDevice.getName(), mBlueteethDevice.getMacAddress()));
            mBlueteethDevice.disconnect(isConnected -> {
                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + "\n");
                mIsConnected = isConnected;
                runOnUiThread(mConnectionRunnable);
            });
        } else {
            updateReceivedData(String.format("Attempting to connect to  %s - %s...", mBlueteethDevice.getName(), mBlueteethDevice.getMacAddress()));
            mBlueteethDevice.connect(true, isConnected -> {
                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + ", Bond State=" + mBlueteethDevice.getBondState());
                mIsConnected = isConnected;
                runOnUiThread(mConnectionRunnable);
            });
        }
    }

    Runnable mConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsConnected) {
                mConnectionButton.setText(R.string.disconnect);
                mReadButton.setEnabled(true);
                mWriteButton.setEnabled(true);
            } else {
                mConnectionButton.setText(R.string.connect);
                mReadButton.setEnabled(false);
                mWriteButton.setEnabled(false);
            }
        }
    };

    @OnClick(R.id.button_read)
    void readCharacteristic() {
        updateReceivedData("Attempting to Read ...");
        BlueteethUtils.read(DEVICE_MANUFACTURER_NAME, DEVICE_SERVICE, mBlueteethDevice, data -> {
            if (data == null) {
                updateReceivedData("No data read...");
                return;
            }

            try {
                updateReceivedData(new String(data));
                updateReceivedData(new String(data, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
    }

    @OnClick(R.id.button_write)
    void writeCharacteristic() {
        updateReceivedData("Attempting to Write ...");
        BlueteethUtils.writeData(new byte[]{1, 2, 3, 4}, OTA_CONTROL_CHARACTERISTIC, OTA_SERVICE, mBlueteethDevice, () -> updateReceivedData("Characteristic written... " + OTA_CONTROL_CHARACTERISTIC.toString() + " with 1234"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        String macAddress = getIntent().getStringExtra(getString(R.string.extra_mac_address));
        mBlueteethDevice = BlueteethManager.with(this).getPeripheral(macAddress);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBlueteethDevice.close();
    }

    private void updateReceivedData(String message) {
        runOnUiThread(() -> {
            mConsoleTextView.append(message + "\n");
            mScrollView.smoothScrollTo(0, mConsoleTextView.getBottom());
        });
    }
}

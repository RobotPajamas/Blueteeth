package com.robotpajamas.android.blueteeth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethManager;
import com.robotpajamas.blueteeth.BlueteethUtils;
import com.robotpajamas.blueteeth.Callback.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.Callback.OnConnectionChangedListener;

import java.util.Arrays;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class DeviceActivity extends Activity {

    // Using standard 16bit UUIDs, transformed into the correct 128-bit UUID
    private static final UUID DEVICE_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_NAME = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_MODEL = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_HARDWARE = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_FIRMWARE = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_SOFTWARE = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb");

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

    @OnClick(R.id.button_clear)
    void clearConsole() {
        mConsoleTextView.setText("");
    }

    @OnClick(R.id.button_connect)
    void connect() {
        if (mIsConnected) {
            updateReceivedData("Attempting to disconnect...");
            mBlueteethDevice.disconnect(isConnected -> {
                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + "\n");
                mIsConnected = isConnected;
                runOnUiThread(mConnectionRunnable);
            });
        } else {
            updateReceivedData("Attempting to connect...");
            mBlueteethDevice.connect(isConnected -> {
                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + "\n");
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
            } else {
                mConnectionButton.setText(R.string.connect);
                mReadButton.setEnabled(false);
            }
        }
    };

    @OnClick(R.id.button_read)
    void sendReadCommand() {
        updateReceivedData("Attempting to Read ...");
        BlueteethUtils.read(DEVICE_MANUFACTURER_NAME, DEVICE_SERVICE, mBlueteethDevice, data -> {
            if (data == null) {
                return;
            }

            Timber.e(Arrays.toString(data));
            for (byte b : data) {
                Timber.e(b + "");
            }

        });
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
    protected void onPause() {
        super.onPause();
        mBlueteethDevice.close();
    }

    private void updateReceivedData(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConsoleTextView.append(message + "\n");
                mScrollView.smoothScrollTo(0, mConsoleTextView.getBottom());
            }
        });
    }
}

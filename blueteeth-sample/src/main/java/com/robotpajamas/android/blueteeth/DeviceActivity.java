package com.robotpajamas.android.blueteeth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.robotpajamas.android.blueteeth.peripherals.SamplePeripheral;
import com.robotpajamas.blueteeth.BlueteethManager;
import com.robotpajamas.blueteeth.BlueteethResponse;

import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends Activity {

    private SamplePeripheral mSamplePeripheral;
    private boolean mIsConnected;

    @Bind(R.id.scrollview)
    ScrollView mScrollView;

    @Bind(R.id.textview_console)
    TextView mConsoleTextView;

    @Bind(R.id.button_connect)
    Button mConnectionButton;

    @Bind(R.id.button_read_counter)
    Button mReadCounterButton;

    @Bind(R.id.button_write)
    Button mWriteButton;

    @Bind(R.id.button_write_no_response)
    Button mWriteNoResponseButton;

    @OnClick(R.id.button_clear)
    void clearConsole() {
        mConsoleTextView.setText("");
    }

    @OnClick(R.id.button_connect)
    void connect() {
        if (mIsConnected) {
            updateReceivedData(String.format("Attempting to disconnect from %s - %s...", mSamplePeripheral.getName(), mSamplePeripheral.getMacAddress()));
            mSamplePeripheral.disconnect(isConnected -> {
                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + "\n");
                mIsConnected = isConnected;
                runOnUiThread(mConnectionRunnable);
            });
        } else {
            updateReceivedData(String.format("Attempting to connect to  %s - %s...", mSamplePeripheral.getName(), mSamplePeripheral.getMacAddress()));
            mSamplePeripheral.connect(true, isConnected -> {
                updateReceivedData("Connection Status: " + Boolean.toString(isConnected));
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
                mReadCounterButton.setEnabled(true);
                mWriteButton.setEnabled(true);
                mWriteNoResponseButton.setEnabled(true);
            } else {
                mConnectionButton.setText(R.string.connect);
                mReadCounterButton.setEnabled(false);
                mWriteButton.setEnabled(false);
                mWriteNoResponseButton.setEnabled(false);
            }
        }
    };

    @OnClick(R.id.button_read_counter)
    void readCounter() {
        updateReceivedData("Attempting to Read Counter ...");
        mSamplePeripheral.readCounter((response, data) -> {
            if (response != BlueteethResponse.NO_ERROR) {
                updateReceivedData("Read error... " + response.name());
                return;
            }

            updateReceivedData(Arrays.toString(data));
        });
    }

    @OnClick(R.id.button_write)
    void write() {
        updateReceivedData("Attempting to Reset Counter ...");
        mSamplePeripheral.writeCounter((byte) 42, response -> {
            if (response != BlueteethResponse.NO_ERROR) {
                updateReceivedData("Write error... " + response.name());
                return;
            }
            updateReceivedData("Counter characteristic reset to 42");
        });
    }

    @OnClick(R.id.button_write_no_response)
    void writeNoResponse() {
        updateReceivedData("Resetting Counter with No Response ...");
        mSamplePeripheral.writeNoResponseCounter((byte) 42);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        String macAddress = getIntent().getStringExtra(getString(R.string.extra_mac_address));
        mSamplePeripheral = new SamplePeripheral(BlueteethManager.with(this).getPeripheral(macAddress));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSamplePeripheral.close();
    }

    private void updateReceivedData(String message) {
        runOnUiThread(() -> {
            mConsoleTextView.append(message + "\n");
            mScrollView.smoothScrollTo(0, mConsoleTextView.getBottom());
        });
    }
}

package com.robotpajamas.android.blueteeth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.robotpajamas.blueteeth.BlueteethDevice;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends Activity {

    private BlueteethDevice device;
    private boolean mIsConnected;

    @BindView(R.id.scrollview)
    ScrollView mScrollView;

    @BindView(R.id.textview_console)
    TextView mConsoleTextView;

    @BindView(R.id.button_connect)
    Button mConnectionButton;

    @BindView(R.id.button_write)
    Button mWriteButton;

    @BindView(R.id.button_write_no_response)
    Button mWriteNoResponseButton;

    @BindView(R.id.button_read_counter)
    Button mReadCounterButton;

    @BindView(R.id.button_toggle_notify)
    Button mToggleNotifyButton;

    @BindView(R.id.button_toggle_indicate)
    Button mToggleIndicateButton;

    @OnClick(R.id.button_clear)
    void clearConsole() {
        mConsoleTextView.setText("");
    }

    @OnClick(R.id.button_connect)
    void connect() {
        if (mIsConnected) {
            updateReceivedData(String.format("Attempting to disconnect from %s - %s...", device.getName(), device.getMacAddress()));
//            device.disconnect(isConnected -> {
//                updateReceivedData("Connection Status: " + Boolean.toString(isConnected) + "\n");
//                mIsConnected = isConnected;
//                runOnUiThread(mConnectionRunnable);
//            });
        } else {
            updateReceivedData(String.format("Attempting to connect to  %s - %s...", device.getName(), device.getMacAddress()));
//            device.connect(false, isConnected -> {
//                updateReceivedData("Connection Status: " + Boolean.toString(isConnected));
//                mIsConnected = isConnected;
//                runOnUiThread(mConnectionRunnable);
//            });
        }
    }

    Runnable mConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsConnected) {
                mConnectionButton.setText(R.string.disconnect);
                mWriteButton.setEnabled(true);
                mWriteNoResponseButton.setEnabled(true);
                mReadCounterButton.setEnabled(true);
                mToggleNotifyButton.setEnabled(true);
                mToggleIndicateButton.setEnabled(true);
            } else {
                mConnectionButton.setText(R.string.connect);
                mWriteButton.setEnabled(false);
                mWriteNoResponseButton.setEnabled(false);
                mReadCounterButton.setEnabled(false);
                mToggleNotifyButton.setEnabled(false);
                mToggleIndicateButton.setEnabled(false);
            }
        }
    };

    @OnClick(R.id.button_read_counter)
    void readCounter() {
        updateReceivedData("Attempting to Read Counter ...");
//        device.read() .readCounter((response, data) -> {
//            if (response != BlueteethResponse.NO_ERROR) {
//                updateReceivedData("Read error... " + response.name());
//                return;
//            }
//            updateReceivedData(Arrays.toString(data));
//        });
    }

    private boolean mNotifyEnabled = false;

    @OnClick(R.id.button_toggle_notify)
    void toggleNotify() {
        mNotifyEnabled = !mNotifyEnabled;
        updateReceivedData("Toggle notifications ...");
//        mSamplePeripheral.toggleNotification(mNotifyEnabled, (response, data) -> {
//            if (response != BlueteethResponse.NO_ERROR) {
//                updateReceivedData("Notification error... " + response.name());
//                return;
//            }
//            updateReceivedData(Arrays.toString(data));
//        });
    }

    @OnClick(R.id.button_write)
    void write() {
        updateReceivedData("Attempting to Reset Counter ...");
//        mSamplePeripheral.writeCounter((byte) 42, response -> {
//            if (response != BlueteethResponse.NO_ERROR) {
//                updateReceivedData("Write error... " + response.name());
//                return;
//            }
//            updateReceivedData("Counter characteristic reset to 42");
//        });
    }

    @OnClick(R.id.button_write_no_response)
    void writeNoResponse() {
        updateReceivedData("Resetting Counter with No Response ...");
//        mSamplePeripheral.writeNoResponseCounter((byte) 42);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        String macAddress = getIntent().getStringExtra(getString(R.string.extra_mac_address));
//        mSamplePeripheral = new SamplePeripheral(Blueteeth.INSTANCE.getPeripheral(macAddress));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mSamplePeripheral.close();
    }

    private void updateReceivedData(String message) {
        runOnUiThread(() -> {
            mConsoleTextView.append(message + "\n");
            mScrollView.smoothScrollTo(0, mConsoleTextView.getBottom());
        });
    }
}

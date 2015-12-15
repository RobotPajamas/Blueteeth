package com.robotpajamas.android.blueteeth;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethManager;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DeviceActivity extends Activity {

    @Bind(R.id.textview_connected)
    TextView mConnectedTextView;

    private BlueteethDevice mBlueteethDevice;

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
        mBlueteethDevice.connect(isConnected -> runOnUiThread(() -> mConnectedTextView.setText(isConnected + "")));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBlueteethDevice.close();
    }
}

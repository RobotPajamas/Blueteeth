package com.robotpajamas.android.blueteeth;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethManager;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends ListActivity {
    private static final int REQ_BLUETOOTH_ENABLE = 1000;
    private static final int DEVICE_SCAN_MILLISECONDS = 5000;

    @Bind(R.id.swiperefresh)
    SwipeRefreshLayout mSwipeRefresh;
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Timber.plant(new Timber.DebugTree());

        // If BLE support isn't there, quit the app
        checkBluetoothSupport();

        mSwipeRefresh.setOnRefreshListener(this::startScanning);

        // Setup the device array adapter
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAdapter.clear();

        // Start automatic scan
        mSwipeRefresh.setRefreshing(true);
        startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        stopScanning();

        List<BlueteethDevice> blueteethDevices = BlueteethManager.with(this).getPeripherals();
        BlueteethDevice blueteethDevice = blueteethDevices.get(position);

        final Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(getString(R.string.extra_mac_address), blueteethDevice.getMacAddress());
        startActivity(intent);
    }

    private void startScanning() {
        // Clear existing devices (assumes none are connected)
        Timber.d("Start scanning");
        mAdapter.clear();
        BlueteethManager.with(this).scanForPeripherals(DEVICE_SCAN_MILLISECONDS, bleDevices -> {
            Timber.d("On Scan completed");
            mSwipeRefresh.setRefreshing(false);
            for (BlueteethDevice device : bleDevices) {
                if (!TextUtils.isEmpty(device.getBluetoothDevice().getName())) {
                    mAdapter.add(device.getBluetoothDevice().getName());
                }
            }
        });
    }

    private void stopScanning() {
        // Update the button, and shut off the progress bar
        mSwipeRefresh.setRefreshing(false);
        BlueteethManager.with(this).stopScanForPeripherals();
    }

    private void checkBluetoothSupport() {
        // Check for BLE support - also checked from Android manifest.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            exitApp("No BLE Support...");
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            exitApp("No BLE Support...");
        }

        //noinspection ConstantConditions
        if (!btAdapter.isEnabled()) {
            enableBluetooth();
        }
    }

    private void exitApp(String reason) {
        // Something failed, exit the app and send a toast as to why
        Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_LONG).show();
        finish();
    }

    private void enableBluetooth() {
        // Ask user to enable bluetooth if it is currently disabled
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BLUETOOTH_ENABLE);
    }
}

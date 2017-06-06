package com.lucasbritz.proximitybeacon;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 1;
    private static final String TAG = "MainActivity";
    private static final String BEACON_ADDRESS = "D3:8A:63:6D:FB:79";

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    private TextView tvAddress;
    private TextView tvRssi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAddress = (TextView) findViewById(R.id.tvAddress);
        tvRssi = (TextView) findViewById(R.id.tvRssi);

        if (needPermissions(this)) {
            requestPermissions();
        }

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();

            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.startScan(filters, settings, mScanCallback);
                }
            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            String name = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            int rssi = result.getRssi();
            discoveredBeacon(name, address, rssi);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }
    };

    private void discoveredBeacon(String name, String address, int rssi) {
        if (name != null && BEACON_ADDRESS.equals(address)) {
            tvAddress.setText("Beacon encontrado - " + name);
            tvRssi.setText(address + " (" + rssi + ")");
        } else {
            tvAddress.setText("Nenhum beacon encontrado");
            tvRssi.setText("");
        }
    }

    static public boolean needPermissions(Activity activity) {
        Log.d(TAG, "needPermissions: ");
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        Log.d(TAG, "requestPermissions: ");
        String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };
        requestPermissions(permissions, PERMISSIONS_REQUEST_ALL_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ALL_PERMISSIONS:
                boolean hasAllPermissions = true;
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        hasAllPermissions = false;
                        Log.e(TAG, "Unable to get permission " + permissions[i]);
                    }
                }
                if (hasAllPermissions) {
                    finish();
                } else {
                    Toast.makeText(this,
                            "Unable to get all required permissions", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                break;
            default:
                Log.e(TAG, "Unexpected request code");
        }
    }
}

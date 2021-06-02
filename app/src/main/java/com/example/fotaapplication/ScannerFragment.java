package com.example.fotaapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Set;

import static com.example.fotaapplication.MainActivity.SCANNER_FRAGMENT;

public class ScannerFragment extends Fragment implements View.OnClickListener {

    private View mView;
    private DeviceAdapter mDeviceAdapter;
    private ListView mListView;

    // Bluetooth
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    // Stops scanning after 5 seconds
    private boolean mScanning = false;
    private static final long SCAN_TIMEOUT = 5000;
    private Handler mScanTimeOutHandler = new Handler(Looper.getMainLooper());
    private Runnable mScanTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (scanner != null && mBluetoothAdapter.isEnabled()) {
                mScanning = false;
                stopScan();
            }
        }
    };

    /**
     * Callback for BLE Scan
     * This callback is called when a BLE device is found near by.
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                // Should not happen.
                return;
            }
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                return;
            }
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceAdapter.addDevice(result.getDevice(), result.getRssi());
                        notifyDeviceListUpdated();
                    }
                });
            }
        }
    };

    public ScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_scanner, container, false);
        MainActivity.currentFragment = SCANNER_FRAGMENT;
        mView = rootView;
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btnScan).setOnClickListener(this);
        mDeviceAdapter = new DeviceAdapter(getActivity());
        mListView = mView.findViewById(R.id.lvDevices);
        mListView.setAdapter(mDeviceAdapter);

        if(initializeBluetooth()){
            startScanTimer();
            startScan();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnScan:
                if (!mScanning) {
                    startScanTimer();
                    startScan();
                } else {
                    cancelScanTimer();
                    stopScan();
                }
                break;
        }
    }

    private boolean initializeBluetooth() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;
    }

    private void startScan(){
        mDeviceAdapter.clear();

        // get bonded devices
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice currentDevice : bondedDevices) {
                mDeviceAdapter.addDevice(currentDevice, 1);
            }
        }
        notifyDeviceListUpdated();

        // scan for other devices
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        Button b = mView.findViewById(R.id.btnScan);
        if (scanner != null) {
            mScanning = true;
            b.setText(R.string.button_device_scanning);
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan(null, settings, mScanCallback);
        } else {
            mScanning = false;
            b.setText(R.string.button_device_not_scanning);
        }
    }

    private void stopScan() {
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) {
            mScanning = false;
            Button b = mView.findViewById(R.id.btnScan);
            b.setText(R.string.button_device_not_scanning);
            scanner.stopScan(mScanCallback);
        }
    }

    private void notifyDeviceListUpdated() {
        try {
            mDeviceAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startScanTimer() {
        cancelScanTimer();
        mScanTimeOutHandler.postDelayed(mScanTimeOutRunnable, SCAN_TIMEOUT);
    }

    private void cancelScanTimer() {
        mScanTimeOutHandler.removeCallbacks(mScanTimeOutRunnable);
    }
}
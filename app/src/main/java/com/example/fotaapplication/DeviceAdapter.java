package com.example.fotaapplication;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.innovationzed.fotalibrary.FotaApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DeviceAdapter extends BaseAdapter {
    private static String DEVICE_NAME_UNKNOWN;

    private ArrayList<BluetoothDevice> deviceList;
    private FragmentActivity mActivity;
    private Map<String, Integer> rssiValues;

    public DeviceAdapter(FragmentActivity activity) {
        DEVICE_NAME_UNKNOWN = activity.getString(R.string.unknown_device);
        this.deviceList = new ArrayList<>();
        this.rssiValues = new HashMap<>();
        this.mActivity = activity;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        if (view == null) {
            view = mActivity.getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
        }

        String name = deviceList.get(position).getName();
        if (name == null) {
            name = DEVICE_NAME_UNKNOWN;
        }
        TextView deviceName = view.findViewById(R.id.tvDeviceName);
        deviceName.setText(name);

        String address = deviceList.get(position).getAddress();
        TextView deviceAddress = view.findViewById(R.id.tvMacAddress);
        deviceAddress.setText(address);

        TextView deviceRssid = view.findViewById(R.id.tvDeviceRssi);
        String rssiString = rssiValues.get(address) + "dBm";
        if (rssiValues.get(address) > 0) {
            rssiString = "";
        }
        deviceRssid.setText(rssiString);

        Button selectButton = view.findViewById(R.id.btnSelect);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FotaApi.macAddress = deviceList.get(position).getAddress();
                CommonUtils.replaceFragment(mActivity, new UpdateFragment(), "update fragment", true);
            }
        });

        return view;
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            rssiValues.put(device.getAddress(), rssi);
        }
    }

    public void clear() {
        deviceList.clear();
    }
}

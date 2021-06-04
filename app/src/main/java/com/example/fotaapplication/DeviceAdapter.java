package com.example.fotaapplication;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.innovationzed.fotalibrary.FotaApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DeviceAdapter extends BaseAdapter implements Filterable {
    private static String DEVICE_NAME_UNKNOWN;

    private ArrayList<BluetoothDevice> mDeviceList;
    private ArrayList<BluetoothDevice> mFilteredDeviceList;
    private ItemFilter mFilter = new ItemFilter();
    private boolean mFilteringActive = false;
    private FragmentActivity mActivity;
    private Map<String, Integer> mRssiValues;

    public DeviceAdapter(FragmentActivity activity) {
        DEVICE_NAME_UNKNOWN = activity.getString(R.string.unknown_device);
        this.mDeviceList = new ArrayList<>();
        this.mFilteredDeviceList = new ArrayList<>();
        this.mRssiValues = new HashMap<>();
        this.mActivity = activity;
    }

    @Override
    public int getCount() {
        final ArrayList<BluetoothDevice> devices = mFilteringActive ? mFilteredDeviceList : mDeviceList;
        return devices.size();
    }

    @Override
    public Object getItem(int i) {
        final ArrayList<BluetoothDevice> devices = mFilteringActive ? mFilteredDeviceList : mDeviceList;
        return devices.get(i);
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

        ArrayList<BluetoothDevice> currentList = mFilteringActive ? mFilteredDeviceList : mDeviceList;

        BluetoothDevice device = currentList.get(position);
        String name = currentList.get(position).getName();
        if (name == null) {
            name = DEVICE_NAME_UNKNOWN;
        }
        TextView deviceName = view.findViewById(R.id.tvDeviceName);
        deviceName.setText(name);

        String address = currentList.get(position).getAddress();
        TextView deviceAddress = view.findViewById(R.id.tvMacAddress);
        deviceAddress.setText(address);

        TextView deviceRssid = view.findViewById(R.id.tvDeviceRssi);
        String rssiString = mRssiValues.get(address) + "dBm";
        if (mRssiValues.get(address) > 0) {
            rssiString = "";
        }
        deviceRssid.setText(rssiString);

        Button selectButton = view.findViewById(R.id.btnSelect);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FotaApi.macAddress = currentList.get(position).getAddress();
                CommonUtils.replaceFragment(mActivity, new UpdateFragment(), mActivity.getString(R.string.update_fragment_tag), true);
            }
        });

        return view;
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        if (!mDeviceList.contains(device)) {
            mDeviceList.add(device);
            mRssiValues.put(device.getAddress(), rssi);
        }
    }

    private void addFilteredDevice(BluetoothDevice device, int rssi) {
        // New device found
        mRssiValues.put(device.getAddress(), rssi);
        if (!mFilteredDeviceList.contains(device)) {
            mFilteredDeviceList.add(device);
        }
    }

    public void setFilteringActive(boolean val) {
        mFilteringActive = val;
    }

    public void clear() {
        setFilteringActive(false);
        mDeviceList.clear();
        mFilteredDeviceList.clear();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // Not performing filtering here as this method is executed in non-main thread.
            // Instead performing filtering in publishResults which is executed in main thread.
            // This is to omit synchronized access to the mDevices variable.
            return new FilterResults();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            String filterString = constraint.toString().toLowerCase();
            final ArrayList<BluetoothDevice> list = mDeviceList;
            final ArrayList<BluetoothDevice> filteredList = new ArrayList<>(list.size());

            for (int i = 0, n = list.size(); i < n; i++) {
                String name = list.get(i).getName();
                if (name == null) {
                    name = DEVICE_NAME_UNKNOWN;
                }
                name = name.toLowerCase();

                String address = list.get(i).getAddress().toLowerCase();

                if (name.contains(filterString) || address.contains(filterString)) {
                    filteredList.add(list.get(i));
                }
            }

            mFilteredDeviceList.clear();
            for (int i = 0, n = filteredList.size(); i < n; i++) {
                BluetoothDevice device = filteredList.get(i);
                addFilteredDevice(device, mRssiValues.get(device.getAddress()));
            }
            notifyDataSetChanged(); // notifies the data with new filtered values
        }
    }
}

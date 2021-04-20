package com.innovationzed.fotalibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.BackendCommunication.BackendApiRequest;
import com.innovationzed.fotalibrary.BackendCommunication.Firmware;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.OTAFirmwareUpdate.OTAFirmwareUpgrade;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FotaApi {

    public static String macAddress;
    public static String latestFirmwareVersion;
    public static final String ROOT_DIR = "/storage/emulated/0/CySmart";

    private Context mContext;
    private Intent mIntent;
    private static Dictionary mDeviceInformation;
    private BackendApiRequest mBackend;

    private boolean mUpdatePossible;
    private boolean mUpdatePossibleChecked;

//    private BroadcastReceiver mOTAStatusReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            synchronized (this) {
//                final String action = intent.getAction();
//                if (action.equals(BluetoothLeService.ACTION_OTA_SUCCESS) || action.equals(BluetoothLeService.ACTION_OTA_FAIL)){
//                    mContext.stopService(mIntent);
//                }
//            }
//        }
//    };

    public FotaApi (Context context, String macAddress){
        this.macAddress = macAddress;

        mContext = context;
        mIntent = new Intent(mContext, OTAFirmwareUpgrade.class);
        mBackend = new BackendApiRequest(context);

        // Register receiver
//        BluetoothLeService.registerBroadcastReceiver(mContext, mOTAStatusReceiver, Utils.makeOTAFinishedIntentFilter());

        // Set up dummy device data
        mDeviceInformation = new Hashtable();
        getDeviceInformation();

        // Check if firmware update is possible
        mUpdatePossible = false;
        mUpdatePossibleChecked = false;
        checkFirmwareUpdatePossible();
    }

    public boolean isFirmwareUpdatePossible(){
        if (mUpdatePossibleChecked){
            return mUpdatePossible;
        }
        return false;
    }

    public void doFirmwareUpdate(boolean userConfirmation){
        if (userConfirmation){
            mContext.startService(mIntent);
        }
    }

    private void getDeviceInformation(){
        mDeviceInformation.put("deviceSN", "12345");
        mDeviceInformation.put("firmwareVersion", "0.3.0");
        mDeviceInformation.put("batteryLevel", 75);
    }

    public static String getDeviceSN(){
        return (String) mDeviceInformation.get("deviceSN");
    }

    public static String getDeviceFirmwareVersion(){
        return (String) mDeviceInformation.get("firmwareVersion");
    }

    private int compareVersion(String version1, String version2) {
        String[] array1 = version1.split("\\.");
        String[] array2 = version2.split("\\.");

        int i = 0;
        while (i < array1.length || i < array2.length){
            if (i < array1.length && i < array2.length){
                if (Integer.parseInt(array1[i]) < Integer.parseInt(array2[i])) {
                    return -1;
                } else if (Integer.parseInt(array1[i]) > Integer.parseInt(array2[i])) {
                    return 1;
                }
            } else if (i < array1.length) {
                if (Integer.parseInt(array1[i]) != 0) {
                    return 1;
                }
            } else if (i < array2.length) {
                if (Integer.parseInt(array2[i]) != 0) {
                    return -1;
                }
            }
            i++;
        }
        return 0;
    }

    private void checkFirmwareUpdatePossible(){
        // Check wifi
        boolean wifi = Utils.checkWifi(mContext);

        if (wifi){
            // Get latest available firmware version and do the checks on response
            Callback callback = new Callback<List<Firmware>>() {
                @Override
                public void onResponse(Call<List<Firmware>> call, Response<List<Firmware>> response) {

                    if (!response.isSuccessful()) {
                        return;
                    }
                    List<Firmware> versions = response.body();
                    latestFirmwareVersion = versions.get(0).getFirmwareVersion();

                    boolean updatePossible = true;

                    // Compare firmware versions
                    if (compareVersion((String)mDeviceInformation.get("firmwareVersion"), latestFirmwareVersion) >= 0){
                        updatePossible = false;
                    }

                    // Check phone battery
                    BatteryManager bm = (BatteryManager)mContext.getSystemService(mContext.BATTERY_SERVICE);
                    int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    if (percentage < 50){
                        updatePossible = false;
                    }

                    // Check device battery
                    if ((int)mDeviceInformation.get("batteryLevel") < 50){
                        updatePossible = false;
                    }

                    mUpdatePossible = updatePossible;
                    mUpdatePossibleChecked = true;
                }

                @Override
                public void onFailure(Call<List<Firmware>> call, Throwable t) {
                }
            };

            mBackend.getLatestFirmwareVersion(callback);
        }

    }
}

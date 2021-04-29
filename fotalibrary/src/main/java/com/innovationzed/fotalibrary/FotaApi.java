package com.innovationzed.fotalibrary;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.BackendCommunication.BackendApiRequest;
import com.innovationzed.fotalibrary.BackendCommunication.Firmware;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.OTAFirmwareUpdate.OTAFirmwareUpgrade;

import java.io.File;
import java.util.Dictionary;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.innovationzed.fotalibrary.CommonUtils.Utils.OTA_REASON;

public class FotaApi {

    public static String macAddress;
    public static String latestFirmwareVersion;
    public static String DOWNLOADED_FIRMWARE_DIR;
    public static final String ROOT_DIR = "/storage/emulated/0/Download";
    public static final String ACTION_FOTA_POSSIBLE = BluetoothLeService.ACTION_OTA_IS_POSSIBLE;
    public static final String ACTION_FOTA_NOT_POSSIBLE = BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE;
    public static final String ACTION_FOTA_SUCCESS = BluetoothLeService.ACTION_OTA_SUCCESS;
    public static final String ACTION_FOTA_FAIL = BluetoothLeService.ACTION_OTA_FAIL;

    private Context mContext;
    private Intent mOTAServiceIntent;
    private Intent mBluetoothLeServiceIntent;
    private static Dictionary mDeviceInformation;
    private BackendApiRequest mBackend;

    private boolean mUpdatePossible;
    private boolean mHasPostedToBackend;
    private boolean mUserConfirmation;

    private BroadcastReceiver mOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (mUpdatePossible && mUserConfirmation && (action.equals(BluetoothLeService.ACTION_OTA_SUCCESS) || action.equals(BluetoothLeService.ACTION_OTA_FAIL))){
                    mUpdatePossible = false;
                    Utils.deleteFirmwareFile();

                    Boolean success = action.equals(BluetoothLeService.ACTION_OTA_SUCCESS) ? true : false;
                    String reason = action.equals(BluetoothLeService.ACTION_OTA_SUCCESS) ? "N/A" : "Default fail message";

                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(OTA_REASON)){
                        reason = bundle.getString(OTA_REASON);
                    }
                    if (!mHasPostedToBackend) {
                        mBackend.postFotaResult(success, reason);
                        mHasPostedToBackend = true;
                    }
                    mContext.stopService(mOTAServiceIntent);
                    mContext.stopService(mBluetoothLeServiceIntent);
                }
            }
        }
    };

    /**
     * API for performing firmware updates over-the-air via BLE
     * @param context: the current context
     * @param macAddress: MAC address of the device
     */
    public FotaApi (Context context, String macAddress){
        this.macAddress = macAddress;

        mHasPostedToBackend = true;
        mContext = context;
        mOTAServiceIntent = new Intent(mContext, OTAFirmwareUpgrade.class);
        mBluetoothLeServiceIntent = new Intent(mContext, BluetoothLeService.class);
        mBackend = new BackendApiRequest(context);

        // Register receiver
        BluetoothLeService.registerBroadcastReceiver(mContext, mOTAStatusReceiver, Utils.makeOTAIntentFilter());

        // Set up dummy device data
        mDeviceInformation = Utils.getDeviceInformation(); //TODO: use real data when a BLE status request is available

        mUpdatePossible = false;
    }

    /**
     * Checks if a firmware update is possible. The following criteria applies:
     * - phone battery level (>= 50%)
     * - device battery level (>= 50%)
     * - phone connected to wifi
     * - firmware update exists
     *
     * When the check is done one of the following actions will be broadcasted:
     * - BluetoothLeService.ACTION_OTA_IS_POSSIBLE
     * - BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE
     */
    public void isFirmwareUpdatePossible(){
        if (!mUpdatePossible){
            checkFirmwareUpdatePossible();
        } else {
            broadcast(BluetoothLeService.ACTION_OTA_IS_POSSIBLE);
        }
    }

    /**
     * Update the firmware over-the-air via BLE. The device needs to be in boot mode before calling this method.
     *
     * @param userConfirmation: if the user has confirmed that they want to do a firmware update of their device
     *
     * If the firmware update finishes successfully the following action will be broadcasted:
     * - BluetoothLeService.ACTION_OTA_SUCCESS
     * If the firmware update fails the following action will be broadcasted:
     * - BluetoothLeService.ACTION_OTA_FAIL
     */
    public void doFirmwareUpdate(boolean userConfirmation){
        mUserConfirmation = userConfirmation;
        if (mUserConfirmation && mUpdatePossible){
            mHasPostedToBackend = false;
            mContext.startService(mBluetoothLeServiceIntent);
            mContext.startService(mOTAServiceIntent);
        }
        else {
            Utils.deleteFirmwareFile();
            // This won't be posted to backend, it's just a fail broadcast for the 3rd party app
            Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Firmware update was not possible or user has not confirmed update.");
        }

    }


    /********** PRIVATE HELPER FUNCTIONS **********/

    /**
     * Checks if a firmware update is possible. The following criteria applies:
     * - phone battery level (>= 50%)
     * - device battery level (>= 50%)
     * - phone connected to wifi
     * - firmware update exists
     *
     * When the check is done one of the following actions will be broadcasted:
     * - BluetoothLeService.ACTION_OTA_IS_POSSIBLE
     * - BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE
     */
    private void checkFirmwareUpdatePossible(){
        mUpdatePossible = true;

        // Check permissions
        mUpdatePossible = mUpdatePossible &&
                mContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                mContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Check wifi
        mUpdatePossible = mUpdatePossible && Utils.checkWifi(mContext);

        // Check phone battery
        BatteryManager bm = (BatteryManager)mContext.getSystemService(mContext.BATTERY_SERVICE);
        int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        mUpdatePossible = mUpdatePossible && percentage >= 50;

        // Check device battery
        mUpdatePossible = mUpdatePossible && (int)mDeviceInformation.get("batteryLevel") >= 50;

        if (!mUpdatePossible) {
            broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
        } else {
            // Get latest available firmware version and do the checks on response
            Callback callback = new Callback<List<Firmware>>() {
                @Override
                public void onResponse(Call<List<Firmware>> call, Response<List<Firmware>> response) {

                    if (!response.isSuccessful()) {
                        mUpdatePossible = false;
                        broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
                    } else {
                        List<Firmware> versions = response.body();
                        latestFirmwareVersion = versions.get(0).getFirmwareVersion();

                        // Compare firmware versions
                        if (compareVersion((String)mDeviceInformation.get("firmwareVersion"), latestFirmwareVersion) >= 0){
                            mUpdatePossible = false;
                        }

                        // Download firmware file
                        Callback<ResponseBody> callback = new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                                if (!response.isSuccessful()) {
                                    mUpdatePossible = false;
                                    broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
                                    return;
                                }
                                // Get filename from Content-Disposition
                                String contentDisposition = response.raw().header("Content-Disposition");
                                String strFilename = "filename=";
                                int startIndex = contentDisposition.indexOf(strFilename);
                                String filename = contentDisposition.substring(startIndex + strFilename.length());

                                // Create folder ROOT_DIR if it doesn't exist
                                File folder = new File(ROOT_DIR);
                                if (!folder.exists()) {
                                    folder.mkdir();
                                }
                                String firmwarePath =  ROOT_DIR + File.separator + filename;
                                boolean writtenToDisk = Utils.writeResponseBodyToDisk(response.body(), firmwarePath);

                                if (writtenToDisk) {
                                    DOWNLOADED_FIRMWARE_DIR = firmwarePath;
                                    broadcast(BluetoothLeService.ACTION_OTA_IS_POSSIBLE);
                                } else {
                                    broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
                            }
                        };

                        if (mUpdatePossible) {
                            mBackend.downloadLatestFirmwareFile(callback);
                        } else {
                            broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
                        }
                    }

                }

                @Override
                public void onFailure(Call<List<Firmware>> call, Throwable t) {
                    broadcast(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE);
                }
            };

            mBackend.getLatestFirmwareVersion(callback);
        }

    }

    /**
     * Sends a local broadcast
     * @param action
     */
    private void broadcast(String action){
        Intent intent = new Intent(action);
        BluetoothLeService.sendLocalBroadcastIntent(mContext, intent);
    }

    /**
     * Compares two version number strings
     *
     * @param version1
     * @param version2
     * @return  1 if version1 > version2
     *          0 if version1 = version2
     *          -1 if version1 < version2
     */
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
}

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

    /**
     * Actions that can be broadcasted by fotalibrary
     */
    public final static String ACTION_FOTA_SUCCESS =
            "com.innovationzed.fotalibrary.ACTION_FOTA_SUCCESS";
    public final static String ACTION_FOTA_FAIL =
            "com.innovationzed.fotalibrary.ACTION_FOTA_FAIL";
    public final static String ACTION_FOTA_COULD_NOT_BE_STARTED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_COULD_NOT_BE_STARTED";
    public final static String ACTION_FOTA_FILE_DOWNLOADED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_FILE_DOWNLOADED";
    public final static String ACTION_FOTA_FILE_DOWNLOAD_FAILED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_FILE_DOWNLOAD_FAILED";
    public final static String ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED";
    public final static String ACTION_FOTA_NO_UPDATE_EXISTS =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NO_UPDATE_EXISTS";
    public final static String ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION";
    public final static String ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE";
    public final static String ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE";
    public final static String ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED";
    public final static String ACTION_FOTA_POSSIBLE =
            "com.innovationzed.fotalibrary.ACTION_FOTA_POSSIBLE";

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
                if (mUpdatePossible && mUserConfirmation && (action.equals(ACTION_FOTA_SUCCESS) || action.equals(ACTION_FOTA_FAIL))){
                    mUpdatePossible = false;

                    Boolean success = action.equals(ACTION_FOTA_SUCCESS) ? true : false;
                    String reason = action.equals(ACTION_FOTA_SUCCESS) ? "N/A" : "Default fail message";

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
                } else if (action.equals(ACTION_FOTA_FILE_DOWNLOADED)){
                    mContext.startService(mBluetoothLeServiceIntent);
                    mContext.startService(mOTAServiceIntent);
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
     * - ACTION_FOTA_POSSIBLE
     * - ACTION_FOTA_NOT_POSSIBLE
     */
    public void isFirmwareUpdatePossible(){
        if (!mUpdatePossible){
            checkFirmwareUpdatePossible();
        } else {
            broadcast(ACTION_FOTA_POSSIBLE);
        }
    }

    /**
     * Update the firmware over-the-air via BLE
     *
     * @param userConfirmation: if the user has confirmed that they want to do a firmware update of their device
     *
     * If the firmware update finishes successfully the following action will be broadcasted:
     * - ACTION_FOTA_SUCCESS
     * If the firmware update fails the following action will be broadcasted:
     * - ACTION_FOTA_FAIL
     */
    public void doFirmwareUpdate(boolean userConfirmation){
        mUserConfirmation = userConfirmation;
        if (mUserConfirmation && mUpdatePossible){
            mHasPostedToBackend = false;
            downloadFirmwareFile();
        }
        else {
            Utils.deleteFirmwareFile();
            // This won't be posted to backend, it's just a fail broadcast for the 3rd party app
            Utils.broadcastOTAFinished(mContext, ACTION_FOTA_COULD_NOT_BE_STARTED, "Firmware update was not possible or user has not confirmed update.");
        }

    }


    /********** PRIVATE HELPER FUNCTIONS **********/

    /**
     * Download firmware file from backend
     */
    private void downloadFirmwareFile(){
        // Download firmware file
        Callback<ResponseBody> callback = new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (!response.isSuccessful()) {
                    broadcast(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
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
                    broadcast(ACTION_FOTA_FILE_DOWNLOADED);
                } else {
                    broadcast(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                broadcast(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
            }
        };

        mBackend.downloadLatestFirmwareFile(callback);
    }

    /**
     * Checks if a firmware update is possible. The following criteria applies:
     * - phone battery level (>= 50%)
     * - device battery level (>= 50%)
     * - phone connected to wifi
     * - firmware update exists
     *
     * When the check is done one of the following actions will be broadcasted:
     * - ACTION_FOTA_POSSIBLE
     * - ACTION_FOTA_NOT_POSSIBLE
     */
    private void checkFirmwareUpdatePossible(){
        mUpdatePossible = true;

        // Check permissions
        mUpdatePossible &= mContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                mContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!mUpdatePossible) {
            broadcast(ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED);
            return;
        }

        // Get latest available firmware version and do the checks on response
        Callback callback = new Callback<List<Firmware>>() {
            @Override
            public void onResponse(Call<List<Firmware>> call, Response<List<Firmware>> response) {

                if (!response.isSuccessful()) {
                    mUpdatePossible = false;
                    broadcast(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
                    return;
                }

                // Compare firmware versions
                List<Firmware> versions = response.body();
                latestFirmwareVersion = versions.get(0).getFirmwareVersion();
                mUpdatePossible &= (compareVersion((String)mDeviceInformation.get("firmwareVersion"), latestFirmwareVersion) < 0);
                if (!mUpdatePossible){
                    broadcast(ACTION_FOTA_NO_UPDATE_EXISTS);
                    return;
                }

                // Check wifi
                mUpdatePossible &= Utils.checkWifi(mContext);
                if (!mUpdatePossible) {
                    broadcast(ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION);
                    return;
                }

                // Check phone battery
                BatteryManager bm = (BatteryManager)mContext.getSystemService(mContext.BATTERY_SERVICE);
                int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                mUpdatePossible &= percentage >= 50;
                if (!mUpdatePossible) {
                    broadcast(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE);
                    return;
                }

                // Check device battery
                mUpdatePossible &= (int)mDeviceInformation.get("batteryLevel") >= 50;
                if (!mUpdatePossible) {
                    broadcast(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE);
                    return;
                }
            }

            @Override
            public void onFailure(Call<List<Firmware>> call, Throwable t) {
                broadcast(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
            }
        };

        mBackend.getLatestFirmwareVersion(callback);
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

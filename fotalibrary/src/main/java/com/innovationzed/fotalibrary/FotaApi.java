package com.innovationzed.fotalibrary;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.innovationzed.fotalibrary.BLEConnectionServices.BatteryInformationService;
import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.BLEConnectionServices.DeviceInformationService;
import com.innovationzed.fotalibrary.BackendCommunication.BackendApiRequest;
import com.innovationzed.fotalibrary.BackendCommunication.Firmware;
import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.FotaBroadcastReceiver;
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.OTAFirmwareUpdate.OTAFirmwareUpgrade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_CONNECTED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_DISCONNECTED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.STATE_CONNECTED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_BLE_CONNECTION_FAILED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_COULD_NOT_BE_STARTED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_BATTERY_READ;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_INFO_READ;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FAIL;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FILE_DOWNLOADED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FILE_DOWNLOAD_FAILED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_POSSIBLE;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_SUCCESS;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_TIMEOUT;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.DEVICE_BOOT_NAME;
import static com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase.UUID_IMMEDIATE_ALERT_SERVICE;
import static com.innovationzed.fotalibrary.CommonUtils.Utils.IS_IN_BOOT_MODE;
import static com.innovationzed.fotalibrary.CommonUtils.Utils.OTA_REASON;

public class FotaApi {

    public static final String ROOT_DIR = "/storage/emulated/0/Download";

    public static String macAddress;
    public static String latestFirmwareVersion;
    public static String downloadedFirmwareDir;

    private static boolean mUpdatePossible = false;
    private static boolean mShouldPostToBackend = false;
    private static boolean isInitialized = false;
    private static boolean mIsFotaInProgress = false;
    private static boolean mHasCheckedUpdatePossible = false;
    private static boolean mShouldTryToBondInBootMode = false;

    private static Intent mOTAServiceIntent;
    private static Dictionary mDeviceInformation;

    private final Context mContext;
    private final BackendApiRequest mBackend;

    private DeviceInformationService mDeviceInformationService;
    private BatteryInformationService mBatteryInformationService;

    /**
     * Variables for timeout handling
     */
    private static Handler mTimeoutHandler;
    private static final int CHECK_FIRMWARE_UPDATE_TIME_LIMIT = 30000;
    private static final int FOTA_PROGRESS_TIME_LIMIT = 90000; // Longer since it involves user interaction

    private Runnable mFotaProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mIsFotaInProgress) {
                mShouldPostToBackend = true;
                BluetoothLeService.unregisterBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver);
                BluetoothLeService.unregisterBroadcastReceiver(mContext, mBootModeReceiver);
                try {
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_TIMEOUT, "FOTA timed out.", true);
                } catch (Exception e) {
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Error during check for FOTA timeout.", true);
                }
            }
        }
    };

    private Runnable mCheckFirmwareRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mHasCheckedUpdatePossible) {
                mShouldPostToBackend = true;
                if (null != mDeviceInformationService) {
                    mDeviceInformationService.stop(mContext);
                    mDeviceInformationService = null;
                }
                if (null != mBatteryInformationService) {
                    mBatteryInformationService.stopReadingBatteryInfo(mContext);
                    mBatteryInformationService = null;
                }
                BluetoothLeService.unregisterBroadcastReceiver(mContext, mAppModeReceiver);
                try {
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Check for firmware update timed out.");
                } catch (Exception e) {
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Error during check for firmware update timeout.");
                }
            }
        }
    };

    /**
     * BroadcastReceiver that handles the BLE connection flow and the FOTA flow
     */
    private FotaBroadcastReceiver mAppModeReceiver = new FotaBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_GATT_CONNECTED)) {
                    BluetoothLeService.discoverServices();
                } else if (action.equals(ACTION_GATT_DISCONNECTED)) {
                    broadcastTo3rdPartyApp(ACTION_FOTA_BLE_CONNECTION_FAILED);
                } else if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                    BluetoothGattService service = BluetoothLeService.getService(UUIDDatabase.UUID_DEVICE_INFORMATION_SERVICE);
                    if (null != service) {
                        if (null == mDeviceInformationService) {
                            mDeviceInformationService = DeviceInformationService.create(service);
                            mDeviceInformationService.startReadingDeviceInfo(mContext);
                        }
                    } else {
                        broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ);
                    }
                } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL)) {
                    broadcastTo3rdPartyApp(ACTION_FOTA_BLE_CONNECTION_FAILED);
                } else if (action.equals(ACTION_FOTA_DEVICE_INFO_READ)) {
                    if (null != mDeviceInformationService) {
                        mDeviceInformationService.stop(mContext);
                        mDeviceInformationService = null;
                    }
                    mDeviceInformation = DeviceInformationService.getDeviceInformation();

                    // TODO: Remove this hardcoded device hw rev when app returns real value
                    if (mDeviceInformation.get("HardwareRevision") == "not found")
                    {
                        mDeviceInformation.put("HardwareRevision", "V5");
                    }

                    // Read battery level
                    BluetoothGattService service = BluetoothLeService.getService(UUIDDatabase.UUID_BATTERY_SERVICE);
                    if (null != service) {
                        if (null == mBatteryInformationService) {
                            mBatteryInformationService = BatteryInformationService.create(service);
                            mBatteryInformationService.startReadingBatteryInfo(mContext);
                        }
                    } else {
                        broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ);
                    }
                } else if (action.equals(ACTION_FOTA_DEVICE_BATTERY_READ)) {
                    if (null != mBatteryInformationService) {
                        mBatteryInformationService.stopReadingBatteryInfo(mContext);
                        mBatteryInformationService = null;
                    }

                    int battery = BatteryInformationService.getBatteryLevel();
                    mDeviceInformation.put("BatteryLevel", battery);
                    checkRemainingPrerequisites();
                }
            }
        }
    };

    /**
     * BroadcastReceiver that handles the BLE connection flow and the FOTA flow
     */
    private FotaBroadcastReceiver mDiscoverImmediateAlertServiceReceiver = new FotaBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_FOTA_FILE_DOWNLOADED)) {
                    // Timeout check
                    mTimeoutHandler.postDelayed(mFotaProgressRunnable, FOTA_PROGRESS_TIME_LIMIT);

                    // Check if device is connected and paired
                    BluetoothDevice device = BluetoothLeService.getRemoteDevice(FotaApi.macAddress);
                    int connectionState = BluetoothLeService.getConnectionState(device);
                    if (device.getBondState() == BOND_BONDED && connectionState == STATE_CONNECTED){
                        if (!BluetoothLeService.discoverServices()){
                            Utils.broadcastOTAFinished(mContext, ACTION_FOTA_COULD_NOT_BE_STARTED, "Could not start service discovery");
                        }
                    } else {
                        // Broadcast message saying that FOTA could not be started because the device isn't bonded and connected
                        Utils.broadcastOTAFinished(mContext, ACTION_FOTA_COULD_NOT_BE_STARTED, "Device is not bonded and connected");
                    }
                } else if (action.equals(ACTION_FOTA_FILE_DOWNLOAD_FAILED)) {
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver);
                    mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                    resetAllVariables();
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "File download failed.");
                } else if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver);
                    jumpToBoot();
                } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL)) {
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver);
                    broadcastTo3rdPartyApp(ACTION_FOTA_COULD_NOT_BE_STARTED);
                }
            }

        }
    };

    /**
     * BroadcastReceiver for handling when the device is found in boot mode
     */
    private FotaBroadcastReceiver mBootModeReceiver = new FotaBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND)){
                    // Look for the device in boot mode and unpair when it's found
                    Bundle extras = intent.getExtras();
                    BluetoothDevice device = extras.getParcelable(BluetoothDevice.EXTRA_DEVICE);

                    if (device.getAddress().equals(FotaApi.macAddress) && device.getName().equals(DEVICE_BOOT_NAME)) {
                        BluetoothLeService.stopDeviceScan();
                        mShouldTryToBondInBootMode = true;
                        BluetoothLeService.unpairDevice(device);
                    }
                } else if (action.equals(ACTION_BOND_STATE_CHANGED)) {
                    BluetoothDevice device = BluetoothLeService.getRemoteDevice();
                    if (device.getBondState() == BOND_NONE && mShouldTryToBondInBootMode){
                        // Device in boot mode has unpaired, pair again to be able to discover OTA service
                        mShouldTryToBondInBootMode = false;
                        device.createBond();
                    } else if (device.getBondState() == BOND_BONDED) {
                        BluetoothLeService.connect(FotaApi.macAddress, mContext);
                    }
                } else if (action.equals(ACTION_GATT_CONNECTED)) {
                    if (!BluetoothLeService.discoverServices()){
                        Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Service discovery could not be started.", true);
                    }
                } else if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                    if (BluetoothLeService.getService(UUIDDatabase.UUID_OTA_UPDATE_SERVICE) != null && !mIsFotaInProgress){
                        mIsFotaInProgress = true;
                        BluetoothLeService.unregisterBroadcastReceiver(mContext, mBootModeReceiver);
                        mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                        mContext.startService(mOTAServiceIntent);
                    } else {
                        Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Could not discover OTA service.", true);
                    }
                } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL)) {
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Could not discover services", true);
                }
            }
        }
    };

    /**
     * BroadcastReceiver for handling FOTA actions
     */
    private FotaBroadcastReceiver mFOTAReceiver = new FotaBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_FOTA_SUCCESS) || action.equals(ACTION_FOTA_FAIL) || action.equals(ACTION_FOTA_TIMEOUT)) {
                    boolean success = action.equals(ACTION_FOTA_SUCCESS);
                    String reason = action.equals(ACTION_FOTA_SUCCESS) ? "N/A" : "Default fail message";
                    boolean isInBootMode = false;

                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(OTA_REASON)) {
                        reason = bundle.getString(OTA_REASON);
                    }

                    if (bundle.containsKey(IS_IN_BOOT_MODE)) {
                        isInBootMode = bundle.getBoolean(IS_IN_BOOT_MODE);
                    }

                    if (mShouldPostToBackend) {
                        if (mDeviceInformation == null) {
                            mDeviceInformation = DeviceInformationService.getDeviceInformation();
                        }
                        mBackend.postFotaResult(success, reason, mDeviceInformation);
                        mShouldPostToBackend = false;
                    }

                    if (isInBootMode) {
                        // Unpair in case it's in a state where it's still paired
                        BluetoothLeService.unpairDevice(BluetoothLeService.getRemoteDevice(macAddress));
                    }

                    mContext.stopService(mOTAServiceIntent);
                    mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mFOTAReceiver);
                    resetAllVariables();
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
        mContext = context;
        mBackend = new BackendApiRequest(context);

        if (!isInitialized) {
            FotaApi.macAddress = macAddress;
            mOTAServiceIntent = new Intent(mContext, OTAFirmwareUpgrade.class);
            mTimeoutHandler = new Handler();

            // Start BLE service
            mContext.startService(new Intent(mContext, BluetoothLeService.class));

            // Initialize variables
            resetAllVariables();
            isInitialized = true;
        }
    }

    /**
     * Change device. This needs to be done before isFirmwareUpdatePossible and doFirmwareUpdate is called
     * @param macAddress
     */
    public void changeDevice(String macAddress){
        this.macAddress = macAddress;
        resetAllVariables();
    }

    /**
     * Checks if a firmware update is possible. The following criteria applies:
     * - app permissions are granted
     * - phone battery level (>= 50%)
     * - device battery level (>= 50%)
     * - phone connected to wifi
     * - firmware update exists
     */
    public void isFirmwareUpdatePossible(){
        // Register receivers
        BluetoothLeService.registerBroadcastReceiver(mContext, mFOTAReceiver, Utils.makeFotaApiIntentFilter());
        BluetoothLeService.registerBroadcastReceiver(mContext, mAppModeReceiver, Utils.makeAppModeIntentFilter());
        checkFirmwareUpdatePossible();
    }

    /**
     * Update the firmware over-the-air via BLE
     *
     * @param userConfirmation: if the user has confirmed that they want to do a firmware update of their device
     */
    public void doFirmwareUpdate(boolean userConfirmation){
        if (userConfirmation && mUpdatePossible){
            BluetoothLeService.registerBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver, Utils.makeImmediateAlertIntentFilter());
            mShouldPostToBackend = true;

            downloadFirmwareFile();
        }
        else {
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
                    broadcastTo3rdPartyApp(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Response from downloadLatestFirmwareFile was not successful.");
                    return;
                }

                if (response.code() == 204) {
                    // 204 No Content, no FW file returned for the specified HW rev
                    broadcastTo3rdPartyApp(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "No firmware file exists for current hardware.");
                    return;
                }

                // Get filename from Content-Disposition
                String contentDisposition = response.raw().header("Content-Disposition");
                String strFilename = "filename=";
                int startIndex = contentDisposition.indexOf(strFilename);
                String filename = contentDisposition.substring(startIndex + strFilename.length());

                boolean isCyacd2File = filename != null && Utils.isCyacd2File(filename);
                Utils.setBooleanSharedPreference(mContext, Constants.PREF_IS_CYACD2_FILE, isCyacd2File);

                if (isCyacd2File) {
                    String versionNumber = filename.substring(0, filename.length() - ".cyacd2".length());
                    if (!versionNumber.equalsIgnoreCase(latestFirmwareVersion)) {
                        broadcastTo3rdPartyApp(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                        Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "The version number of the downloaded file is not the same as the latest version number downloaded before.");
                        return;
                    }
                }

                // Create folder ROOT_DIR if it doesn't exist
                File folder = new File(ROOT_DIR);
                if (!folder.exists()) {
                    folder.mkdir();
                }
                String firmwarePath =  ROOT_DIR + File.separator + filename;
                boolean writtenToDisk = writeResponseBodyToDisk(response.body(), firmwarePath);

                if (writtenToDisk) {
                    downloadedFirmwareDir = firmwarePath;
                    BluetoothLeService.sendLocalBroadcastIntent(mContext, new Intent(ACTION_FOTA_FILE_DOWNLOADED));
                } else {
                    broadcastTo3rdPartyApp(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Firmware file could not be written to disk on phone.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                broadcastTo3rdPartyApp(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "Call to downloadLatestFirmwareFile failed.");
            }
        };

        mBackend.downloadLatestFirmwareFile(callback, mDeviceInformation);
    }

    /**
     * Checks if a firmware update is possible.
     */
    private void checkFirmwareUpdatePossible(){
        // Timeout check
        mTimeoutHandler.postDelayed(mCheckFirmwareRunnable, CHECK_FIRMWARE_UPDATE_TIME_LIMIT);

        resetAllVariables();

        // Check permissions
        boolean permissions = mContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                mContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!permissions) {
            broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED);
            return;
        }

        // Check wifi and network connection
        boolean networkConnection = Utils.checkWifi(mContext) && Utils.checkNetwork(mContext);
        if (!networkConnection) {
            broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION);
            return;
        }

        // Check phone battery
        BatteryManager bm = (BatteryManager)mContext.getSystemService(mContext.BATTERY_SERVICE);
        int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        boolean enoughBatteryPhone = percentage >= 50;
        if (!enoughBatteryPhone) {
            broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE);
            return;
        }

        // Connect to device in order to read device information
        BluetoothDevice device = BluetoothLeService.getRemoteDevice(FotaApi.macAddress);
        if (device.getBondState() == BOND_BONDED){
            if (!BluetoothLeService.connect(FotaApi.macAddress, mContext)){
                broadcastTo3rdPartyApp(ACTION_FOTA_BLE_CONNECTION_FAILED);
            }
        } else {
            broadcastTo3rdPartyApp(ACTION_FOTA_BLE_CONNECTION_FAILED);
        }
    }

    /**
     * Checks the remaining FOTA pre-reqs after reading info from the device.
     */
    private void checkRemainingPrerequisites(){
        // Get latest available firmware version and do the checks on response
        Callback callback = new Callback<Firmware>() {
            @Override
            public void onResponse(Call<Firmware> call, Response<Firmware> response) {

                if (!response.isSuccessful()) {
                    broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
                    return;
                }

                if (response.code() == 204) {
                    // 204 No Content, no FW file for the specified HW rev
                    broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS);
                    return;
                }

                // Compare firmware versions
                Firmware fwResponse = response.body();
                latestFirmwareVersion = fwResponse.getFirmwareVersion();
                boolean updateExists = (Utils.compareVersion((String)mDeviceInformation.get("FirmwareRevision"), latestFirmwareVersion) < 0);
                if (!updateExists){
                    broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS);
                    return;
                }

                // Check device battery
                boolean enoughBatteryDevice = (int)mDeviceInformation.get("BatteryLevel") >= 50;
                if (!enoughBatteryDevice) {
                    broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE);
                    return;
                }

                // If FOTA is possible, broadcast ACTION_FOTA_POSSIBLE
                mUpdatePossible = true;
                broadcastTo3rdPartyApp(ACTION_FOTA_POSSIBLE);
            }

            @Override
            public void onFailure(Call<Firmware> call, Throwable t) {
                broadcastTo3rdPartyApp(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
            }
        };

        mBackend.getLatestFirmwareVersion(callback, mDeviceInformation);
    }

    /**
     * Sends a local broadcast and stops listening in receiver
     * @param action
     */
    private void broadcastTo3rdPartyApp(String action){
        BluetoothLeService.unregisterBroadcastReceiver(mContext, mAppModeReceiver);
        mHasCheckedUpdatePossible = true;
        mTimeoutHandler.removeCallbacks(mCheckFirmwareRunnable);
        Intent intent = new Intent(action);
        BluetoothLeService.sendLocalBroadcastIntent(mContext, intent);
    }

    /**
     * Resets all variables to starting values
     */
    private void resetAllVariables(){
        mUpdatePossible = false;
        mShouldPostToBackend = false;
        mIsFotaInProgress = false;
        mHasCheckedUpdatePossible = false;
        mShouldTryToBondInBootMode = false;
    }

    /**
     * Jump to boot mode by sending an immediate alert
     */
    private void jumpToBoot(){
        // Send immediate alert to jump to boot
        BluetoothGattService service = BluetoothLeService.getService(UUID_IMMEDIATE_ALERT_SERVICE);
        if(service != null){
            byte[] convertedBytes = Utils.convertingToByteArray("0x01");
            BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(UUIDDatabase.UUID_ALERT_LEVEL);
            if (gattCharacteristic != null) {
                BluetoothLeService.writeCharacteristicNoResponse(gattCharacteristic, convertedBytes);

                BluetoothLeService.registerBroadcastReceiver(mContext, mBootModeReceiver, Utils.makeBootModeIntentFilter());
                BluetoothLeService.startDeviceScan();
            } else {
                Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "FOTA failed during jump to boot. Alert level characteristic not found.");
            }

        } else {
            Utils.broadcastOTAFinished(mContext, ACTION_FOTA_FAIL, "FOTA failed during jump to boot. Immediate alert service not found.");
        }
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String path) {
        try {
            File latestFirmwareFile = new File(path);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(latestFirmwareFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d("File Download: " , fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}

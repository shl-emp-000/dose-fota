package com.innovationzed.fotalibrary;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
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
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.OTAFirmwareUpdate.OTAFirmwareUpgrade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.List;

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
                try {
                    OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "FOTA timed out.");
                } catch (Exception e) {
                    OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Error during check for FOTA timeout.");
                }
            }
        }
    };

    private Runnable mCheckFirmwareRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mHasCheckedUpdatePossible) {
                mShouldPostToBackend = true;
                try {
                    OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Check for firmware update timed out.");
                } catch (Exception e) {
                    OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Error during check for firmware update timeout.");
                }
            }
        }
    };

    /**
     * BroadcastReceiver that handles the BLE connection flow and the FOTA flow
     */
    private BroadcastReceiver mAppModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_GATT_CONNECTED)) {
                    BluetoothLeService.discoverServices();
                } else if (action.equals(ACTION_GATT_DISCONNECTED)){
                    broadcastFirmwareCheck(ACTION_FOTA_BLE_CONNECTION_FAILED);
                } else if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                    BluetoothGattService service = BluetoothLeService.getService(UUIDDatabase.UUID_DEVICE_INFORMATION_SERVICE);
                    if (service != null) {
                        mDeviceInformationService = DeviceInformationService.create(service);
                        mDeviceInformationService.startReadingDeviceInfo(mContext);
                    } else {
                        broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ);
                    }
                } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL)) {
                    broadcastFirmwareCheck(ACTION_FOTA_BLE_CONNECTION_FAILED);
                } else if (action.equals(ACTION_FOTA_DEVICE_INFO_READ)) {
                    mDeviceInformationService.stop(mContext);
                    mDeviceInformation = DeviceInformationService.getDeviceInformation();

                    // Read battery level
                    BluetoothGattService service = BluetoothLeService.getService(UUIDDatabase.UUID_BATTERY_SERVICE);
                    if (service != null) {
                        mBatteryInformationService = BatteryInformationService.create(service);
                        mBatteryInformationService.startReadingBatteryInfo(mContext);
                    } else {
                        broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ);
                    }
                } else if (action.equals(ACTION_FOTA_DEVICE_BATTERY_READ)) {
                    mBatteryInformationService.stopReadingBatteryInfo(mContext);
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
    private BroadcastReceiver mDiscoverImmediateAlertServiceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver);
                    jumpToBoot();
                } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL)) {
                    // TODO: retry x times
                    broadcastFirmwareCheck(ACTION_FOTA_COULD_NOT_BE_STARTED);
                }
            }

        }
    };

    /**
     * BroadcastReceiver for handling when the device is found in boot mode
     */
    private BroadcastReceiver mBootModeReceiver = new BroadcastReceiver() {
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
                        OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_FAIL, "Service discovery could not be started.");
                    }
                } else if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                    if (BluetoothLeService.getService(UUIDDatabase.UUID_OTA_UPDATE_SERVICE) != null){
                        downloadFirmwareFile();
                    } else {
                        OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_FAIL, "Could not discover OTA service.");
                    }
                } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL)) {
                    OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_FAIL, "Could not discover services"); // TODO: fix actions?
                } else if (action.equals(ACTION_FOTA_FILE_DOWNLOADED) && !mIsFotaInProgress) {
                    mIsFotaInProgress = true;
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mBootModeReceiver);
                    mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                    mContext.startService(mOTAServiceIntent);
                } else if (action.equals(ACTION_FOTA_FILE_DOWNLOAD_FAILED)) {
                    BluetoothLeService.unregisterBroadcastReceiver(mContext, mBootModeReceiver);
                    mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                    resetAllVariables();
                }
            }
        }
    };

    /**
     * BroadcastReceiver for handling FOTA actions
     */
    private BroadcastReceiver mFOTAReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_FOTA_SUCCESS) || action.equals(ACTION_FOTA_FAIL) || action.equals(ACTION_FOTA_TIMEOUT)) {
                    boolean success = action.equals(ACTION_FOTA_SUCCESS);
                    String reason = action.equals(ACTION_FOTA_SUCCESS) ? "N/A" : "Default fail message";

                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(OTA_REASON)) {
                        reason = bundle.getString(OTA_REASON);
                    }

                    if (mShouldPostToBackend) {
                        if (mDeviceInformation == null) {
                            mDeviceInformation = DeviceInformationService.getDeviceInformation();
                        }
                        mBackend.postFotaResult(success, reason, mDeviceInformation);
                        mShouldPostToBackend = false;
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

            // Register receiver
            BluetoothLeService.registerBroadcastReceiver(mContext, mFOTAReceiver, Utils.makeFotaApiIntentFilter());

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
        // Register receiver
        BluetoothLeService.registerBroadcastReceiver(mContext, mAppModeReceiver, Utils.makeAppModeIntentFilter());

        checkFirmwareUpdatePossible();
    }

    /**
     * Update the firmware over-the-air via BLE
     *
     * @param userConfirmation: if the user has confirmed that they want to do a firmware update of their device
     */
    public void doFirmwareUpdate(boolean userConfirmation){
        BluetoothLeService.registerBroadcastReceiver(mContext, mDiscoverImmediateAlertServiceReceiver, Utils.makeGattUpdateIntentFilter());
        if (userConfirmation && mUpdatePossible){
            mShouldPostToBackend = true;

            // Timeout check
            mTimeoutHandler.postDelayed(mFotaProgressRunnable, FOTA_PROGRESS_TIME_LIMIT);

            // Check if device is connected and paired
            BluetoothDevice device = BluetoothLeService.getRemoteDevice(FotaApi.macAddress);
            int connectionState = BluetoothLeService.getConnectionState(device);
            if (device.getBondState() == BOND_BONDED && connectionState == STATE_CONNECTED){
                if (!BluetoothLeService.discoverServices()){
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_COULD_NOT_BE_STARTED, "Initial service discovery failed"); //TODO: change action?
                }
            } else {
                // Broadcast message saying that FOTA could not be started because the device isn't bonded and connected
                Utils.broadcastOTAFinished(mContext, ACTION_FOTA_COULD_NOT_BE_STARTED, "Device is not bonded and connected");
            }
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
                    broadcastFirmwareCheck(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
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
                boolean writtenToDisk = writeResponseBodyToDisk(response.body(), firmwarePath);

                if (writtenToDisk) {
                    downloadedFirmwareDir = firmwarePath;
                    broadcastFOTAProgress(ACTION_FOTA_FILE_DOWNLOADED);
                } else {
                    broadcastFirmwareCheck(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                broadcastFirmwareCheck(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
            }
        };

        mBackend.downloadLatestFirmwareFile(callback);
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
            broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED);
            return;
        }

        // Connect to device in order to read device information
        BluetoothDevice device = BluetoothLeService.getRemoteDevice(FotaApi.macAddress);
        int connectionState = BluetoothLeService.getConnectionState(device);
        if (device.getBondState() == BOND_BONDED && connectionState == STATE_CONNECTED){
            if (!BluetoothLeService.connect(FotaApi.macAddress, mContext)){
                broadcastFirmwareCheck(ACTION_FOTA_BLE_CONNECTION_FAILED);
            }
        } else {
            broadcastFirmwareCheck(ACTION_FOTA_BLE_CONNECTION_FAILED);;
        }
    }

    /**
     * Checks the remaining FOTA pre-reqs after reading info from the device.
     */
    private void checkRemainingPrerequisites(){
        // Get latest available firmware version and do the checks on response
        Callback callback = new Callback<List<Firmware>>() {
            @Override
            public void onResponse(Call<List<Firmware>> call, Response<List<Firmware>> response) {

                if (!response.isSuccessful()) {
                    broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
                    return;
                }

                // Compare firmware versions
                List<Firmware> versions = response.body();
                latestFirmwareVersion = versions.get(0).getFirmwareVersion();
                boolean updateExists = (Utils.compareVersion((String)mDeviceInformation.get("FirmwareRevision"), latestFirmwareVersion) < 0);
                if (!updateExists){
                    broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS);
                    return;
                }

                // Check wifi
                boolean networkConnection = Utils.checkWifi(mContext) && Utils.checkNetwork(mContext);
                if (!networkConnection) {
                    broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION);
                    return;
                }

                // Check phone battery
                BatteryManager bm = (BatteryManager)mContext.getSystemService(mContext.BATTERY_SERVICE);
                int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                boolean enoughBatteryPhone = percentage >= 50;
                if (!enoughBatteryPhone) {
                    broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE);
                    return;
                }

                // Check device battery
                boolean enoughBatteryDevice = (int)mDeviceInformation.get("BatteryLevel") >= 50;
                if (!enoughBatteryDevice) {
                    broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE);
                    return;
                }

                // If FOTA is possible, broadcast ACTION_FOTA_POSSIBLE
                mUpdatePossible = true;
                broadcastFirmwareCheck(ACTION_FOTA_POSSIBLE);
            }

            @Override
            public void onFailure(Call<List<Firmware>> call, Throwable t) {
                broadcastFirmwareCheck(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
            }
        };

        mBackend.getLatestFirmwareVersion(callback);
    }

    /**
     * Sends a local broadcast
     * @param action
     */
    private void broadcastFOTAProgress(String action){
        Intent intent = new Intent(action);
        BluetoothLeService.sendLocalBroadcastIntent(mContext, intent);
    }

    /**
     * Sends a local broadcast and stops listening in receiver
     * @param action
     */
    private void broadcastFirmwareCheck(String action){
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
                OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_FAIL, "FOTA failed during jump to boot. Alert level characteristic not found.");
            }

        } else {
            OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_FAIL, "FOTA failed during jump to boot. Immediate alert service not found.");
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

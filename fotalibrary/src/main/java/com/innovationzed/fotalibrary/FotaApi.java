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
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_CONNECTED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_CONNECTING;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_DISCONNECTED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_DISCONNECTING;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.STATE_CONNECTED;
import static com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase.UUID_IMMEDIATE_ALERT_SERVICE;
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
    public final static String ACTION_FOTA_TIMEOUT =
            "com.innovationzed.fotalibrary.ACTION_FOTA_TIMEOUT";
    public final static String ACTION_FOTA_COULD_NOT_BE_STARTED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_COULD_NOT_BE_STARTED";
    public final static String ACTION_FOTA_BLE_CONNECTION_FAILED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_BLE_CONNECTION_FAILED";
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
    public final static String ACTION_DEVICE_INFO_READ =
            "com.innovationzed.fotalibrary.ACTION_DEVICE_INFO_READ";

    private static Context mContext;
    private static Intent mOTAServiceIntent;
    private static Intent mBluetoothLeServiceIntent;
    private static Dictionary mDeviceInformation;
    private static BackendApiRequest mBackend;
    private static DeviceInformationService mDeviceInformationService;

    private static boolean mUpdatePossible = false;
    private static boolean mShouldPostToBackend = false;
    private static boolean mUserConfirmation = false;

    private static boolean isInitialized = false;
    private static boolean mShouldListen = false;
    private static boolean mHasStartedBonding = false;
    private static boolean mIsFotaInProgress = false;
    private static boolean mHasBonded = false;
    private static boolean mServiceDiscoveryStarted = false;
    private static boolean mFileDownloadStarted = false;

    private static final int STATE_READ_DEVICE_INFO = 0;
    private static final int STATE_JUMP_TO_BOOT = 1;
    private static final int STATE_START_FOTA = 2;
    private static int mNextState = STATE_READ_DEVICE_INFO;

    private Handler mTimeoutHandler;
    private static final int CHECK_FIRMWARE_UPDATE_TIME_LIMIT = 30000;
    private static final int FOTA_PROGRESS_TIME_LIMIT = 90000; // Longer since it involves user interaction

    private Runnable mFotaProgressRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "FOTA timed out.");
            } catch (Exception e) {
                OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Error during check for FOTA timeout.");
            }
        }
    };

    private Runnable mCheckFirmwareRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Check for firmware update timed out.");
            } catch (Exception e) {
                OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_TIMEOUT, "Error during check for firmware update timeout.");
            }
        }
    };

    private BroadcastReceiver mOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (mShouldListen) {
                    if (action.equals(ACTION_BOND_STATE_CHANGED)) {
                        if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_BONDING) {
                            // Do nothing, waiting for bonding to complete
                            mHasStartedBonding = true;
                        } else if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_NONE && !mHasStartedBonding) {
                            BluetoothLeService.getRemoteDevice().createBond();
                        } else if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_BONDED && mHasStartedBonding) {
                            mHasBonded = true;
                            if (BluetoothLeService.getConnectionState() == STATE_CONNECTED && !mServiceDiscoveryStarted) {
                                mServiceDiscoveryStarted = BluetoothLeService.discoverServices();
                            } else { // connect again if it is paired but not currently connected
                                BluetoothLeService.connect(FotaApi.macAddress, context);
                            }
                        }
                    } else if (action.equals(ACTION_GATT_CONNECTED)) {
                        if (mHasBonded && !mServiceDiscoveryStarted) {
                            // if it is paired and connected we should discover services
                            mServiceDiscoveryStarted = BluetoothLeService.discoverServices();
                        } else {
                            // this is where it goes after initial connect
                            if (BluetoothLeService.getRemoteDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                                mServiceDiscoveryStarted = BluetoothLeService.discoverServices();
                            } else {
                                BluetoothLeService.getRemoteDevice().createBond();
                            }
                        }
                    } else if (action.equals(ACTION_GATT_CONNECTING)) {
                        // wait for connection...
                    } else if (action.equals(ACTION_GATT_DISCONNECTING)) {
                        // wait for disconnection...
                    } else if (action.equals(ACTION_GATT_DISCONNECTED)) {
                        // do nothing for now, it can get disconnected when re-pairing
                    } else if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                        // services has been discovered and it has been paired, jump to boot or do fota
                        if (mNextState == STATE_READ_DEVICE_INFO) {
                            BluetoothGattService service = BluetoothLeService.getService(UUIDDatabase.UUID_DEVICE_INFORMATION_SERVICE);
                            mDeviceInformationService = DeviceInformationService.create(service);
                            mDeviceInformationService.startReadingDeviceInfo(mContext);
                        } else if (mNextState == STATE_JUMP_TO_BOOT) {
                            jumpToBoot();
                        } else if (mNextState == STATE_START_FOTA && !mFileDownloadStarted) {
                            downloadFirmwareFile();
                            mFileDownloadStarted = true;
                        }
                    } else if (action.equals(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL) && mHasBonded) {
                        broadcastFirmwareCheck(ACTION_FOTA_BLE_CONNECTION_FAILED);
                    } else if (mUpdatePossible && mUserConfirmation && (action.equals(ACTION_FOTA_SUCCESS) || action.equals(ACTION_FOTA_FAIL))) {
                        Boolean success = action.equals(ACTION_FOTA_SUCCESS) ? true : false;
                        String reason = action.equals(ACTION_FOTA_SUCCESS) ? "N/A" : "Default fail message";

                        Bundle bundle = intent.getExtras();
                        if (bundle.containsKey(OTA_REASON)) {
                            reason = bundle.getString(OTA_REASON);
                        }
                        if (mShouldPostToBackend) {
                            mBackend.postFotaResult(success, reason, mDeviceInformation);
                            mShouldPostToBackend = false;
                        }
                        mContext.stopService(mOTAServiceIntent);
                        mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                        resetAllVariables();
                    } else if (action.equals(ACTION_FOTA_FILE_DOWNLOADED) && !mIsFotaInProgress) {
                        mIsFotaInProgress = true;
                        mTimeoutHandler.removeCallbacks(mFotaProgressRunnable);
                        mContext.startService(mOTAServiceIntent);
                    } else if (action.equals(ACTION_DEVICE_INFO_READ)) {
                        mDeviceInformationService.stop(mContext);
                        mDeviceInformation = mDeviceInformationService.getDeviceInformation();
                        mDeviceInformation.put("BatteryLevel", 75);
                        checkRemainingPrerequisites();
                    }
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
        if (!isInitialized) {
            this.macAddress = macAddress;
            mContext = context;
            mOTAServiceIntent = new Intent(mContext, OTAFirmwareUpgrade.class);
            mBluetoothLeServiceIntent = new Intent(mContext, BluetoothLeService.class);
            mBackend = new BackendApiRequest(context);
            mTimeoutHandler = new Handler();

            // Register receiver
            BluetoothLeService.registerBroadcastReceiver(mContext, mOTAStatusReceiver, Utils.makeOTAIntentFilter());

            // Start BLE service
            mContext.startService(mBluetoothLeServiceIntent);

            // Initialize variables
            resetAllVariables();
            isInitialized = true;
        }
    }

    /**
     * Change device. This needs to be done before isFirmwareUpdatePossible and doFirmwareUpdate is called
     */
    public void changeDevice(String macAddress){
        this.macAddress = macAddress;
        resetAllVariables();
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
            broadcastFirmwareCheck(ACTION_FOTA_POSSIBLE);
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
            mShouldPostToBackend = true;
            mShouldListen = true;
            resetBLEConnectionVariables();

            // Timeout check
            mTimeoutHandler.postDelayed(mFotaProgressRunnable, FOTA_PROGRESS_TIME_LIMIT);

            // Check if device is connected and paired
            BluetoothDevice device = BluetoothLeService.getRemoteDevice(FotaApi.macAddress);
            int connectionState = BluetoothLeService.getConnectionState(device);
            if (device.getBondState() == BOND_BONDED && connectionState == STATE_CONNECTED){
                if (!BluetoothLeService.connect(FotaApi.macAddress, mContext)){
                    Utils.broadcastOTAFinished(mContext, ACTION_FOTA_COULD_NOT_BE_STARTED, "Initial connection to device failed");
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
                    DOWNLOADED_FIRMWARE_DIR = firmwarePath;
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
        // Timeout check
        mTimeoutHandler.postDelayed(mCheckFirmwareRunnable, CHECK_FIRMWARE_UPDATE_TIME_LIMIT);

        resetAllVariables();
        mShouldListen = true;

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
                Utils.broadcastOTAFinished(mContext, ACTION_FOTA_BLE_CONNECTION_FAILED, "Initial connection to device failed");
            }
        } else {
            // Broadcast message saying that FOTA could not be started because the device isn't bonded and connected
            Utils.broadcastOTAFinished(mContext, ACTION_FOTA_BLE_CONNECTION_FAILED, "Device is not bonded and connected");
        }
    }

    /**
     *
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
                    broadcastFirmwareCheck(ACTION_FOTA_NO_UPDATE_EXISTS);
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
                mNextState = STATE_JUMP_TO_BOOT;
                resetBLEConnectionVariables();
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
        mShouldListen = false;
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
        mUserConfirmation = false;
        mIsFotaInProgress = false;
        mShouldListen = false;
        mFileDownloadStarted = false;
        resetBLEConnectionVariables();

        mNextState = STATE_READ_DEVICE_INFO;
    }

    /**
     * Resets BLE connection variables to starting values
     */
    private void resetBLEConnectionVariables(){
        mHasStartedBonding = false;
        mHasBonded = false;
        mServiceDiscoveryStarted = false;
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
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            resetBLEConnectionVariables();
                            mNextState = STATE_START_FOTA;
                            // unpair and pair again after jump to be able to discover OTA service
                            BluetoothLeService.unpairDevice(BluetoothLeService.getRemoteDevice());
                        } catch (Exception e) {
                            OTAFirmwareUpgrade.OTAFinished(mContext, ACTION_FOTA_FAIL, "FOTA failed during jump to boot. " + e.getMessage());
                        }
                    }
                }, 1000);

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

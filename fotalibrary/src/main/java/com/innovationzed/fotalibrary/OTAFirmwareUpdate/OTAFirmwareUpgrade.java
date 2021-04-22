/*
 * (c) 2014-2020, Cypress Semiconductor Corporation or a subsidiary of
 * Cypress Semiconductor Corporation.  All rights reserved.
 *
 * This software, including source code, documentation and related
 * materials ("Software"),  is owned by Cypress Semiconductor Corporation
 * or one of its subsidiaries ("Cypress") and is protected by and subject to
 * worldwide patent protection (United States and foreign),
 * United States copyright laws and international treaty provisions.
 * Therefore, you may use this Software only as provided in the license
 * agreement accompanying the software package from which you
 * obtained this Software ("EULA").
 * If no EULA applies, Cypress hereby grants you a personal, non-exclusive,
 * non-transferable license to copy, modify, and compile the Software
 * source code solely for use in connection with Cypress's
 * integrated circuit products.  Any reproduction, modification, translation,
 * compilation, or representation of this Software except as specified
 * above is prohibited without the express written permission of Cypress.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, NONINFRINGEMENT, IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. Cypress
 * reserves the right to make changes to the Software without notice. Cypress
 * does not assume any liability arising out of the application or use of the
 * Software or any product or circuit described in the Software. Cypress does
 * not authorize its products for use in any products where a malfunction or
 * failure of the Cypress product may reasonably be expected to result in
 * significant property damage, injury or death ("High Risk Product"). By
 * including Cypress's product in a High Risk Product, the manufacturer
 * of such system or application assumes all risk of such use and in doing
 * so agrees to indemnify Cypress against all liability.
 */

package com.innovationzed.fotalibrary.OTAFirmwareUpdate;

import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.BackendCommunication.BackendApiRequest;
import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.GattAttributes;
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.FotaApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.STATE_CONNECTED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.STATE_DISCONNECTED;
import static com.innovationzed.fotalibrary.FotaApi.ROOT_DIR;

/**
 * OTA update fragment
 */
public class OTAFirmwareUpgrade extends Service implements OTAFUHandlerCallback {
    public static String DOWNLOADED_FIRMWARE_DIR;

    private final IBinder mBinder = new LocalBinder();
    public boolean mBound;

    //Option Mapping
    public static final String REGEX_MATCHES_CYACD2 = "(?i).*\\.cyacd2$";
    public static final String REGEX_ENDS_WITH_CYACD_OR_CYACD2 = "(?i)\\.cyacd2?$";
    public static boolean mFileUpgradeStarted = false;

    // UUID key
    private static final String LIST_UUID = "UUID";

    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceData = new ArrayList<>();
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceFindMeData = new ArrayList<>();
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceProximityData = new ArrayList<HashMap<String, BluetoothGattService>>();
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattDbServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceMasterData = new ArrayList<>();

    private static BluetoothGattService mService;
    private String mNewFirmwarePath;
    private BackendApiRequest mBackend;

    private static OTAFUHandler DUMMY_HANDLER = (OTAFUHandler) Proxy.newProxyInstance(OTAFirmwareUpgrade.class.getClassLoader(), new Class<?>[]{OTAFUHandler.class}, new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            try {
                new RuntimeException().fillInStackTrace().printStackTrace(pw);
            } finally {
                pw.close();//this will close StringWriter as well
            }
            return null;
        }
    });

    // GATT service and characteristics
    private static BluetoothGattService mOtaService;
    private static BluetoothGattCharacteristic mOtaCharacteristic;

    private OTAFUHandler mOTAFUHandler = DUMMY_HANDLER;//Initializing to DUMMY_HANDLER to avoid NPEs

    private OTAResponseReceiver_v1 mOTAResponseReceiverV1;
    private OTAResponseReceiver_v0 mOTAResponseReceiverV0;

    private BroadcastReceiver mGattOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                processOTAStatus(intent);
            }
        }
    };

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public OTAFirmwareUpgrade getService() {
            return OTAFirmwareUpgrade.this;
        }
    }

    @Override
    public void onCreate() {
        mOTAResponseReceiverV1 = new OTAResponseReceiver_v1();
        mOTAResponseReceiverV0 = new OTAResponseReceiver_v0();
        BluetoothLeService.registerBroadcastReceiver(this, mGattOTAStatusReceiver, Utils.makeGattUpdateIntentFilter());
        BluetoothLeService.registerBroadcastReceiver(this, mOTAResponseReceiverV1, Utils.makeOTADataFilter());
        BluetoothLeService.registerBroadcastReceiver(this, mOTAResponseReceiverV0, Utils.makeOTADataFilterV0());
        mBackend = new BackendApiRequest(this);

        if(connectAndDiscoverServices()){
            Callback<ResponseBody> callback = new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    if (!response.isSuccessful()) {
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
                    mNewFirmwarePath =  ROOT_DIR + File.separator + filename;
                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(), mNewFirmwarePath);

                    if (writtenToDisk) {
                        DOWNLOADED_FIRMWARE_DIR = mNewFirmwarePath;
                        doFota();
                    } else {
                    }

                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                }
            };
            mBackend.downloadLatestFirmwareFile(callback);
        }
    }

    private void doFota(){
        OTAFirmwareUpgrade.mOtaCharacteristic = getGattData();

        boolean isCyacd2File = mNewFirmwarePath != null && isCyacd2File(mNewFirmwarePath);
        Utils.setBooleanSharedPreference(this, Constants.PREF_IS_CYACD2_FILE, isCyacd2File);
        mOTAFUHandler = new OTAFUHandler_v1(this, OTAFirmwareUpgrade.mOtaCharacteristic, mNewFirmwarePath, this, mBackend);

        try {
            prepareFileWrite();
        } catch (Exception e) {
            showErrorDialogMessage("getResources().getString(R.string.ota_alert_invalid_file)", true);
        }
    }

    private void processOTAStatus(Intent intent) {
        /**
         * Shared preference to hold the state of the bootloader
         */
        final String bootloaderState = Utils.getStringSharedPreference(this, Constants.PREF_BOOTLOADER_STATE);
        final String action = intent.getAction();
        Bundle extras = intent.getExtras();
        if (action.equals(BluetoothLeService.ACTION_OTA_STATUS) || action.equals(BluetoothLeService.ACTION_OTA_STATUS_V1)) {
            mOTAFUHandler.processOTAStatus(bootloaderState, extras);
        }
    }

    @Override
    public void showErrorDialogMessage(String errorMessage, final boolean stayOnPage) {
    }


    private boolean connectAndDiscoverServices(){
        int state = STATE_DISCONNECTED;
        int n = 0;
        while (n < 5 && state != STATE_CONNECTED){
            BluetoothLeService.connect(FotaApi.macAddress, "BLE DFU Device", this);
            long timer = System.currentTimeMillis();
            n++;
            do {
                state = BluetoothLeService.getConnectionState();
            } while (state != STATE_CONNECTED && System.currentTimeMillis() - timer < 2000);
        }

        if (state != STATE_CONNECTED) {
            return false;
        }

        boolean result = BluetoothLeService.discoverServices();
        if (result) {
            n = 0;
            List<BluetoothGattService> supportedServices = BluetoothLeService.getSupportedGattServices();
            while (n < 5 && supportedServices.size() == 0) {
                supportedServices = BluetoothLeService.getSupportedGattServices();
                n++;
                try {
                    Thread.sleep(2000);
                } catch (Exception e){
                    return false;
                }
            }
            if (supportedServices.size() > 0) {
                prepareData(supportedServices);

                // Find the gatt service for ota update
                HashMap<String, BluetoothGattService> otaItem = mGattServiceData.get(0);
                for (HashMap<String, BluetoothGattService> item : mGattServiceData) {
                    BluetoothGattService gattService = item.get("UUID");
                    if (gattService.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_SERVICE)) {
                        otaItem = item;
                    }
                }
                mService = otaItem.get("UUID");
                OTAFirmwareUpgrade.mOtaService = mService;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    /**
     * Prepare GATTServices data.
     *
     * @param gattServices
     */
    private void prepareData(List<BluetoothGattService> gattServices) {
        boolean mFindMeSet = false;
        boolean mProximitySet = false;
        boolean mGattSet = false;
        if (gattServices == null)
            return;
        // Clear all array list before entering values.
        mGattServiceData.clear();
        mGattServiceFindMeData.clear();
        mGattServiceMasterData.clear();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, BluetoothGattService> currentServiceData = new HashMap<String, BluetoothGattService>();
            UUID uuid = gattService.getUuid();
            // Optimization code for FindMe Profile
            if (uuid.equals(UUIDDatabase.UUID_IMMEDIATE_ALERT_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                mGattServiceMasterData.add(currentServiceData);
                if (!mGattServiceFindMeData.contains(currentServiceData)) {
                    mGattServiceFindMeData.add(currentServiceData);
                }
                if (!mFindMeSet) {
                    mFindMeSet = true;
                    mGattServiceData.add(currentServiceData);
                }
            }
            // Optimization code for Proximity Profile
            else if (uuid.equals(UUIDDatabase.UUID_LINK_LOSS_SERVICE)
                    || uuid.equals(UUIDDatabase.UUID_TRANSMISSION_POWER_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                mGattServiceMasterData.add(currentServiceData);
                if (!mGattServiceProximityData.contains(currentServiceData)) {
                    mGattServiceProximityData.add(currentServiceData);
                }
                if (!mProximitySet) {
                    mProximitySet = true;
                    mGattServiceData.add(currentServiceData);
                }
            }// Optimization code for GATTDB
            else if (uuid.equals(UUIDDatabase.UUID_GENERIC_ACCESS_SERVICE)
                    || uuid.equals(UUIDDatabase.UUID_GENERIC_ATTRIBUTE_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                mGattDbServiceData.add(currentServiceData);
                if (!mGattSet) {
                    mGattSet = true;
                    mGattServiceData.add(currentServiceData);
                }
            } //Optimization code for HID
            else if (uuid.equals(UUIDDatabase.UUID_HID_SERVICE)) {
                /**
                 * Special handling for KITKAT devices
                 */
                if (android.os.Build.VERSION.SDK_INT < 21) {
                    List<BluetoothGattCharacteristic> allCharacteristics = gattService.getCharacteristics();
                    List<BluetoothGattCharacteristic> RDKCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
                    List<BluetoothGattDescriptor> RDKDescriptors = new ArrayList<BluetoothGattDescriptor>();

                    //Find all Report characteristics
                    for (BluetoothGattCharacteristic characteristic : allCharacteristics) {
                        if (characteristic.getUuid().equals(UUIDDatabase.UUID_REPORT)) {
                            RDKCharacteristics.add(characteristic);
                        }
                    }

                    //Find all Report descriptors
                    for (BluetoothGattCharacteristic rdkcharacteristic : RDKCharacteristics) {
                        List<BluetoothGattDescriptor> descriptors = rdkcharacteristic.
                                getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            RDKDescriptors.add(descriptor);
                        }
                    }
                    /**
                     * Wait for all  descriptors to receive
                     */
                    if (RDKDescriptors.size() == RDKCharacteristics.size() * 2) {

                        for (int pos = 0, descPos = 0; descPos < RDKCharacteristics.size(); pos++, descPos++) {
                            BluetoothGattCharacteristic rdkCharacteristic = RDKCharacteristics.get(descPos);
                            //Mapping the characteristic and descriptors
                            BluetoothGattDescriptor clientdescriptor = RDKDescriptors.get(pos);
                            BluetoothGattDescriptor reportdescriptor = RDKDescriptors.get(pos + 1);
                            if (!rdkCharacteristic.getDescriptors().contains(clientdescriptor)) {
                                rdkCharacteristic.addDescriptor(clientdescriptor);
                            }
                            if (!rdkCharacteristic.getDescriptors().contains(reportdescriptor)) {
                                rdkCharacteristic.addDescriptor(reportdescriptor);
                            }
                            pos++;
                        }
                    }
                    currentServiceData.put(LIST_UUID, gattService);
                    mGattServiceMasterData.add(currentServiceData);
                    mGattServiceData.add(currentServiceData);
                } else {
                    currentServiceData.put(LIST_UUID, gattService);
                    mGattServiceMasterData.add(currentServiceData);
                    mGattServiceData.add(currentServiceData);
                }
            } else {
                currentServiceData.put(LIST_UUID, gattService);
                mGattServiceMasterData.add(currentServiceData);
                mGattServiceData.add(currentServiceData);
            }
        }
        //mApplication.setGattServiceMasterData(mGattServiceMasterData);
    }

    @Override
    public void onDestroy() {
        if (mOTAFUHandler != DUMMY_HANDLER) {
            mOTAFUHandler.setPrepareFileWriteEnabled(false);//This is expected case. onDestroy might be invoked before the file to upgrade is selected.
        }
        BluetoothLeService.unregisterBroadcastReceiver(this, mGattOTAStatusReceiver);
        BluetoothLeService.unregisterBroadcastReceiver(this, mOTAResponseReceiverV1);
        BluetoothLeService.unregisterBroadcastReceiver(this, mOTAResponseReceiverV0);

        if (OTAFirmwareUpgrade.mOtaCharacteristic != null) {
            final String sharedPrefStatus = Utils.getStringSharedPreference(this, Constants.PREF_BOOTLOADER_STATE);
            if (!sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands_v0.EXIT_BOOTLOADER)) {
                clearDataAndPreferences();
            }
            stopBroadcastDataNotify(OTAFirmwareUpgrade.mOtaCharacteristic);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Nullable
    private OTAFUHandler createOTAFUHandler(BluetoothGattCharacteristic otaCharacteristic, byte activeApp, long securityKey, String filepath) {
        boolean isCyacd2File = filepath != null && isCyacd2File(filepath);
        Utils.setBooleanSharedPreference(this, Constants.PREF_IS_CYACD2_FILE, isCyacd2File);

        OTAFUHandler handler = DUMMY_HANDLER;
        if (otaCharacteristic != null && filepath != null && filepath != "") {
            handler = isCyacd2File
                    ? new OTAFUHandler_v1(this, otaCharacteristic, filepath, this, mBackend)
                    : new OTAFUHandler_v0(this, otaCharacteristic, activeApp, securityKey, filepath, this);
        }
        return handler;
    }

    private void prepareFileWrite() {
        if (OTAFirmwareUpgrade.mOtaCharacteristic != null) {
            mOTAFUHandler.prepareFileWrite();
        }
    }

    /**
     * Clears all Shared Preference Data & Resets UI
     */
    public void clearDataAndPreferences() {
        //Resetting all preferences on Stop Button
        Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_ONE_NAME, "Default");
        Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_PATH, "Default");
        Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_NAME, "Default");
        Utils.setStringSharedPreference(this, Constants.PREF_OTA_ACTIVE_APP_ID, "Default");
        Utils.setStringSharedPreference(this, Constants.PREF_OTA_SECURITY_KEY, "Default");
        Utils.setStringSharedPreference(this, Constants.PREF_BOOTLOADER_STATE, "Default");
        Utils.setIntSharedPreference(this, Constants.PREF_PROGRAM_ROW_NO, 0);
        Utils.setIntSharedPreference(this, Constants.PREF_PROGRAM_ROW_START_POS, 0);
        Utils.setIntSharedPreference(this, Constants.PREF_ARRAY_ID, 0);
    }

    @Override
    public String saveAndReturnDeviceAddress() {
        String deviceAddress = BluetoothLeService.getBluetoothDeviceAddress();
        Utils.setStringSharedPreference(this, Constants.PREF_DEV_ADDRESS, deviceAddress);
        return Utils.getStringSharedPreference(this, Constants.PREF_DEV_ADDRESS);
    }

    /**
     * Method to get required characteristics from service
     */
    BluetoothGattCharacteristic getGattData() {
        BluetoothGattCharacteristic characteristic = null;
        List<BluetoothGattCharacteristic> characteristics = OTAFirmwareUpgrade.mOtaService.getCharacteristics();
        for (BluetoothGattCharacteristic c : characteristics) {
            String characteristicUUID = c.getUuid().toString();
            if (characteristicUUID.equalsIgnoreCase(GattAttributes.OTA_CHARACTERISTIC)) {
                characteristic = c;
                prepareBroadcastDataNotify(c);
            }
        }
        return characteristic;
    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param characteristic
     */
    void prepareBroadcastDataNotify(BluetoothGattCharacteristic characteristic) {
        if (BluetoothLeService.isPropertySupported(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            BluetoothLeService.setCharacteristicNotification(characteristic, true);
        }
    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param characteristic
     */
    void stopBroadcastDataNotify(BluetoothGattCharacteristic characteristic) {
        if (BluetoothLeService.isPropertySupported(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            BluetoothLeService.setCharacteristicNotification(characteristic, false);
        }
    }

    @Override
    public boolean isSecondFileUpdateNeeded() {
        String secondFilePath = Utils.getStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_PATH);
        return BluetoothLeService.getBluetoothDeviceAddress().equalsIgnoreCase(saveAndReturnDeviceAddress())
                && (!secondFilePath.equalsIgnoreCase("Default")
                && (!secondFilePath.equalsIgnoreCase("")));
    }

    @Override
    public void setFileUpgradeStarted(boolean status) {
        OTAFirmwareUpgrade.mFileUpgradeStarted = status;
    }

    private boolean isCyacd2File(String file) {
        return file.matches(REGEX_MATCHES_CYACD2);
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String path) {
        try {
            File latestFirmwareFile = new File(path);//getExternalFilesDir(null) + File.separator + filename);

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

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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.GattAttributes;
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.FotaApi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.ACTION_OTA_FAIL;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.STATE_CONNECTED;
import static com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService.STATE_DISCONNECTED;
import static com.innovationzed.fotalibrary.FotaApi.DOWNLOADED_FIRMWARE_DIR;

/**
 * OTA update service
 */
public class OTAFirmwareUpgrade extends Service implements OTAFUHandlerCallback {
    private final IBinder mBinder = new LocalBinder();
    public boolean mBound;

    //Option Mapping
    public static final String REGEX_MATCHES_CYACD2 = "(?i).*\\.cyacd2$";
    public static final String REGEX_ENDS_WITH_CYACD_OR_CYACD2 = "(?i)\\.cyacd2?$";
    public static boolean mFileUpgradeStarted = false;

    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceData = new ArrayList<>();
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceFindMeData = new ArrayList<>();
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceProximityData = new ArrayList<HashMap<String, BluetoothGattService>>();
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattDbServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceMasterData = new ArrayList<>();

    private static BluetoothGattService mService;

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

    private Context mContext;

    private OTAResponseReceiver_v1 mOTAResponseReceiverV1;
    private OTAResponseReceiver_v0 mOTAResponseReceiverV0;
    private static boolean mIsBonded = false;
    private static boolean mIsFotaInProgress = false;
    private static boolean mHasPairedSuccessfully = false;

    private static Handler mTimeoutHandler;
    private static Runnable mTimeoutRunnable;

    private BroadcastReceiver mGattOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                if (intent.getAction().equals(ACTION_BOND_STATE_CHANGED)){
                    if (mIsFotaInProgress){
                        if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_BONDING) {
                            // Do nothing, waiting for bonding to complete
                            mIsBonded = true;
                        } else if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_NONE && !mIsBonded){
                            BluetoothLeService.getRemoteDevice().createBond();
                        } else if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_NONE && mIsBonded){
                            OTAFinished(getApplicationContext(), ACTION_OTA_FAIL, "Could not pair with device. Possible reasons: user did not approve pairing request or device was not in boot mode.");
                        } else if (BluetoothLeService.getRemoteDevice().getBondState() == BOND_BONDED && mIsBonded){
                            mHasPairedSuccessfully = true;
                            mOtaService = BluetoothLeService.getService(mContext, UUIDDatabase.UUID_OTA_UPDATE_SERVICE);
                            if(mOtaService != null){
                                doFota();
                            } else {
                                OTAFinished(getApplicationContext(), ACTION_OTA_FAIL, "Could not connect or discover services of device.");
                            }
                        }
                    }
                } else {
                    processOTAStatus(intent);
                }
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
        mContext = this;
        mIsFotaInProgress = true;
        mIsBonded = false;
        mOTAResponseReceiverV1 = new OTAResponseReceiver_v1();
        mOTAResponseReceiverV0 = new OTAResponseReceiver_v0();
        BluetoothLeService.registerBroadcastReceiver(this, mGattOTAStatusReceiver, Utils.makeGattUpdateIntentFilter());
        BluetoothLeService.registerBroadcastReceiver(this, mOTAResponseReceiverV1, Utils.makeOTADataFilter());
        BluetoothLeService.registerBroadcastReceiver(this, mOTAResponseReceiverV0, Utils.makeOTADataFilterV0());

        mTimeoutHandler = new Handler();
        mTimeoutRunnable = new Runnable(){
            @Override
            public void run() {
                if (!mHasPairedSuccessfully){
                    OTAFinished(getApplicationContext(), ACTION_OTA_FAIL, "Timeout/disconnect");
                }
            }
        };
        mTimeoutHandler.postDelayed(mTimeoutRunnable, 60000);

        BluetoothLeService.connect(FotaApi.macAddress, this);
        try {
            Thread.sleep(1500);
        } catch (Exception e) {

        }

        if (BluetoothLeService.getRemoteDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
            BluetoothLeService.unpairDevice(BluetoothLeService.getRemoteDevice());
        } else {
            BluetoothLeService.getRemoteDevice().createBond();
        }
    }

    private void doFota(){
        OTAFirmwareUpgrade.mOtaCharacteristic = getGattData();
        if (OTAFirmwareUpgrade.mOtaCharacteristic == null){
            OTAFinished(this, ACTION_OTA_FAIL, "getGattData() failed (OTAFirmwareUpgrade.mOtaCharacteristic is null)");
        }

        boolean isCyacd2File = DOWNLOADED_FIRMWARE_DIR != null && isCyacd2File(DOWNLOADED_FIRMWARE_DIR);
        Utils.setBooleanSharedPreference(this, Constants.PREF_IS_CYACD2_FILE, isCyacd2File);
        mOTAFUHandler = new OTAFUHandler_v1(this, OTAFirmwareUpgrade.mOtaCharacteristic, DOWNLOADED_FIRMWARE_DIR, this);

        try {
            mOTAFUHandler.prepareFileWrite();
        } catch (Exception e) {
            OTAFinished(this, ACTION_OTA_FAIL, "Invalid firmware file.");
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
    public void onDestroy() {
        if (mOTAFUHandler != DUMMY_HANDLER) {
            mOTAFUHandler.setPrepareFileWriteEnabled(false);//This is expected case. onDestroy might be invoked before the file to upgrade is selected.
        }
        BluetoothLeService.unregisterBroadcastReceiver(this, mGattOTAStatusReceiver);
        BluetoothLeService.unregisterBroadcastReceiver(this, mOTAResponseReceiverV1);
        BluetoothLeService.unregisterBroadcastReceiver(this, mOTAResponseReceiverV0);

        final String sharedPrefStatus = Utils.getStringSharedPreference(this, Constants.PREF_BOOTLOADER_STATE);
        if (!sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands_v0.EXIT_BOOTLOADER)) {
            clearDataAndPreferences();
        }

        if (OTAFirmwareUpgrade.mOtaCharacteristic != null) {
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
                    ? new OTAFUHandler_v1(this, otaCharacteristic, filepath, this)
                    : new OTAFUHandler_v0(this, otaCharacteristic, activeApp, securityKey, filepath, this);
        }
        return handler;
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

    public static void OTAFinished(Context context, String action, String reason){
        mIsFotaInProgress = false;
        mHasPairedSuccessfully = false;
        mTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        Utils.broadcastOTAFinished(context, action, reason);
    }

}

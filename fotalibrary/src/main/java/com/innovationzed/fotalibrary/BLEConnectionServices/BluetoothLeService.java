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

package com.innovationzed.fotalibrary.BLEConnectionServices;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.FotaBroadcastReceiver;
import com.innovationzed.fotalibrary.CommonUtils.GattAttributes;
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;
import com.innovationzed.fotalibrary.CommonUtils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.fota.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE_V1 =
            "com.fota.bluetooth.le.ACTION_OTA_DATA_AVAILABLE_V1";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
    public final static String ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL =
            "com.example.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL";
    public final static String ACTION_WRITE_COMPLETED =
            "android.bluetooth.device.action.ACTION_WRITE_COMPLETED";
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    public final static String ACTION_GATT_INSUFFICIENT_ENCRYPTION =
            "com.example.bluetooth.le.ACTION_GATT_INSUFFICIENT_ENCRYPTION";
    public static final String ACTION_PAIRING_CANCEL =
            "android.bluetooth.device.action.PAIRING_CANCEL";

    public final static String ACTION_OTA_STATUS = "com.example.bluetooth.le.ACTION_OTA_STATUS";
    public final static String ACTION_OTA_STATUS_V1 = "com.example.bluetooth.le.ACTION_OTA_STATUS_V1";

    /**
     * Connection status constants
     */
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 4;

    public static final boolean MTU_USE_NEGOTIATED = true;//Use negotiated MTU vs MTU_DEFAULT(20)
    public static final int MTU_DEFAULT = 20;//MIN_MTU(23) - 3
    public static final int MTU_NUM_BYTES_TO_SUBTRACT = 3;//3 bytes need to be subtracted
    public static Semaphore writeSemaphore = new Semaphore(1);

    /**
     * BluetoothAdapter for handling connections
     */
    public static BluetoothAdapter mBluetoothAdapter;

    public static BluetoothGatt mBluetoothGatt;

    public static boolean mClearCacheOnDisconnect = false;
    public static boolean mUnpairOnDisconnect = false;

    /**
     * Disable/enable notification
     */
    public static ArrayList<BluetoothGattCharacteristic> mEnabledCharacteristics = new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mRDKCharacteristics = new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mGlucoseCharacteristics = new ArrayList<>();
    private static ArrayList<BluetoothGattCharacteristic> mSelectedCharacteristicsToEnable = new ArrayList<>();
    private static ArrayList<BluetoothGattCharacteristic> mSelectedCharacteristicsToDisable = new ArrayList<>();

    public static boolean mDisableEnabledCharacteristicsFlag = false;
    public static boolean mEnableRDKCharacteristicsFlag = false;
    private static boolean mEnableGlucoseCharacteristicsFlag = false;
    private static boolean mEnableSelectedCharacteristicsFlag = false;
    private static boolean mDisableSelectedCharacteristicsFlag = false;
    private static boolean mPostponedDisableSelectedCharacteristicsFlag = false;

    private static int mConnectionState = STATE_DISCONNECTED;
    private static boolean mOtaExitBootloaderCmdInProgress = false;

    /**
     * Device address
     */
    private static String mBluetoothDeviceAddress;
    private static String mBluetoothDeviceName;
    private static Context mContext;

    public static boolean mSyncCommandFlag;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * UUID key
     */
    private static final String LIST_UUID = "UUID";

    /**
     * Implements callback methods for GATT events that the app cares about. For
     * example,connection change and services discovered.
     */
    private final static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // GATT Server connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTED;
                }
                broadcastConnectionUpdate(ACTION_GATT_CONNECTED);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTED;
                }
                broadcastConnectionUpdate(ACTION_GATT_DISCONNECTED);

                // mBluetoothGatt should only be accessed from within the main thread.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // The connection might drop due to the kit being manually reset (CONFIGURATORS-899)
                        // For such cases we need to do the cleanup here
                        if (mClearCacheOnDisconnect) {
                            // Clearing Bluetooth cache before disconnecting from the device
                            if (mBluetoothGatt != null) {
                                refreshDeviceCache(mBluetoothGatt);
                            }
                        }
                        if (mUnpairOnDisconnect) {
                            // Deleting bond before disconnecting from the device
                            if (mBluetoothGatt != null) {
                                unpairDevice(mBluetoothGatt.getDevice());
                            }
                        }

                        // ... and release connection handler
                        close();
                    }
                });
            }
            // GATT Server Connecting
            else if (newState == BluetoothProfile.STATE_CONNECTING) {
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTING;
                }
                broadcastConnectionUpdate(ACTION_GATT_CONNECTING);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTING;
                }
                broadcastConnectionUpdate(ACTION_GATT_DISCONNECTING);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // GATT Services discovered
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                        status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
//                    pairDevice();
                }
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            } else {

                Intent characteristicErrorIntent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                characteristicErrorIntent.putExtra(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE, "" + status);
                sendGlobalBroadcastIntent(mContext, characteristicErrorIntent);

                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                    sendGlobalBroadcastIntent(mContext, new Intent(ACTION_GATT_INSUFFICIENT_ENCRYPTION));
                }
            }

            boolean isExitBootloaderCmd = false;
            boolean isSyncCommandFlag = false;
            synchronized (mGattCallback) {
                isExitBootloaderCmd = mOtaExitBootloaderCmdInProgress;
                isSyncCommandFlag = mSyncCommandFlag;
                mOtaExitBootloaderCmdInProgress = false;
                mSyncCommandFlag = false;
            }
            if (isExitBootloaderCmd) {
                onOtaExitBootloaderComplete(status);
            }

            if (characteristic.getUuid().toString().equalsIgnoreCase(GattAttributes.OTA_CHARACTERISTIC)) {
                writeSemaphore.release();
            }

            if (isSyncCommandFlag) {
                Intent otaStatusV1Intent = new Intent(BluetoothLeService.ACTION_OTA_STATUS_V1);
                Bundle extras = new Bundle();
                if (BluetoothGatt.GATT_SUCCESS != status) {
                    extras.putString(Constants.EXTRA_ERROR_OTA, "" + status);
                }
                otaStatusV1Intent.putExtras(extras);
                sendGlobalBroadcastIntent(mContext, otaStatusV1Intent);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // GATT Characteristic read
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastNotifyUpdate(characteristic);
            } else {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
//                    pairDevice();
                    sendGlobalBroadcastIntent(mContext, new Intent(ACTION_GATT_INSUFFICIENT_ENCRYPTION));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastNotifyUpdate(characteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Utils.setIntSharedPreference(mContext, Constants.PREF_MTU_NEGOTIATED, mtu);
            }
        }
    };

    // NOTE: Android 8 (Oreo) bans implicit broadcasts (where the intent doesn't specify the receiver's package and/or Java class)
    // FIX: use explicit broadcasts
    public static void sendGlobalBroadcastIntent(Context context, Intent intent) {
        // Make intent explicit by specifying the package name
        intent.setPackage(Constants.PACKAGE_NAME);
        context.sendBroadcast(intent);
    }

    // NOTE: Local broadcasts are not being received by the receivers registered in the AndroidManifest.xml
    public static void sendLocalBroadcastIntent(Context context, Intent intent) {
        // Make intent explicit by specifying the package name
        intent.setPackage(Constants.PACKAGE_NAME);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void registerBroadcastReceiver(Context context, FotaBroadcastReceiver receiver, IntentFilter filter) {
        if (!receiver.isRegistered) {
            // Registering receiver as a LOCAL receiver
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

            // Registering receiver as a GLOBAL receiver
            context.registerReceiver(receiver, filter);
            receiver.isRegistered = true;
        }
    }

    public static void unregisterBroadcastReceiver(Context context, FotaBroadcastReceiver receiver) {
        if (receiver.isRegistered) {
            // Unregistering receiver as a LOCAL receiver
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);

            // Unregistering receiver as a GLOBAL receiver
            context.unregisterReceiver(receiver);
            receiver.isRegistered = false;
        }
    }

    public static void exchangeGattMtu(int mtu) {
        int retry = 5;
        boolean status = false;
        while ((false == status) && retry > 0) {
            status = mBluetoothGatt.requestMtu(mtu);
            retry--;
        }
    }

    private final IBinder mBinder = new LocalBinder();
    /**
     * Flag to check the mBound status
     */
    public boolean mBound;
    /**
     * BlueTooth manager for handling connections
     */
    private static BluetoothManager mBluetoothManager;

    public static String getBluetoothDeviceAddress() {
        return mBluetoothDeviceAddress;
    }

    public static String getBluetoothDeviceName() {
        return mBluetoothDeviceName;
    }

    private static void broadcastConnectionUpdate(String action) {
        Intent intent = new Intent(action);
        // NOTE: sending GLOBAL broadcast as there is a receiver in AndroidManifest.xml which listens to ACTION_GATT_DISCONNECTED intents
        sendGlobalBroadcastIntent(mContext, intent);
    }

    private static void broadcastWriteStatusUpdate(final String action) {
        final Intent intent = new Intent((action));
        sendGlobalBroadcastIntent(mContext, intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent dataAvailableIntent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle bundle = new Bundle();
        // Putting the byte value read for GATT Db
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE,
                characteristic.getValue());
        bundle.putString(Constants.EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());
        bundle.putInt(Constants.EXTRA_BYTE_INSTANCE_VALUE,
                characteristic.getInstanceId());
        bundle.putString(Constants.EXTRA_BYTE_SERVICE_UUID_VALUE,
                characteristic.getService().getUuid().toString());
        bundle.putInt(Constants.EXTRA_BYTE_SERVICE_INSTANCE_VALUE,
                characteristic.getService().getInstanceId());

        //case for OTA characteristic received
        if (characteristic.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_CHARACTERISTIC)) {
            boolean isCyacd2File = Utils.getBooleanSharedPreference(mContext, Constants.PREF_IS_CYACD2_FILE);
            String intentAction = isCyacd2File
                    ? BluetoothLeService.ACTION_OTA_DATA_AVAILABLE_V1
                    : BluetoothLeService.ACTION_OTA_DATA_AVAILABLE;
            Intent otaDataAvailableIntent = new Intent(intentAction);
            otaDataAvailableIntent.putExtras(bundle);
            // NOTE: sending GLOBAL broadcast as there are receivers in AndroidManifest.xml which listen to ACTION_OTA_DATA_AVAILABLE_V1 and ACTION_OTA_DATA_AVAILABLE intents
            sendGlobalBroadcastIntent(mContext, otaDataAvailableIntent);
        }
        // Manufacturer Name read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_MANUFACTURER_NAME)) {
            bundle.putString(Constants.EXTRA_MANUFACTURER_NAME,
                    Utils.getManufacturerName(characteristic));
        }
        // Model Number read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_MODEL_NUMBER)) {
            bundle.putString(Constants.EXTRA_MODEL_NUMBER,
                    Utils.getModelNumber(characteristic));
        }
        // Serial Number read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SERIAL_NUMBER)) {
            bundle.putString(Constants.EXTRA_SERIAL_NUMBER,
                    Utils.getSerialNumber(characteristic));
        }
        // Hardware Revision read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_HARDWARE_REVISION)) {
            bundle.putString(Constants.EXTRA_HARDWARE_REVISION,
                    Utils.getHardwareRevision(characteristic));
        }
        // Firmware Revision read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_FIRMWARE_REVISION)) {
            bundle.putString(Constants.EXTRA_FIRMWARE_REVISION,
                    Utils.getFirmwareRevision(characteristic));
        }
        // Software Revision read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SOFTWARE_REVISION)) {
            bundle.putString(Constants.EXTRA_SOFTWARE_REVISION,
                    Utils.getSoftwareRevision(characteristic));
        }
        // System ID read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SYSTEM_ID)) {
            bundle.putString(Constants.EXTRA_SYSTEM_ID,
                    Utils.getSystemId(characteristic));
        }
        // Regulatory Certification Data List read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_REGULATORY_CERTIFICATION_DATA_LIST)) {
            bundle.putString(Constants.EXTRA_REGULATORY_CERTIFICATION_DATA_LIST,
                    Utils.byteArrayToHex(characteristic.getValue()));
        }
        // PnP ID read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_PNP_ID)) {
            bundle.putString(Constants.EXTRA_PNP_ID,
                    Utils.getPnPId(characteristic));
        }
        // Battery level read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_BATTERY_LEVEL)) {
            bundle.putString(Constants.EXTRA_BTL_VALUE,
                    Utils.getBatteryLevel(characteristic));
        }

        // Alert level read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_ALERT_LEVEL)) {
            bundle.putString(Constants.EXTRA_ALERT_VALUE,
                    Utils.getAlertLevel(characteristic));
        }
        // Transmission power level read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_TRANSMISSION_POWER_LEVEL)) {
            bundle.putInt(Constants.EXTRA_POWER_VALUE,
                    Utils.getTransmissionPower(characteristic));
        }

        dataAvailableIntent.putExtras(bundle);
        /**
         * Sending the broad cast so that it can be received on registered
         * receivers
         */
        sendLocalBroadcastIntent(mContext, dataAvailableIntent);
    }

    private static void onOtaExitBootloaderComplete(int status) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE, new byte[]{(byte) status});
        Intent otaDataAvailableIntent = new Intent(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE_V1);
        otaDataAvailableIntent.putExtras(bundle);
        // NOTE: sending GLOBAL broadcast as there is receiver in AndroidManifest.xml which listens to ACTION_OTA_DATA_AVAILABLE intents
        sendGlobalBroadcastIntent(mContext, otaDataAvailableIntent);
    }

    public static boolean startDeviceScan(){
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.startDiscovery();
        }
        return false;
    }

    public static boolean stopDeviceScan(){
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.cancelDiscovery();
        }
        return false;
    }

    /**
     * Connects to the GATT server hosted on the BlueTooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static boolean connect(final String address, Context context) {
        mContext = context;
        Utils.setIntSharedPreference(mContext, Constants.PREF_MTU_NEGOTIATED, 0);//The actual value will be set in hte onMtuChanged callback method

        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = device.getName();
        mClearCacheOnDisconnect = Utils.getBooleanSharedPreference(mContext, Constants.PREF_CLEAR_CACHE_ON_DISCONNECT);
        mUnpairOnDisconnect = Utils.getBooleanSharedPreference(mContext, Constants.PREF_UNPAIR_ON_DISCONNECT);
        return true;
    }

    /**
     * Reconnect method to connect to already connected device
     */
    public static void reconnect() {
        BluetoothDevice device = getRemoteDevice();
        if (device == null) {
            return;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close(); // Disposing off previous connection resources
        }
        mBluetoothGatt = null;//Creating a new instance of GATT before connect
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
    }

    public static BluetoothDevice getRemoteDevice() {
        return mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
    }

    public static BluetoothDevice getRemoteDevice(String macAddress) {
        return mBluetoothAdapter.getRemoteDevice(macAddress);
    }

    /**
     * Method to clear the device cache
     *
     * @param gatt
     * @return boolean
     */
    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method refresh = gatt.getClass().getMethod("refresh");
            if (refresh != null) {
                return (Boolean) refresh.invoke(gatt);
            }
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        // Clearing Bluetooth cache before disconnecting from the device
        if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_CLEAR_CACHE_ON_DISCONNECT)) {
            BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
            mClearCacheOnDisconnect = false;
        }
        // Deleting bond before disconnecting from the device
        if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_UNPAIR_ON_DISCONNECT)) {
            BluetoothLeService.unpairDevice(mBluetoothGatt.getDevice());
            mUnpairOnDisconnect = false;
        }

        mBluetoothGatt.disconnect();
        close();
    }

    public static boolean discoverServices() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return false;
        } else {
            boolean result = mBluetoothGatt.discoverServices();
            return result;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public static void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a read on a given {@code BluetoothGattDescriptor }.
     *
     * @param descriptor The descriptor to read from.
     */
    public static void readDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readDescriptor(descriptor);
    }

    /**
     * Request a write with no response on a given
     * {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray      to write
     */
    public static void writeCharacteristicNoResponse(BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            characteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    public static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value, boolean isExitBootloaderCmd) {
        synchronized (mGattCallback) {
            writeOTABootLoaderCommand(characteristic, value);
            if (isExitBootloaderCmd) {
                mOtaExitBootloaderCmdInProgress = true;
            }
        }
    }

    public static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            writeOTABootLoaderCommandNoResponse(characteristic, value);
        } else {
            writeOTABootLoaderCommandWithResponse(characteristic, value);
        }
    }

    private static void writeOTABootLoaderCommandNoResponse(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        final int mtuValue;
        if (MTU_USE_NEGOTIATED) {
            int negotiatedMtu = Utils.getIntSharedPreference(mContext, Constants.PREF_MTU_NEGOTIATED);
            mtuValue = Math.max(MTU_DEFAULT, (negotiatedMtu - MTU_NUM_BYTES_TO_SUBTRACT));
        } else {
            mtuValue = MTU_DEFAULT;
        }

        int totalLength = value.length;
        int localLength = 0;
        byte[] localValue = new byte[mtuValue];

        do {
            try {
                writeSemaphore.acquire();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            if (totalLength >= mtuValue) {
                for (int i = 0; i < mtuValue; i++) {
                    localValue[i] = value[localLength + i];
                }
                characteristic.setValue(localValue);
                totalLength -= mtuValue;
                localLength += mtuValue;
            } else {
                byte[] lastValue = new byte[totalLength];
                for (int i = 0; i < totalLength; i++) {
                    lastValue[i] = value[localLength + i];
                }
                characteristic.setValue(lastValue);
                totalLength = 0;
            }

            int counter = 20;
            boolean status;

            do {
                int i = 0;
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                status = mBluetoothGatt.writeCharacteristic(characteristic);
                if (false == status) {
                    try {
                        i++;
                        Thread.sleep(100, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while ((false == status) && (counter-- > 0));

            if (status) {
            } else {
                writeSemaphore.release();
            }
        } while (totalLength > 0);
    }

    private static void writeOTABootLoaderCommandWithResponse(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        characteristic.setValue(value);
        int counter = 20;
        boolean status;
        do {
            int i = 0;
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            status = mBluetoothGatt.writeCharacteristic(characteristic);
            if (false == status) {
                try {
                    i++;
                    Thread.sleep(100, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while ((false == status) && (counter-- > 0));
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray
     */
    public static void writeCharacteristicGattDb(
            BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            characteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * Writes the characteristic value to the given characteristic.
     *
     * @param characteristic the characteristic to write to
     * @return true if request has been sent
     */
    public static final boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        if (!isPropertySupported(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE))
            return false;

        return gatt.writeCharacteristic(characteristic);
    }


    /**
     * Request a write on a given {@code BluetoothGattCharacteristic} for RGB.
     *
     * @param characteristic
     * @param red
     * @param green
     * @param blue
     * @param intensity
     */
    public static void writeCharacteristicRGB(
            BluetoothGattCharacteristic characteristic, int red, int green,
            int blue, int intensity) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = new byte[4];
            valueByte[0] = (byte) red;
            valueByte[1] = (byte) green;
            valueByte[2] = (byte) blue;
            valueByte[3] = (byte) intensity;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public static void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            return;
        }

        // Setting default write type according to CDT 222486
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        if (characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            } else {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Enables or disables indications on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable indications. False otherwise.
     */
    public static void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            return;
        }

        // Setting default write type according to CDT 222486
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        if (characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            } else {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public static List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public static int getConnectionState() {
        synchronized (mGattCallback) {
            return mConnectionState;
        }
    }

    public static int getConnectionState(BluetoothDevice device){
        return mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
    }

    public static boolean unpairDevice(BluetoothDevice device) {
        try {
            Boolean rv = (Boolean) invokeBluetoothDeviceMethod(device, "removeBond");
            return rv;
        } catch (Exception e) {
            return false;
        }
    }

    private static Object invokeBluetoothDeviceMethod(BluetoothDevice dev, String methodName, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class c = dev.getClass();
        Method m = c.getMethod(methodName);
        m.setAccessible(true);
        return m.invoke(dev, args);
    }

    private static void addRemoveData(BluetoothGattDescriptor descriptor) {
        switch (descriptor.getValue()[0]) {
            case 0:
                //Disabled notification and indication
                removeEnabledCharacteristic(descriptor.getCharacteristic());
                break;
            case 1:
                //Enabled notification
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
            case 2:
                //Enabled indication
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
        }
    }

    private static void addEnabledCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!mEnabledCharacteristics.contains(characteristic))
            mEnabledCharacteristics.add(characteristic);
    }

    private static void removeEnabledCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mEnabledCharacteristics.contains(characteristic))
            mEnabledCharacteristics.remove(characteristic);
    }

    public static boolean disableAllEnabledCharacteristics() {
        if (mEnabledCharacteristics.size() > 0) {
            mDisableEnabledCharacteristicsFlag = true;
            BluetoothGattCharacteristic c = mEnabledCharacteristics.get(0);
            Utils.debug("Disabling characteristic " + c.getUuid());
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                setCharacteristicNotification(c, false);
            } else if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                setCharacteristicIndication(c, false);
            }
        } else {
            mDisableEnabledCharacteristicsFlag = false;
        }
        return mDisableEnabledCharacteristicsFlag;
    }

    public static void enableAllRDKCharacteristics() {
        if (mRDKCharacteristics.size() > 0) {
            mEnableRDKCharacteristicsFlag = true;
            BluetoothGattCharacteristic c = mRDKCharacteristics.get(0);
            Utils.debug("RDK characteristics: enabling characteristic " + c.getUuid());
            setCharacteristicNotification(c, true);
        } else {
            Utils.debug("RDK characteristics: all enabled");
            mEnableRDKCharacteristicsFlag = false;
            broadcastWriteStatusUpdate(ACTION_WRITE_COMPLETED);
        }
    }

    public static boolean enableAndDisableSelectedCharacteristics(Collection<BluetoothGattCharacteristic> enableList, Collection<BluetoothGattCharacteristic> disableList) {
        mSelectedCharacteristicsToDisable = new ArrayList<>(disableList);
        mDisableSelectedCharacteristicsFlag = false;
        mPostponedDisableSelectedCharacteristicsFlag = true;
        return enableSelectedCharacteristics(enableList);
    }

    public static boolean enableSelectedCharacteristics(Collection<BluetoothGattCharacteristic> enableList) {
        mSelectedCharacteristicsToEnable = new ArrayList<>(enableList);
        return enableSelectedCharacteristics();
    }

    private static boolean enableSelectedCharacteristics() {
        if (mSelectedCharacteristicsToEnable.size() > 0) {
            mEnableSelectedCharacteristicsFlag = true;
            BluetoothGattCharacteristic c = mSelectedCharacteristicsToEnable.get(0);
            Utils.debug("Selected characteristics: enabling characteristic " + c.getUuid());
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                setCharacteristicNotification(c, true);
            } else if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                setCharacteristicIndication(c, true);
            }
        } else {
            Utils.debug("Selected characteristics: all enabled");
            mEnableSelectedCharacteristicsFlag = false;
            broadcastWriteStatusUpdate(ACTION_WRITE_COMPLETED);
            if (mPostponedDisableSelectedCharacteristicsFlag) {
                mPostponedDisableSelectedCharacteristicsFlag = false;
                disableSelectedCharacteristics();
            }
        }
        return mEnableSelectedCharacteristicsFlag;
    }

    public static boolean disableSelectedCharacteristics(Collection<BluetoothGattCharacteristic> disableList) {
        mSelectedCharacteristicsToDisable = new ArrayList<>(disableList);
        return disableSelectedCharacteristics();
    }

    private static boolean disableSelectedCharacteristics() {
        // remove characteristics which (either/or)
        // - were never enabled
        // - have been disabled as a result of previous invocation of this method
        for (Iterator<BluetoothGattCharacteristic> it = mSelectedCharacteristicsToDisable.iterator(); it.hasNext(); ) {
            BluetoothGattCharacteristic c = it.next();
            if (!mEnabledCharacteristics.contains(c)) {
                it.remove();
            }
        }

        if (mSelectedCharacteristicsToDisable.size() > 0) {
            mDisableSelectedCharacteristicsFlag = true;
            BluetoothGattCharacteristic c = mSelectedCharacteristicsToDisable.get(0);
            Utils.debug("Selected characteristics: disabling characteristic " + c.getUuid());
            if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                setCharacteristicNotification(c, false);
            } else if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                setCharacteristicIndication(c, false);
            }
        } else {
            Utils.debug("Selected characteristics: all disabled");
            mDisableSelectedCharacteristicsFlag = false;
        }
        return mDisableSelectedCharacteristicsFlag;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public static void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local BlueTooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;
    }

    @Override
    public void onCreate() {
        // Initializing the service
        if (false == initialize()) {
            return;
        }
    }

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public static void setSyncCommandFlag(boolean value) {
        synchronized (mGattCallback) {
            mSyncCommandFlag = value;
        }
    }

    /**
     * Return the property enabled in the characteristic
     *
     * @param characteristic
     * @param requestedProps
     * @return
     */
    public static boolean isPropertySupported(BluetoothGattCharacteristic characteristic, int requestedProps) {
        return (characteristic.getProperties() & requestedProps) > 0;
    }

    /**
     * Tries to find a specific gatt service by uuid
     * (returns null if the service is not found)
     * @param uuidService
     * @return
     */
    public static BluetoothGattService getService(UUID uuidService){
        List<BluetoothGattService> supportedServices = BluetoothLeService.getSupportedGattServices();

        if (supportedServices.size() > 0) {
            ArrayList<HashMap<String, BluetoothGattService>> gattServiceData = prepareData(supportedServices);

            // Find the gatt service
            for (HashMap<String, BluetoothGattService> item : gattServiceData) {
                BluetoothGattService gattService = item.get("UUID");
                if (gattService.getUuid().equals(uuidService)) {
                    return item.get("UUID");
                }
            }
        }
        return null;
    }

    /**
     * Prepare GATTServices data.
     *
     * @param gattServices
     */
    private static ArrayList<HashMap<String, BluetoothGattService>> prepareData(List<BluetoothGattService> gattServices) {
        ArrayList<HashMap<String, BluetoothGattService>> gattServiceData = new ArrayList<>();
        if (gattServices == null) {
            return gattServiceData;
        }

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, BluetoothGattService> currentServiceData = new HashMap<String, BluetoothGattService>();

            currentServiceData.put(LIST_UUID, gattService);
            gattServiceData.add(currentServiceData);
        }
        return gattServiceData;
    }
}

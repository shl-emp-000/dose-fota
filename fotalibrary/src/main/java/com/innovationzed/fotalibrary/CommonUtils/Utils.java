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

package com.innovationzed.fotalibrary.CommonUtils;

import android.R.integer;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import static com.innovationzed.fotalibrary.FotaApi.downloadedFirmwareDir;

/**
 * Class for commonly used methods in the project
 */
public class Utils {

    public static final String OTA_REASON = "OTA_REASON";
    public static final String IS_IN_BOOT_MODE = "IS_IN_BOOT_MODE";

    public static final String REGEX_MATCHES_CYACD2 = "(?i).*\\.cyacd2$";
    public static final String REGEX_ENDS_WITH_CYACD_OR_CYACD2 = "(?i)\\.cyacd2?$";

    // Shared preference constant
    private static final String SHARED_PREF_NAME = "FOTA Shared Preference";

    private static final String BASE_UUID_FORMAT = "(((0000)|(\\d{4}))(\\d{4}))-0000-1000-8000-00805F9B34FB";
    private static final Pattern BASE_UUID_PATTERN = Pattern.compile(BASE_UUID_FORMAT, Pattern.CASE_INSENSITIVE);
    public static final Locale DATA_LOCALE = Locale.ROOT;
    public static final String DATA_LOGGER_FILENAME_PATTERN = "dd-MMM-yyyy";


    /**
     * Delete the firmware file and folder
     */
    public static void deleteFirmwareFile(){
        // Delete firmware file
        if (downloadedFirmwareDir != null){
            File file = new File(downloadedFirmwareDir);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Broadcasts an action specifying how OTA was finished and the reason
     * @param context
     * @param action describing how OTA was finished (ex: ACTION_FOTA_FAIL)
     * @param reason for finishing this way (error message etc)
     */
    public static void broadcastOTAFinished(Context context, String action, String reason){
        broadcastOTAFinished(context, action, reason, false);
    }

    /**
     * Broadcasts an action specifying how OTA was finished and the reason
     * @param context
     * @param action describing how OTA was finished (ex: ACTION_FOTA_FAIL)
     * @param reason for finishing this way (error message etc)
     */
    public static void broadcastOTAFinished(Context context, String action, String reason, boolean bootMode){
        deleteFirmwareFile();
        Intent otaFinishedIntent = new Intent(action);
        Bundle bundle = new Bundle();
        bundle.putString(OTA_REASON, reason);
        bundle.putBoolean(IS_IN_BOOT_MODE, bootMode);
        otaFinishedIntent.putExtras(bundle);
        BluetoothLeService.sendLocalBroadcastIntent(context, otaFinishedIntent);
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
    public static int compareVersion(String version1, String version2) {
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

    /**
     * Checks if input UUID string is of base UUID format and if that is true returns the unique 16 or 32 bits of it
     *
     * @param uuid128 complete 128 bit UUID string
     * @return
     */
    public static String getUuidShort(String uuid128) {
        String result = uuid128;
        if (uuid128 != null) {
            Matcher m = BASE_UUID_PATTERN.matcher(uuid128);
            if (m.matches()) {
                boolean isUuid16 = m.group(3) != null;
                if (isUuid16) { // 0000xxxx
                    String uuid16 = m.group(5);
                    result = uuid16;
                } else { // xxxxxxxx
                    String uuid32 = m.group(1);
                    result = uuid32;
                }
            }
        }
        return result;
    }

    /**
     * Returns the manufacture name from the given characteristic
     */
    public static String getManufacturerName(BluetoothGattCharacteristic characteristic) {
        String manufacturerName = characteristic.getStringValue(0);
        return manufacturerName;
    }

    /**
     * Returns the model number from the given characteristic
     */
    public static String getModelNumber(BluetoothGattCharacteristic characteristic) {
        String modelNumber = characteristic.getStringValue(0);
        return modelNumber;
    }

    /**
     * Returns the serial number from the given characteristic
     */
    public static String getSerialNumber(BluetoothGattCharacteristic characteristic) {
        String serialNumber = characteristic.getStringValue(0);
        return serialNumber;
    }

    /**
     * Returns the hardware revision from the given characteristic
     */
    public static String getHardwareRevision(BluetoothGattCharacteristic characteristic) {
        String hardwareRevision = characteristic.getStringValue(0);
        return hardwareRevision;
    }

    /**
     * Returns the Firmware revision from the given characteristic
     */
    public static String getFirmwareRevision(BluetoothGattCharacteristic characteristic) {
        String firmwareRevision = characteristic.getStringValue(0);
        return firmwareRevision;
    }

    /**
     * Returns the software revision from the given characteristic
     */
    public static String getSoftwareRevision(BluetoothGattCharacteristic characteristic) {
        String softwareRevision = characteristic.getStringValue(0);
        return softwareRevision;
    }

    /**
     * Returns the SystemID from the given characteristic
     */
    public static String getSystemId(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        final StringBuilder sb = new StringBuilder(data.length);
        if (data != null && data.length > 0) {
            for (byte b : data)
                sb.append(formatForRootLocale("%02X ", b));
        }
        return String.valueOf(sb);
    }

    /**
     * Returns the PNP ID from the given characteristic
     */
    public static String getPnPId(BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();
        final StringBuilder sb = new StringBuilder(data.length);
        if (data != null && data.length > 0) {
            for (byte b : data)
                sb.append(formatForRootLocale("%02X ", b));
        }
        return String.valueOf(sb);
    }

    /**
     * Adding the necessary Intent filters for Broadcast receivers
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothLeService.ACTION_PAIRING_CANCEL);
        filter.addAction(BluetoothLeService.ACTION_OTA_STATUS);//CYACD
        filter.addAction(BluetoothLeService.ACTION_OTA_STATUS_V1);//CYACD2
        filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING);
        filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTING);
        filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
        filter.addAction(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_ERROR);
        filter.addAction(BluetoothLeService.ACTION_GATT_INSUFFICIENT_ENCRYPTION);
        filter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        filter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESS);
        filter.addAction(BluetoothLeService.ACTION_WRITE_FAILED);
        filter.addAction(BluetoothLeService.ACTION_WRITE_COMPLETED);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        return filter;
    }

    public static IntentFilter makeOTADataFilter(){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE_V1);
        return filter;
    }

    /**
     * Adding the necessary Intent filters for 3rd party broadcast receivers
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter make3rdPartyIntentFilter(){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FOTA_BLE_CONNECTION_FAILED);
        filter.addAction(ACTION_FOTA_COULD_NOT_BE_STARTED);
        filter.addAction(ACTION_FOTA_FAIL);
        filter.addAction(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED);
        filter.addAction(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
        filter.addAction(ACTION_FOTA_POSSIBLE);
        filter.addAction(ACTION_FOTA_SUCCESS);
        filter.addAction(ACTION_FOTA_TIMEOUT);
        return filter;
    }

    /**
     * Adding the necessary Intent filters for Broadcast receiver in FotaApi
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter makeFotaApiIntentFilter(){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FOTA_FAIL);
        filter.addAction(ACTION_FOTA_SUCCESS);
        filter.addAction(ACTION_FOTA_TIMEOUT);
        return filter;
    }

    /**
     * Adding the necessary Intent filters for Broadcast receiver in FotaApi
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter makeAppModeIntentFilter(){
        final IntentFilter filter = makeGattUpdateIntentFilter();
        filter.addAction(ACTION_FOTA_DEVICE_BATTERY_READ);
        filter.addAction(ACTION_FOTA_DEVICE_INFO_READ);
        return filter;
    }

    /**
     * Adding the necessary Intent filters for Broadcast receiver in FotaApi
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter makeBootModeIntentFilter(){
        final IntentFilter filter = makeGattUpdateIntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        return filter;
    }

    /**
     * Adding the necessary Intent filters for Broadcast receiver in FotaApi
     *
     * @return {@link IntentFilter}
     */
    public static IntentFilter makeImmediateAlertIntentFilter(){
        final IntentFilter filter = makeGattUpdateIntentFilter();
        filter.addAction(ACTION_FOTA_FILE_DOWNLOADED);
        filter.addAction(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
        return filter;
    }

    /**
      * Check whether Internet connection is enabled on the device
      *
      * @param context
      * @return
      */
    public static final boolean checkNetwork(Context context) {
        if (context != null) {
            boolean result = true;
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
                result = false;
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * Check whether Wifi connection is enabled on the device
     *
     * @param context
     * @return
     */
    public static final boolean checkWifi(Context context) {
        if (context != null) {
            WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiMgr.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                if(wifiInfo.getNetworkId() == -1){
                    return false;
                }
                return true;
            }
            else {
                return false;
            }
        } else {
            return false;
        }

    }

    /**
     * Checks if the file is a .cyacd2 file
     * @param file
     * @return true if the file is a cyacd2
     */
    public static boolean isCyacd2File(String file) { //TODO: tests
        return file.matches(REGEX_MATCHES_CYACD2);
    }

    public static String byteArrayToHex(byte[] bytes) {
        if (bytes != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                // Previously was using the following line but it fires "JavaBinder: !!! FAILED BINDER TRANSACTION !!!" with TPUT 2M code example ...
//                sb.append(formatForRootLocale("%02X ", b));
                // ... hence rewrote the line above with the following two lines
                sb.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
                sb.append(Character.toUpperCase(Character.forDigit((b & 0xF), 16)) + " ");
            }
            return sb.toString();
        }
        return "";
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getMSB(String string) {
        StringBuilder msbString = new StringBuilder();
        for (int i = string.length(); i > 0; i -= 2) {
            String str = string.substring(i - 2, i);
            msbString.append(str);
        }
        return msbString.toString();
    }

    /**
     * Method to convert hex to byteArray
     */
    public static byte[] convertingToByteArray(String result) {
        String[] splitted = result.split("\\s+");
        byte[] valueByte = new byte[splitted.length];
        for (int i = 0; i < splitted.length; i++) {
            if (splitted[i].length() > 2) {
                String trimmedByte = splitted[i].split("x")[1];
                valueByte[i] = (byte) convertStringToByte(trimmedByte);
            }
        }
        return valueByte;
    }


    /**
     * Convert the string to byte
     *
     * @param string
     * @return
     */
    private static int convertStringToByte(String string) {
        return Integer.parseInt(string, 16);
    }

    /**
     * Returns the battery level information from the characteristics
     *
     * @param characteristics
     * @return {@link String}
     */
    public static String getBatteryLevel(BluetoothGattCharacteristic characteristics) {
        int batteryLevel = characteristics.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        return String.valueOf(batteryLevel);
    }

    /**
     * Returns the Alert level information from the characteristics
     *
     * @param characteristics
     * @return {@link String}
     */
    public static String getAlertLevel(BluetoothGattCharacteristic characteristics) {
        int alert_level = characteristics.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        return String.valueOf(alert_level);
    }

    /**
     * Returns the Transmission power information from the characteristic
     *
     * @param characteristics
     * @return {@link integer}
     */
    public static int getTransmissionPower(BluetoothGattCharacteristic characteristics) {
        int txPower = characteristics.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
        return txPower;
    }

    /**
     * Setting the shared preference with values provided as parameters
     *
     * @param context
     * @param key
     * @param value
     */
    public static final void setStringSharedPreference(Context context, String key, String value) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Returning the stored values in the shared preference with values provided
     * as parameters
     *
     * @param context
     * @param key
     * @return
     */
    public static final String getStringSharedPreference(Context context, String key) {
        if (context != null) {
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
            return pref.getString(key, "");
        }
        return "";
    }

    /**
     * Setting the shared preference with values provided as parameters
     *
     * @param context
     * @param key
     * @param value
     */
    public static final void setIntSharedPreference(Context context, String key, int value) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Returning the stored values in the shared preference with values provided
     * as parameters
     *
     * @param context
     * @param key
     * @return
     */
    public static final int getIntSharedPreference(Context context, String key) {
        if (context != null) {
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
            return pref.getInt(key, 0);
        }
        return 0;
    }

    public static final void setBooleanSharedPreference(Context context, String key, boolean value) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static final boolean getBooleanSharedPreference(Context context, String key) {
        SharedPreferences Preference = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return Preference.getBoolean(key, false);
    }

    public static final boolean containsSharedPreference(Context context, String key) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return pref.contains(key);
    }

    public static String formatForRootLocale(String format, Object... args) {
        return String.format(DATA_LOCALE, format, args);
    }


    /**
     * Read version name from the manifest
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            String packageName = context.getPackageName();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public static void debug(String msg, Object... objects) {
        StringBuilder sb = new StringBuilder(msg);
        for (Object o : objects) {
            sb.append(" " + o.getClass().getSimpleName() + "(" + System.identityHashCode(o) + ")");
        }
    }
}

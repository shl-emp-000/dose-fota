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

/**
 * Constants used in the project
 */
public class Constants {

    // The value of manifest.package in AndroidManifest.xml
    public static String PACKAGE_NAME;

    public static String DEVICE_BOOT_NAME = "BLE DFU Device";

    /**
     * Actions that can be broadcasted by fotalibrary that the 3rd party app should receive
     */
    public final static String ACTION_FOTA_BLE_CONNECTION_FAILED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_BLE_CONNECTION_FAILED";
    public final static String ACTION_FOTA_COULD_NOT_BE_STARTED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_COULD_NOT_BE_STARTED";
    public final static String ACTION_FOTA_FAIL =
            "com.innovationzed.fotalibrary.ACTION_FOTA_FAIL";
    public final static String ACTION_FOTA_FILE_DOWNLOAD_FAILED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_FILE_DOWNLOAD_FAILED";
    public final static String ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ";
    public final static String ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ";
    public final static String ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE";
    public final static String ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE";
    public final static String ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS";
    public final static String ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION";
    public final static String ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED";
    public final static String ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED";
    public final static String ACTION_FOTA_POSSIBLE =
            "com.innovationzed.fotalibrary.ACTION_FOTA_POSSIBLE";
    public final static String ACTION_FOTA_SUCCESS =
            "com.innovationzed.fotalibrary.ACTION_FOTA_SUCCESS";
    public final static String ACTION_FOTA_TIMEOUT =
            "com.innovationzed.fotalibrary.ACTION_FOTA_TIMEOUT";

    /**
     * Actions that is only used internally in FotaApi
     */
    public final static String ACTION_FOTA_DEVICE_BATTERY_READ =
            "com.innovationzed.fotalibrary.ACTION_FOTA_DEVICE_BATTERY_READ";
    public final static String ACTION_FOTA_DEVICE_INFO_READ =
            "com.innovationzed.fotalibrary.ACTION_FOTA_DEVICE_INFO_READ";
    public final static String ACTION_FOTA_FILE_DOWNLOADED =
            "com.innovationzed.fotalibrary.ACTION_FOTA_FILE_DOWNLOADED";


    /**
     * Extras Constants
     */
    public static final String EXTRA_MANUFACTURER_NAME = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_MANUFACTURER_NAME";
    public static final String EXTRA_MODEL_NUMBER = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_MODEL_NUMBER";
    public static final String EXTRA_SERIAL_NUMBER = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SERIAL_NUMBER";
    public static final String EXTRA_HARDWARE_REVISION = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_HARDWARE_REVISION";
    public static final String EXTRA_FIRMWARE_REVISION = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_FIRMWARE_REVISION";
    public static final String EXTRA_SOFTWARE_REVISION = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SOFTWARE_REVISION";
    public static final String EXTRA_PNP_ID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_PNP_ID";
    public static final String EXTRA_SYSTEM_ID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SYSTEM_ID";
    public static final String EXTRA_REGULATORY_CERTIFICATION_DATA_LIST = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_REGULATORY_CERTIFICATION_DATA_LIST";
    public static final String EXTRA_BTL_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BTL_VALUE";
    public static final String EXTRA_ALERT_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_ALERT_VALUE";
    public static final String EXTRA_POWER_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_POWER_VALUE";
    public static final String EXTRA_BYTE_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BYTE_VALUE";
    public static final String EXTRA_BYTE_UUID_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BYTE_UUID_VALUE";
    public static final String EXTRA_BYTE_INSTANCE_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BYTE_INSTANCE_VALUE";
    public static final String EXTRA_BYTE_SERVICE_UUID_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BYTE_SERVICE_UUID_VALUE";
    public static final String EXTRA_BYTE_SERVICE_INSTANCE_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BYTE_SERVICE_INSTANCE_VALUE";
    public static final String EXTRA_BYTE_DESCRIPTOR_INSTANCE_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BYTE_DESCRIPTOR_INSTANCE_VALUE";
    public static final String EXTRA_DESCRIPTOR_BYTE_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_DESCRIPTOR_BYTE_VALUE";
    public static final String EXTRA_DESCRIPTOR_BYTE_VALUE_UUID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_DESCRIPTOR_BYTE_VALUE_UUID";
    public static final String EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID";
    public static final String EXTRA_DESCRIPTOR_VALUE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_DESCRIPTOR_VALUE";
    public static final String EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID";
    public static final String EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE";
    public static final String EXTRA_CHARACTERISTIC_ERROR_MESSAGE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_CHARACTERISTIC_ERROR_MESSAGE";

    /**
     * Descriptor constants
     */
    public static final String FIRST_BIT_KEY_VALUE = "FIRST BIT VALUE KEY";
    public static final String SECOND_BIT_KEY_VALUE = "SECOND BIT VALUE KEY";
    public static final String EXTRA_SILICON_ID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SILICON_ID";
    public static final String EXTRA_SILICON_REV = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SILICON_REV";
    public static final String EXTRA_APP_VALID = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_APP_VALID";
    public static final String EXTRA_APP_ACTIVE = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_APP_ACTIVE";
    public static final String EXTRA_START_ROW = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_START_ROW";
    public static final String EXTRA_END_ROW = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_END_ROW";
    public static final String EXTRA_SEND_DATA_ROW_STATUS = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SEND_DATA_ROW_STATUS";
    public static final String EXTRA_PROGRAM_ROW_STATUS = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_PROGRAM_ROW_STATUS";
    public static final String EXTRA_VERIFY_ROW_STATUS = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_VERIFY_ROW_STATUS";
    public static final String EXTRA_VERIFY_ROW_CHECKSUM = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_VERIFY_ROW_CHECKSUM";
    public static final String EXTRA_VERIFY_CHECKSUM_STATUS = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_VERIFY_CHECKSUM_STATUS";
    public static final String EXTRA_SET_ACTIVE_APP = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_SET_ACTIVE_APP";
    public static final String EXTRA_VERIFY_APP_STATUS = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_VERIFY_APP_STATUS";
    public static final String EXTRA_VERIFY_EXIT_BOOTLOADER = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_VERIFY_EXIT_BOOTLOADER";
    public static final String EXTRA_ERROR_OTA = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_ERROR_OTA";

    //CYACD2 constants
    public static final String EXTRA_BTLDR_SDK_VER = "com.innovationzed.cysmart.backgroundservices." +
            "EXTRA_BTLDR_SDK_VER";

    /**
     * Shared Preference Status HandShake Status
     */
    public static final String PREF_BOOTLOADER_STATE = "PREF_BOOTLOADER_STATE";
    public static final String PREF_PROGRAM_ROW_NO = "PREF_PROGRAM_ROW_NO";
    public static final String PREF_PROGRAM_ROW_START_POS = "PREF_PROGRAM_ROW_START_POS";
    public static final String PREF_ARRAY_ID = "PREF_EXTRA_ARRAY_ID";
    /**
     * OTA File Selection Extras
     */
    public static final String ARRAYLIST_SELECTED_FILE_PATHS = "ARRAYLIST_SELECTED_FILE_PATHS";
    public static final String ARRAYLIST_SELECTED_FILE_NAMES = "ARRAYLIST_SELECTED_FILE_NAMES";
    public static final String EXTRA_ACTIVE_APP = "EXTRA_ACTIVE_APP";
    public static final byte ACTIVE_APP_NO_CHANGE = -1;
    public static final String EXTRA_SECURITY_KEY = "EXTRA_SECURITY_KEY";
    public static final long NO_SECURITY_KEY = -1;
    public static final int SECURITY_KEY_SIZE = 6;
    /**
     * Shared Preference Status File Status
     */
    public static final String PREF_OTA_FILE_ONE_NAME = "PREF_OTA_FILE_ONE_NAME";
    public static final String PREF_OTA_FILE_TWO_PATH = "PREF_OTA_FILE_TWO_PATH";
    public static final String PREF_OTA_FILE_TWO_NAME = "PREF_OTA_FILE_TWO_NAME";
    public static final String PREF_OTA_ACTIVE_APP_ID = "PREF_OTA_ACTIVE_APP_ID";
    public static final String PREF_OTA_SECURITY_KEY = "PREF_OTA_SECURITY_KEY";
    public static final String PREF_DEV_ADDRESS = "PREF_DEV_ADDRESS";
    public static final String PREF_IS_CYACD2_FILE = "PREF_IS_CYACD2_FILE";
    public static final String PREF_MTU_NEGOTIATED = "PREF_MTU_NEGOTIATED";
    /**
     * Shared Preference Status File Status
     */
    public static final String PREF_CLEAR_CACHE_ON_DISCONNECT = "PREF_CLEAR_CACHE_ON_DISCONNECT";
    public static final String PREF_UNPAIR_ON_DISCONNECT = "PREF_UNPAIR_ON_DISCONNECT";
    public static final boolean PREF_DEFAULT_UNPAIR_ON_DISCONNECT = false;


}

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
 * This class includes a subset of standard GATT attributes
 */
public class GattAttributes {

    /**
     * Services
     */
    public static final String DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static final String IMMEDIATE_ALERT_SERVICE = "00001802-0000-1000-8000-00805f9b34fb";
    public static final String LINK_LOSS_SERVICE = "00001803-0000-1000-8000-00805f9b34fb";
    public static final String TRANSMISSION_POWER_SERVICE = "00001804-0000-1000-8000-00805f9b34fb";
    public static final String HUMAN_INTERFACE_DEVICE_SERVICE = "00001812-0000-1000-8000-00805f9b34fb";
    public static final String OTA_UPDATE_SERVICE = "00060000-f8ce-11e4-abf4-0002a5d5c51b";

    /**
     * Device information characteristics
     */
    public static final String MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb";
    public static final String HARDWARE_REVISION = "00002a27-0000-1000-8000-00805f9b34fb";
    public static final String FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String SOFTWARE_REVISION = "00002a28-0000-1000-8000-00805f9b34fb";
    public static final String SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static final String REGULATORY_CERTIFICATION_DATA_LIST = "00002a2a-0000-1000-8000-00805f9b34fb";
    public static final String UUID_PNP_ID = "00002a50-0000-1000-8000-00805f9b34fb";
    /**
     * Battery characteristics
     */
    public static final String BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";

    /**
     * Gatt services
     */
    public static final String GENERIC_ACCESS_SERVICE = "00001800-0000-1000-8000-00805f9b34fb";
    public static final String GENERIC_ATTRIBUTE_SERVICE = "00001801-0000-1000-8000-00805f9b34fb";
    /**
     * Find me characteristics
     */
    public static final String ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb";
    public static final String TX_POWER_LEVEL = "00002a07-0000-1000-8000-00805f9b34fb";

    /**
     * HID Characteristics
     */
    public static final String REPORT = "00002a4d-0000-1000-8000-00805f9b34fb";
    /**
     * OTA Characteristic
     */
    public static final String OTA_CHARACTERISTIC = "00060001-f8ce-11e4-abf4-0002a5d5c51b";
    /**
     * Descriptor UUID's
     */
    public static final String CHARACTERISTIC_EXTENDED_PROPERTIES = "00002900-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_USER_DESCRIPTION = "00002901-0000-1000-8000-00805f9b34fb";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String SERVER_CHARACTERISTIC_CONFIGURATION = "00002903-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_PRESENTATION_FORMAT = "00002904-0000-1000-8000-00805f9b34fb";
    public static final String REPORT_REFERENCE = "00002908-0000-1000-8000-00805f9b34fb";
}
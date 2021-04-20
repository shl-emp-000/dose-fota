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

import java.util.UUID;

/**
 * This class will store the UUID of the GATT services and characteristics
 */
public class UUIDDatabase {

    /**
     * Device Information Service
     */
    public final static UUID UUID_DEVICE_INFORMATION_SERVICE = UUID
            .fromString(GattAttributes.DEVICE_INFORMATION_SERVICE);
    public static final UUID UUID_MANUFACTURER_NAME = UUID
            .fromString(GattAttributes.MANUFACTURER_NAME);
    public static final UUID UUID_MODEL_NUMBER = UUID
            .fromString(GattAttributes.MODEL_NUMBER);
    public static final UUID UUID_SERIAL_NUMBER = UUID
            .fromString(GattAttributes.SERIAL_NUMBER);
    public static final UUID UUID_HARDWARE_REVISION = UUID
            .fromString(GattAttributes.HARDWARE_REVISION);
    public static final UUID UUID_FIRMWARE_REVISION = UUID
            .fromString(GattAttributes.FIRMWARE_REVISION);
    public static final UUID UUID_SOFTWARE_REVISION = UUID
            .fromString(GattAttributes.SOFTWARE_REVISION);
    public final static UUID UUID_SYSTEM_ID = UUID
            .fromString(GattAttributes.SYSTEM_ID);
    public static final UUID UUID_REGULATORY_CERTIFICATION_DATA_LIST = UUID
            .fromString(GattAttributes.REGULATORY_CERTIFICATION_DATA_LIST);
    public static final UUID UUID_PNP_ID = UUID
            .fromString(GattAttributes.UUID_PNP_ID);

    /**
     * Battery Level related uuid
     */
    public final static UUID UUID_BATTERY_SERVICE = UUID
            .fromString(GattAttributes.BATTERY_SERVICE);
    public final static UUID UUID_BATTERY_LEVEL = UUID
            .fromString(GattAttributes.BATTERY_LEVEL);

    /**
     * Find me related uuid
     */
    public final static UUID UUID_IMMEDIATE_ALERT_SERVICE = UUID
            .fromString(GattAttributes.IMMEDIATE_ALERT_SERVICE);
    public final static UUID UUID_TRANSMISSION_POWER_SERVICE = UUID
            .fromString(GattAttributes.TRANSMISSION_POWER_SERVICE);
    public final static UUID UUID_ALERT_LEVEL = UUID
            .fromString(GattAttributes.ALERT_LEVEL);
    public final static UUID UUID_TRANSMISSION_POWER_LEVEL = UUID
            .fromString(GattAttributes.TX_POWER_LEVEL);
    public final static UUID UUID_LINK_LOSS_SERVICE = UUID
            .fromString(GattAttributes.LINK_LOSS_SERVICE);

    /**
     * RDK related UUID
     */
    public final static UUID UUID_REPORT = UUID
            .fromString(GattAttributes.REPORT);

    /**
     * OTA related UUID
     */
    public final static UUID UUID_OTA_UPDATE_SERVICE = UUID
            .fromString(GattAttributes.OTA_UPDATE_SERVICE);
    public final static UUID UUID_OTA_UPDATE_CHARACTERISTIC = UUID
            .fromString(GattAttributes.OTA_CHARACTERISTIC);

    /**
     * Descriptor UUID
     */
    public final static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID
            .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    public final static UUID UUID_CHARACTERISTIC_EXTENDED_PROPERTIES = UUID
            .fromString(GattAttributes.CHARACTERISTIC_EXTENDED_PROPERTIES);
    public final static UUID UUID_CHARACTERISTIC_USER_DESCRIPTION = UUID
            .fromString(GattAttributes.CHARACTERISTIC_USER_DESCRIPTION);
    public final static UUID UUID_SERVER_CHARACTERISTIC_CONFIGURATION = UUID
            .fromString(GattAttributes.SERVER_CHARACTERISTIC_CONFIGURATION);
    public final static UUID UUID_REPORT_REFERENCE = UUID
            .fromString(GattAttributes.REPORT_REFERENCE);
    public final static UUID UUID_CHARACTERISTIC_PRESENTATION_FORMAT = UUID
            .fromString(GattAttributes.CHARACTERISTIC_PRESENTATION_FORMAT);

    /**
     * GATT related UUID
     */
    public final static UUID UUID_GENERIC_ACCESS_SERVICE = UUID
            .fromString(GattAttributes.GENERIC_ACCESS_SERVICE);
    public final static UUID UUID_GENERIC_ATTRIBUTE_SERVICE = UUID
            .fromString(GattAttributes.GENERIC_ATTRIBUTE_SERVICE);

    /**
     * HID UUID
     */
    public final static UUID UUID_HID_SERVICE = UUID
            .fromString(GattAttributes.HUMAN_INTERFACE_DEVICE_SERVICE);


}

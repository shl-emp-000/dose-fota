package com.innovationzed.fotalibrary.BLEConnectionServices;

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

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;
import com.innovationzed.fotalibrary.CommonUtils.Utils;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static com.innovationzed.fotalibrary.FotaApi.ACTION_DEVICE_INFO_READ;

/**
 * Fragment to display the Device Information Service
 */
public class DeviceInformationService {

    private static BluetoothGattService mService;
    private Queue<BluetoothGattCharacteristic> mReadCharacteristics = new LinkedList<>();
    private Context mContext;

    private static int nbrCharacteristicsRead = 0;
    private final static int NBR_CHARACTERISTICS = 2; // TODO: should be 6 for real device, but example app only has 2

    // Data variables
    private static String mManufacturerName;
    private static String mModelNumber;
    private static String mSerialNumber;
    private static String mHardwareRevision;
    private static String mFirmwareRevision;
    private static String mSoftwareRevision;

    /**
     * BroadcastReceiver for receiving updates from the GATT server
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (extras.containsKey(Constants.EXTRA_MANUFACTURER_NAME)) {
                    mManufacturerName = intent.getStringExtra(Constants.EXTRA_MANUFACTURER_NAME);
                    ++nbrCharacteristicsRead;
                    readNextCharacteristic();
                }
                if (extras.containsKey(Constants.EXTRA_MODEL_NUMBER)) {
                    mModelNumber = intent.getStringExtra(Constants.EXTRA_MODEL_NUMBER);
                    ++nbrCharacteristicsRead;
                    readNextCharacteristic();
                }
                if (extras.containsKey(Constants.EXTRA_SERIAL_NUMBER)) {
                    mSerialNumber = intent.getStringExtra(Constants.EXTRA_SERIAL_NUMBER);
                    ++nbrCharacteristicsRead;
                    readNextCharacteristic();
                }
                if (extras.containsKey(Constants.EXTRA_HARDWARE_REVISION)) {
                    mHardwareRevision = intent.getStringExtra(Constants.EXTRA_HARDWARE_REVISION);
                    ++nbrCharacteristicsRead;
                    readNextCharacteristic();
                }
                if (extras.containsKey(Constants.EXTRA_FIRMWARE_REVISION)) {
                    mFirmwareRevision = intent.getStringExtra(Constants.EXTRA_FIRMWARE_REVISION);
                    ++nbrCharacteristicsRead;
                    readNextCharacteristic();
                }
                if (extras.containsKey(Constants.EXTRA_SOFTWARE_REVISION)) {
                    mSoftwareRevision = intent.getStringExtra(Constants.EXTRA_SOFTWARE_REVISION);
                    ++nbrCharacteristicsRead;
                    readNextCharacteristic();
                }

                // all characteristics read
                if (nbrCharacteristicsRead == NBR_CHARACTERISTICS){
                    BluetoothLeService.sendLocalBroadcastIntent(mContext, new Intent(ACTION_DEVICE_INFO_READ));
                }
            }
        }
    };

    public static DeviceInformationService create(BluetoothGattService service) {
        mService = service;
        return new DeviceInformationService();
    }

    public void startReadingDeviceInfo(Context context){
        mContext = context;
        mManufacturerName = "not found";
        mModelNumber = "not found";
        mSerialNumber = "not found";
        mHardwareRevision = "not found";
        mFirmwareRevision = "not found";
        mSoftwareRevision = "not found";
        BluetoothLeService.registerBroadcastReceiver(mContext, mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());
        getGattData();
    }

    public void stop(Context context){
        BluetoothLeService.unregisterBroadcastReceiver(context, mGattUpdateReceiver);
    }

    public static Dictionary getDeviceInformation(){
        Dictionary deviceInfo = new Hashtable();
        deviceInfo.put("ManufacturerName", mManufacturerName);
        deviceInfo.put("ModelNumber", mModelNumber);
        deviceInfo.put("SerialNumber", mSerialNumber);
        deviceInfo.put("HardwareRevision", mHardwareRevision);
        deviceInfo.put("FirmwareRevision", mFirmwareRevision);
        deviceInfo.put("SoftwareRevision", mSoftwareRevision);
        return deviceInfo;
    }

    private void getGattData() {
        collectReadCharacteristics();
        if (!mReadCharacteristics.isEmpty()) {
            readCharacteristic(mReadCharacteristics.peek());
        }
    }

    private void collectReadCharacteristics() {
        mReadCharacteristics.clear();
        List<BluetoothGattCharacteristic> characteristics = mService.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
            UUID uuid = characteristic.getUuid();
            if (uuid.equals(UUIDDatabase.UUID_MANUFACTURER_NAME)
                    || uuid.equals(UUIDDatabase.UUID_MODEL_NUMBER)
                    || uuid.equals(UUIDDatabase.UUID_SERIAL_NUMBER)
                    || uuid.equals(UUIDDatabase.UUID_HARDWARE_REVISION)
                    || uuid.equals(UUIDDatabase.UUID_FIRMWARE_REVISION)
                    || uuid.equals(UUIDDatabase.UUID_SOFTWARE_REVISION)) {
                int result = characteristic.getProperties();
                if ((result & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                    mReadCharacteristics.add(characteristic);
                }
            }
        }
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        BluetoothLeService.readCharacteristic(characteristic);
    }

    private void readNextCharacteristic() {
        mReadCharacteristics.poll();
        if (!mReadCharacteristics.isEmpty()) {
            readCharacteristic(mReadCharacteristics.peek());
        }
    }
}

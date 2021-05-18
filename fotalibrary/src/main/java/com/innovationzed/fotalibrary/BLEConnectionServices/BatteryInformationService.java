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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.FotaBroadcastReceiver;
import com.innovationzed.fotalibrary.CommonUtils.GattAttributes;
import com.innovationzed.fotalibrary.CommonUtils.Utils;

import java.util.List;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_BATTERY_READ;

/**
 * Class to read the battery level
 */
public class BatteryInformationService {

    private static BluetoothGattService mService;
    private static int mBatteryLevel = -1;
    private static boolean mHasRead = false;

    private FotaBroadcastReceiver mGattUpdateReceiver = new FotaBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Check for battery information
                if (extras.containsKey(Constants.EXTRA_BTL_VALUE) && !mHasRead) {
                    String received_btl_data = intent.getStringExtra(Constants.EXTRA_BTL_VALUE);
                    mBatteryLevel = Integer.parseInt(received_btl_data);
                    mHasRead = true;
                    BluetoothLeService.sendLocalBroadcastIntent(context, new Intent(ACTION_FOTA_DEVICE_BATTERY_READ));
                }
            }

        }

    };

    public static BatteryInformationService create(BluetoothGattService service) {
        mHasRead = false;
        mService = service;
        return new BatteryInformationService();
    }

    /**
     * Start reading battery info
     * @param context
     */
    public void startReadingBatteryInfo(Context context){
        mHasRead = false;
        BluetoothLeService.registerBroadcastReceiver(context, mGattUpdateReceiver, Utils.makeGattUpdateIntentFilter());
        getGattData();
    }

    /**
     * Stop listening for battery info
     * @param context
     */
    public void stopReadingBatteryInfo(Context context){
        BluetoothLeService.unregisterBroadcastReceiver(context, mGattUpdateReceiver);
    }

    /**
     * Gets the battery level, returns -1 if it has not been read
     * @return
     */
    public static int getBatteryLevel(){
        return mHasRead ? mBatteryLevel : -1;
    }


    /**
     * Method to get required characteristics from service
     */
    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService.getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(GattAttributes.BATTERY_LEVEL)) {
                BluetoothLeService.readCharacteristic(gattCharacteristic);
                break;
            }
        }
    }

}

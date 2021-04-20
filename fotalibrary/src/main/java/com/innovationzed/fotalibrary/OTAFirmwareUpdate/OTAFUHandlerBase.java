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

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;


public abstract class OTAFUHandlerBase implements OTAFUHandler {

//    protected final Fragment mFragment;
//    protected final View mView;
//    protected final TextView mProgressText;
    protected static Context mContext;

    protected final BluetoothGattCharacteristic mOtaCharacteristic;
    protected final String mFilepath;
    private final OTAFUHandlerCallback mParent;
    protected boolean mPrepareFileWriteEnabled = true;
    protected int mProgressBarPosition;
    protected byte mActiveApp; //Dual-App Bootloader Active Application ID
    protected long mSecurityKey;

    public OTAFUHandlerBase(Context context, BluetoothGattCharacteristic otaCharacteristic, byte activeApp, long securityKey, String filepath, OTAFUHandlerCallback parent) {
        this.mContext = context;
//        this.mView = view;
        this.mOtaCharacteristic = otaCharacteristic;
        this.mActiveApp = activeApp;
        this.mSecurityKey = securityKey;
        this.mFilepath = filepath;
        this.mParent = parent;
//        mProgressText = (TextView) mView.findViewById(R.id.file_status);
    }

    @Override
    public void setPrepareFileWriteEnabled(boolean enabled) {
        this.mPrepareFileWriteEnabled = enabled;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Resources getResources() {
        return null;//mActivity.getResources();
    }

    protected void startActivity(Intent intent) {
        mContext.startActivity(intent);
    }

    protected void showErrorDialogMessage(String errorMessage, final boolean stayOnPage) {
        mParent.showErrorDialogMessage(errorMessage, stayOnPage);
    }

    protected boolean isSecondFileUpdateNeeded() {
        return mParent.isSecondFileUpdateNeeded();
    }

    protected String storeAndReturnDeviceAddress() {
        return mParent.saveAndReturnDeviceAddress();
    }

    protected void setFileUpgradeStarted(boolean started) {
        mParent.setFileUpgradeStarted(started);
    }

}

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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.CommonUtils.CheckSumUtils;
import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.ConvertUtils;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.DataModelClasses.OTAFlashRowModel_v1;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FAIL;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_SUCCESS;

public class OTAFUHandler_v1 extends OTAFUHandlerBase {

    //NOTE: testing shows that there is no notable advantage of using SendDataWithoutResponse vs SendData
    private static final boolean USE_SEND_DATA_WITHOUT_RESPONSE = false;//Use SendDataWithoutResponse vs SendData
    public static final int SEND_DATA_WITHOUT_RESPONSE_DELAY = 250;//This delay allows the peripheral to complete processing of the SendDataWithoutResponse before starting processing the ProgramData

    private static final int SYNC_RETRY_LIMIT = 100;
    private static final int PROGRAM_RETRY_LIMIT = 10;
    private static final int FLOW_RETRY_LIMIT = 10;
    private static final String BEGIN = "OTAFUHandler_v1.BEGIN";

    private OTAFirmwareWrite_v1 mOtaFirmwareWrite;
    private Map<String, List<OTAFlashRowModel_v1>> mFileContents;
    private byte mCheckSumType;
    private final int mMaxDataSize;

    private int mSyncRetryNum;
    private int mProgramRetryNum;
    private int mFlowRetryNum;
    private boolean mReprogramCurrentRow;

    private Handler mOTATimeoutHandler; // A handler to run a repeating task to check
    private int mInterval = 30000; // Interval for running the timeout check
    private static final int TIME_LIMIT_OTA = 30000; // Time limit to get a response from the device during ota
    private static long timestampStatus;

    public OTAFUHandler_v1(Context context, BluetoothGattCharacteristic otaCharacteristic, String filepath, OTAFUHandlerCallback callback) {
        super(context, otaCharacteristic, Constants.ACTIVE_APP_NO_CHANGE, Constants.NO_SECURITY_KEY, filepath, callback);//AppId will be taken from the header line of the file
        //Prefer WriteNoResponse over WriteWithResponse
        if ((otaCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            this.mMaxDataSize = BootLoaderCommands_v1.WRITE_NO_RESP_MAX_DATA_SIZE;
        } else {
            this.mMaxDataSize = BootLoaderCommands_v1.WRITE_WITH_RESP_MAX_DATA_SIZE;
        }
    }

    @Override
    public void prepareFileWrite() {
        mOtaFirmwareWrite = new OTAFirmwareWrite_v1(mOtaCharacteristic);
        final CustomFileReader_v1 customFileReader = new CustomFileReader_v1(mFilepath);
        /**
         * Reads the file content and provides a 1 second delay
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mFileContents = customFileReader.readLines();
                    startOTA();
                } catch (CustomFileReader_v1.InvalidFileFormatException e) {
                    OTAFinished(ACTION_FOTA_FAIL, "Invalid firmware file");
                }
            }
        }, 1000);
    }

    private void startOTA() {
        setFileUpgradeStarted(true);
        mOTATimeoutHandler = new Handler();
        timestampStatus = System.currentTimeMillis();
        startRepeatingTask();

        mSyncRetryNum = mProgramRetryNum = mFlowRetryNum = 0;
        mReprogramCurrentRow = false;
        processOTAStatus(BEGIN, new Bundle());
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if (!checkTimeSinceLastOTAStatus()){
                    OTAFinished(ACTION_FOTA_FAIL, "Timeout/device disconnected");
                } else {
                    mOTATimeoutHandler.postDelayed(mStatusChecker, mInterval);
                }
            } catch (Exception e) {
                OTAFinished(ACTION_FOTA_FAIL, "Fota failed during timeout check: " + e.getMessage());
            }
        }
    };

    private boolean checkTimeSinceLastOTAStatus(){
        if (System.currentTimeMillis() - timestampStatus > TIME_LIMIT_OTA){
            return false;
        }
        return true;
    }

    private void startRepeatingTask() {
        mStatusChecker.run();
    }

    private void stopRepeatingTask() {
        mOTATimeoutHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void processOTAStatus(String status, Bundle extras) {
        timestampStatus = System.currentTimeMillis();
        if (extras.containsKey(Constants.EXTRA_ERROR_OTA) && FLOW_RETRY_LIMIT > mFlowRetryNum) {
            if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.SYNC) || status.equalsIgnoreCase(BootLoaderCommands_v1.POST_SYNC_ENTER_BOOTLOADER)) {
                if (SYNC_RETRY_LIMIT > mSyncRetryNum) {
                    ++mSyncRetryNum;
                } else {
                    // Fail
                    OTAFinished(ACTION_FOTA_FAIL, "Error in processOTAStatus: " + extras.getString(Constants.EXTRA_ERROR_OTA));
                    return;
                }
            } else if ((status.equalsIgnoreCase("" + BootLoaderCommands_v1.SEND_DATA)
                    || status.equalsIgnoreCase("" + BootLoaderCommands_v1.PROGRAM_DATA)) && PROGRAM_RETRY_LIMIT > mProgramRetryNum) {
                mReprogramCurrentRow = true;
                ++mProgramRetryNum;
                final int rowNum = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO); // Get current row
            } else {
                mReprogramCurrentRow = false;
                ++mFlowRetryNum;
            }

            // Send SYNC(unacknowledged) ...
            BluetoothLeService.setSyncCommandFlag(true);
            mOtaFirmwareWrite.OTASyncCmd(mCheckSumType);
            Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.SYNC);
            return;
        }

        // Fail
        if (extras.containsKey(Constants.EXTRA_ERROR_OTA)) {
            OTAFinished(ACTION_FOTA_FAIL, "Error in processOTAStatus: " + extras.getString(Constants.EXTRA_ERROR_OTA));
            return;
        }

        // ... followed by ENTER_BOOTLOADER
        if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.SYNC)) {
            //Send Enter Bootloader command
            OTAFlashRowModel_v1.Header headerRow = (OTAFlashRowModel_v1.Header) mFileContents.get(CustomFileReader_v1.KEY_HEADER).get(0);
            mOtaFirmwareWrite.OTAEnterBootLoaderCmd(mCheckSumType, headerRow.mProductId);
            Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, BootLoaderCommands_v1.POST_SYNC_ENTER_BOOTLOADER);
            return;
        }

        mSyncRetryNum = 0;

        if (status.equalsIgnoreCase(BootLoaderCommands_v1.POST_SYNC_ENTER_BOOTLOADER)) {
            if (mReprogramCurrentRow) {
                mReprogramCurrentRow = false;
                // Re-send failed row
                Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0); // Start with position 0 in the row
                final int rowNum = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO); // Get current row
                writeEivOrData(rowNum);
                return;
            } else {
                Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO, 0); // Start with row 0
                Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0); // Start with position 0 in the row
                status = BEGIN;
            }
        }

        if (status.equalsIgnoreCase(BEGIN)) {
            /**
             * Always start the programming from the first line
             */
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO, 0);
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0);

            OTAFlashRowModel_v1.Header headerRow = (OTAFlashRowModel_v1.Header) mFileContents.get(CustomFileReader_v1.KEY_HEADER).get(0);
            this.mCheckSumType = headerRow.mCheckSumType;
            this.mActiveApp = headerRow.mAppId;

            //Send Enter Bootloader command
            mOtaFirmwareWrite.OTAEnterBootLoaderCmd(mCheckSumType, headerRow.mProductId);
            Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.ENTER_BOOTLOADER);
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.ENTER_BOOTLOADER)) {
            if (extras.containsKey(Constants.EXTRA_SILICON_ID) && extras.containsKey(Constants.EXTRA_SILICON_REV)) {
                OTAFlashRowModel_v1.Header headerRow = (OTAFlashRowModel_v1.Header) mFileContents.get(CustomFileReader_v1.KEY_HEADER).get(0);
                byte[] siliconIdReceived = extras.getByteArray(Constants.EXTRA_SILICON_ID);
                byte siliconRevReceived = extras.getByte(Constants.EXTRA_SILICON_REV);

                if (Arrays.equals(headerRow.mSiliconId, siliconIdReceived) && headerRow.mSiliconRev == siliconRevReceived) {
                    //Send Set Application Metadata command
                    OTAFlashRowModel_v1.AppInfo appInfoRow = (OTAFlashRowModel_v1.AppInfo) mFileContents.get(CustomFileReader_v1.KEY_APPINFO).get(0);

                    mOtaFirmwareWrite.OTASetAppMetadataCmd(mCheckSumType, mActiveApp, appInfoRow.mAppStart, appInfoRow.mAppSize);
                    Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.SET_APP_METADATA);
                } else {
                    //Wrong SiliconId and SiliconRev
                    OTAFinished(ACTION_FOTA_FAIL, "Error: The SiliconID or SiliconRev does not match");
                    return;
                }
            } else {
                //No SiliconId and SiliconRev
                if (FLOW_RETRY_LIMIT > mFlowRetryNum) {
                    extras.putString(Constants.EXTRA_ERROR_OTA, "CYRET_ERR_UNK"); //Emulate error
                    processOTAStatus(BEGIN, extras); //Re-try complete flow
                    return;
                } else {
                    OTAFinished(ACTION_FOTA_FAIL, "Error: Target returned no SiliconID");
                    return;
                }
            }
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.SET_APP_METADATA)) {
            int rowNum = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO);
            writeEivOrData(rowNum);
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.SET_EIV)) {
            programNextRow();
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.SEND_DATA)) {
            int rowNum = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO);
            writeData(rowNum);//Program data row
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.SEND_DATA_WITHOUT_RESPONSE)) {
            int rowNum = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO);
            writeData(rowNum);//Program data row
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.PROGRAM_DATA)) {
            programNextRow();
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.VERIFY_APP)) {
            if (extras.containsKey(Constants.EXTRA_VERIFY_APP_STATUS)) {
                byte statusReceived = extras.getByte(Constants.EXTRA_VERIFY_APP_STATUS);
                if (statusReceived == 1) {
                    //Send ExitBootloader command
                    mOtaFirmwareWrite.OTAExitBootloaderCmd(mCheckSumType);
                    Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.EXIT_BOOTLOADER);
                } else {
                    if (FLOW_RETRY_LIMIT < mFlowRetryNum) {
                        mFlowRetryNum = FLOW_RETRY_LIMIT - 1; //Only retry once if this step fails
                        extras.putString(Constants.EXTRA_ERROR_OTA, "CYRET_ERR_UNK"); //Emulate error
                        processOTAStatus(BEGIN, extras); //Re-try complete flow
                        return;
                    } else {
                        OTAFinished(ACTION_FOTA_FAIL, "Error: Verification failed for the programmed application");
                        return;
                    }
                }
            }
        } else if (status.equalsIgnoreCase("" + BootLoaderCommands_v1.EXIT_BOOTLOADER)) {
            BluetoothDevice device = BluetoothLeService.getRemoteDevice();
            setFileUpgradeStarted(false);
            storeAndReturnDeviceAddress();
            BluetoothLeService.disconnect();
            BluetoothLeService.unpairDevice(device);

            OTAFinished(ACTION_FOTA_SUCCESS, "Successful firmware update.");
        }
    }

    private void resetSharedPreferences(){
        Utils.setStringSharedPreference(mContext, Constants.PREF_OTA_FILE_ONE_NAME, "Default");
        Utils.setStringSharedPreference(mContext, Constants.PREF_OTA_FILE_TWO_PATH, "Default");
        Utils.setStringSharedPreference(mContext, Constants.PREF_OTA_FILE_TWO_NAME, "Default");
        Utils.setStringSharedPreference(mContext, Constants.PREF_OTA_ACTIVE_APP_ID, "Default");
        Utils.setStringSharedPreference(mContext, Constants.PREF_OTA_SECURITY_KEY, "Default");
        Utils.setStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE, "Default");
        Utils.setIntSharedPreference(mContext, Constants.PREF_PROGRAM_ROW_NO, 0);
    }

    private void OTAFinished(String action, String reason){
        resetSharedPreferences();
        stopRepeatingTask();
        Utils.broadcastOTAFinished(mContext, action, reason, !action.equals(ACTION_FOTA_SUCCESS)); // if it's not a success then it's in boot mode
    }

    private void programNextRow() {
        int rowNum = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO);
        rowNum++;//Increment row number
        mProgramRetryNum = 0;
        List<OTAFlashRowModel_v1> dataRows = mFileContents.get(CustomFileReader_v1.KEY_DATA);
        //Update progress bar
        int totalLines = dataRows.size();
        if (rowNum < totalLines) {//Process next row
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO, rowNum);
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
            writeEivOrData(rowNum);
        }
        if (rowNum == totalLines) {//All rows have been processed
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_NO, 0);
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
            //Programming done, send VerifyApplication command
            mOtaFirmwareWrite.OTAVerifyAppCmd(mCheckSumType, mActiveApp);
            Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.VERIFY_APP);
        }
    }

    private void writeEivOrData(int rowNum) {
        List<OTAFlashRowModel_v1> dataRows = mFileContents.get(CustomFileReader_v1.KEY_DATA);
        OTAFlashRowModel_v1 dataRow = dataRows.get(rowNum);
        if (dataRow instanceof OTAFlashRowModel_v1.EIV) {
            writeEiv(rowNum);//Set EIV
        } else if (dataRow instanceof OTAFlashRowModel_v1.Data) {
            writeData(rowNum);//Program data row
        }
    }

    private void writeData(int rowNum) {
        int startPosition = Utils.getIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS);
        OTAFlashRowModel_v1.Data dataRow = (OTAFlashRowModel_v1.Data) mFileContents.get(CustomFileReader_v1.KEY_DATA).get(rowNum);

        int payloadLength = dataRow.mData.length - startPosition;
        boolean isLastPacket = payloadLength <= mMaxDataSize;
        if (!isLastPacket) {
            payloadLength = mMaxDataSize;
        }
        byte[] payload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
            byte b = dataRow.mData[startPosition];
            payload[i] = b;
            startPosition++;
        }
        if (!isLastPacket) {
            if (USE_SEND_DATA_WITHOUT_RESPONSE) {
                //Send SendDataWithoutResponse command
                mOtaFirmwareWrite.OTASendDataWithoutResponseCmd(mCheckSumType, payload);
                Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.SEND_DATA_WITHOUT_RESPONSE);
                Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, startPosition);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //The following line is necessary as there is no response for SendDataWithoutResponse command
                        Intent otaStatusV1Intent = new Intent(BluetoothLeService.ACTION_OTA_STATUS_V1);
                        Bundle bundle = new Bundle();
                        otaStatusV1Intent.putExtras(bundle);
                        BluetoothLeService.sendGlobalBroadcastIntent(getContext(), otaStatusV1Intent);
                    }
                }, SEND_DATA_WITHOUT_RESPONSE_DELAY);
            } else {
                //Send SendData command
                mOtaFirmwareWrite.OTASendDataCmd(mCheckSumType, payload);
                Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.SEND_DATA);
                Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, startPosition);
            }
        } else {
            //Send ProgramData command
            long crc32 = CheckSumUtils.crc32(dataRow.mData, dataRow.mData.length);
            byte[] baCrc32 = ConvertUtils.intToByteArray((int) crc32);
            mOtaFirmwareWrite.OTAProgramDataCmd(mCheckSumType, dataRow.mAddress, baCrc32, payload);
            Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.PROGRAM_DATA);
            Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
        }
    }

    private void writeEiv(int rowNum) {
        OTAFlashRowModel_v1.EIV eivRow = (OTAFlashRowModel_v1.EIV) mFileContents.get(CustomFileReader_v1.KEY_DATA).get(rowNum);
        //Send SetEiv command
        mOtaFirmwareWrite.OTASetEivCmd(mCheckSumType, eivRow.mEiv);
        Utils.setStringSharedPreference(getContext(), Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands_v1.SET_EIV);
        Utils.setIntSharedPreference(getContext(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
    }
}

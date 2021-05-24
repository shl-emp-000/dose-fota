package com.example.fotaapplication;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.FotaApi;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_BLE_CONNECTION_FAILED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_COULD_NOT_BE_STARTED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FAIL;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_PERMISSION_FINE_LOCATION = 2;
    private static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 3;

    private FotaApi mFotaApi;
    private static boolean mUserWantsToUpdate = false;
    private static boolean mFotaInProgress = false;
    private static String mCurrentText = "";

    // Receiver for the possible actions that can be broadcasted from the FotaApi
    private BroadcastReceiver mOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_FOTA_BLE_CONNECTION_FAILED)){
                    setTextInformation("Bluetooth error: was not able to connect or find the needed services.");
                } else if (action.equals(ACTION_FOTA_COULD_NOT_BE_STARTED)){
                    setTextInformation("Firmware update could not be started: isFirmwareUpdatePossible() has not returned ACTION_FOTA_POSSIBLE, user did not approve update or device isn't paired and connected.");
                } else if (action.equals(ACTION_FOTA_FAIL)){
                    setTextInformation("Firmware update failed.");
                } else if (action.equals(ACTION_FOTA_FILE_DOWNLOAD_FAILED)){
                    setTextInformation("Downloading the firmware file failed.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ)){
                    setTextInformation("The battery level of the device could not be read.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ)){
                    setTextInformation("The device information could not be read.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE)){
                    setTextInformation("The device battery is too low.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE)){
                    setTextInformation("The phone battery is too low.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS)){
                    setTextInformation("The device has the latest firmware installed already.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION)){
                    setTextInformation("No wifi connection.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED)){
                    setTextInformation("Required permissions are not granted.");
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED)){
                    setTextInformation("The version check failed.");
                } else if (action.equals(ACTION_FOTA_POSSIBLE)){
                    setTextInformation("Firmware update is possible.");
                } else if (action.equals(ACTION_FOTA_SUCCESS)){
                    setTextInformation("Firmware update is finished.");
                } else if (action.equals(ACTION_FOTA_TIMEOUT)){
                    setTextInformation("Timeout.");
                }
                mFotaInProgress = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fota_fragment);

        // The checks below can be done in any way the 3rd party app prefers,
        // but the following permissions need to be granted:
        // - Manifest.permission.ACCESS_FINE_LOCATION
        // - Manifest.permission.READ_EXTERNAL_STORAGE
        // - Manifest.permission.WRITE_EXTERNAL_STORAGE
        checkLocationPermission();
        checkStoragePermission();


        // Anders MAC 00:A0:50:BA:CC:CE
        // Emmy MAC 00:A0:50:B4:42:33
        // DOSE v5 MAC 00:A0:50:E2:65:48
        mFotaApi = new FotaApi(this, "00:A0:50:B4:42:33"); // MAC address is hardcoded at this point

        // Register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mOTAStatusReceiver, Utils.make3rdPartyIntentFilter());

        // Attach onClickListeners
        ((Button)findViewById(R.id.buttonMacAddress)).setOnClickListener(this);
        ((Button)findViewById(R.id.buttonPossible)).setOnClickListener(this);
        ((Button)findViewById(R.id.buttonUserConfirmation)).setOnClickListener(this);
        ((Button)findViewById(R.id.buttonFota)).setOnClickListener(this);

        setTextInformation(mCurrentText);
    }

    @Override
    protected void onResume() {
        TextView textView = (TextView)findViewById(R.id.textViewMacAddress);
        textView.requestFocus();
        setTextInformation(mCurrentText);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy(){
        // Unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mOTAStatusReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonMacAddress:
                // This button is just for testing
                EditText mac = (EditText) findViewById(R.id.editTextMacAddress);
                String macString = mac.getText().toString();
                if (BluetoothAdapter.checkBluetoothAddress(macString)){
                    mFotaApi.changeDevice(macString);
                    setTextInformation("Device was set to " + macString);
                } else {
                    setTextInformation("Device not changed, enter a valid MAC address.");
                }
                InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                break;
            case R.id.buttonPossible:
                mFotaApi.isFirmwareUpdatePossible();
                setTextInformation("Checking if firmware update is possible...");
                break;
            case R.id.buttonUserConfirmation:
                //TODO: replace the line below with actual code for asking the user if they want to update
                mUserWantsToUpdate = true;
                setTextInformation("User has " + (mUserWantsToUpdate ? "" : "not ") + "confirmed that they want to update.");
                break;
            case R.id.buttonFota:
                setTextInformation("Firmware upgrade in progress...");
                mFotaInProgress = true;
                mFotaApi.doFirmwareUpdate(mUserWantsToUpdate);
                break;
            default:
                break;
        }
        TextView textView = (TextView)findViewById(R.id.textViewMacAddress);
        textView.requestFocus();
    }

    private void setTextInformation(String s){
        mCurrentText = s;
        ((TextView)findViewById(R.id.text_info)).setText(s);
    }

    private boolean checkLocationPermission() {
        // Since Marshmallow either ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission is required for BLE scan

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Grant permission to access Location Service
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert_message_permission_required_title)
                        .setMessage(R.string.alert_message_location_permission_required_message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (this != null) {
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION);
                                    }
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
                return false;
            }
        }
        return true;
    }

    private boolean checkStoragePermission() {
        // Since Marshmallow Read/Write access to Storage need to be requested

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert_message_permission_required_title)
                        .setMessage(R.string.alert_message_storage_permission_required_message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                if (this != null) {
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_EXTERNAL_STORAGE);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                dialog.cancel();
                            }
                        });
                builder.show();
                return false;
            }
        }
        return true;

    }
}
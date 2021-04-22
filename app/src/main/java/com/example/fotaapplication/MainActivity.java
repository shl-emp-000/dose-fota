package com.example.fotaapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.FotaApi;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_PERMISSION_FINE_LOCATION = 2;
    private static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 3;

    private FotaApi mFotaApi;
    private boolean mFirmwareUpdatePossible;
    private boolean mUserWantsToUpdate;

    // Receiver for the 4 possible actions that can be broadcasted from the FotaApi
    private BroadcastReceiver mOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(BluetoothLeService.ACTION_OTA_SUCCESS)){
                    setTextInformation("Firmware update is finished.");
                } else if (action.equals(BluetoothLeService.ACTION_OTA_FAIL)){
                    setTextInformation("Firmware update failed.");
                } else if (action.equals(BluetoothLeService.ACTION_OTA_IS_POSSIBLE)){
                    mFirmwareUpdatePossible = true;
                    setTextInformation("Firmware update is possible.");
                } else if (action.equals(BluetoothLeService.ACTION_OTA_IS_NOT_POSSIBLE)){
                    setTextInformation("Firmware update is not possible.");
                }
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

        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        startService(gattServiceIntent);

        // Register receiver
        BluetoothLeService.registerBroadcastReceiver(this, mOTAStatusReceiver, Utils.makeOTAIntentFilter());

        // Anders MAC 00:A0:50:BA:CC:CE
        mFotaApi = new FotaApi(this, "00:A0:50:B4:42:33"); // MAC address is hardcoded at this point
        mFirmwareUpdatePossible = false;
        mUserWantsToUpdate = false;

        // Attach onClickListeners
        ((Button)findViewById(R.id.buttonPossible)).setOnClickListener(this);
        ((Button)findViewById(R.id.buttonUserConfirmation)).setOnClickListener(this);
        ((Button)findViewById(R.id.buttonFota)).setOnClickListener(this);

    }

    @Override
    public void onDestroy(){
        // Unregister receiver
        BluetoothLeService.unregisterBroadcastReceiver(this, mOTAStatusReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
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
                if (mFirmwareUpdatePossible){
                    setTextInformation("Firmware upgrade in progress...");
                    mFotaApi.doFirmwareUpdate(mUserWantsToUpdate);
                }
                break;
            default:
                break;
        }
    }

    private void setTextInformation(String s){
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
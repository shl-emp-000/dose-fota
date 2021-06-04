package com.example.fotaapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements  SelectFwServerDialogFragment.SelectFwServerDialogListener {

    public static final int SCANNER_FRAGMENT = 1;
    public static final int UPDATE_FRAGMENT = 2;
    public static int currentFragment = SCANNER_FRAGMENT;

    private static final int REQUEST_PERMISSION_FINE_LOCATION = 2;
    private static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The checks below can be done in any way the 3rd party app prefers,
        // but the following permissions need to be granted:
        // - Manifest.permission.ACCESS_FINE_LOCATION
        // - Manifest.permission.READ_EXTERNAL_STORAGE
        // - Manifest.permission.WRITE_EXTERNAL_STORAGE
        checkLocationPermission();
        checkStoragePermission();

        String version = BuildConfig.VERSION_NAME;
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.app_name) + " (v" + version + ")");

        if (currentFragment == UPDATE_FRAGMENT) {
            CommonUtils.replaceFragment(this, new UpdateFragment(), getString(R.string.update_fragment_tag), true);
        } else {
            CommonUtils.replaceFragment(this, new ScannerFragment(), getString(R.string.scanner_fragment_tag));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
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

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the SelectFwServerDialogFragment.SelectFwServerDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment dialog)
    {
        SelectFwServerDialogFragment d = (SelectFwServerDialogFragment) dialog;
        ArrayList<FirmwareServer> serverArray = new ArrayList<>();
        UpdateFragment fragment = (UpdateFragment) this.getSupportFragmentManager().findFragmentByTag(getString(R.string.update_fragment_tag));
        Toast toast;

        if (null == fragment) {
            toast = Toast.makeText(this, "Something went wrong, server not changed!", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (d.getSelectedItem() < 0) {
            // No item was selected, go with hardcoded server
            fragment.setFirmwareServer("","");
            ((TextView) this.findViewById(R.id.textSelectedFirmwareServer)).setText("Default");
            toast = Toast.makeText(this, "No server selected, using the default!", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            // Load servers from preference
            SharedPreferences prefs = this.getSharedPreferences(getString(R.string.preferences_file_fw_servers), Context.MODE_PRIVATE);

            try {
                serverArray = (ArrayList<FirmwareServer>) ObjectSerializer.deserialize(prefs.getString(getString(R.string.preferences_fw_servers_key), ObjectSerializer.serialize(new ArrayList<FirmwareServer>())));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            String urlAddress = serverArray.get(d.getSelectedItem()).getServerAddress();

            if (Patterns.WEB_URL.matcher(urlAddress).matches()) {
                fragment.setFirmwareServer(urlAddress, serverArray.get(d.getSelectedItem()).getServerSigningKey());
                ((TextView) this.findViewById(R.id.textSelectedFirmwareServer)).setText(serverArray.get(d.getSelectedItem()).getServerAddress());
                toast = Toast.makeText(this, "Successfully changed server!", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                toast = Toast.makeText(this, "Discarded server change, Invalid URL!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
        Toast toast = Toast.makeText(this, "Discarded server change!", Toast.LENGTH_SHORT);
        toast.show();
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
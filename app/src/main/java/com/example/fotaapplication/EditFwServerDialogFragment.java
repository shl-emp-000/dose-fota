package com.example.fotaapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditFwServerDialogFragment extends DialogFragment {
    public static final int PICKFILE_RESULT_CODE = 1;

    private Button btnChooseFile;
    private TextView tvItemPath;

    private Uri fileUri;
    private String filePath;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.edit_fw_server, null);
        btnChooseFile = (Button) v.findViewById(R.id.buttonBrowse);
        tvItemPath = (TextView) v.findViewById(R.id.tvFilePath);

        btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("text/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
            }
        });
        builder.setTitle("Upload new server list:")
                .setView(v)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ArrayList<FirmwareServer> serverArray = getFirmwareServerList();

                        Toast toast;
                        if (serverArray.isEmpty()) {
                            toast = Toast.makeText(getActivity(), "Unable to parse the selected file!", Toast.LENGTH_SHORT);
                        } else {
                            // Save the server list to preference
                            SharedPreferences prefs = getContext().getSharedPreferences(getString(R.string.preferences_file_fw_servers), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            try {
                                editor.putString(getString(R.string.preferences_fw_servers_key), ObjectSerializer.serialize((Serializable) serverArray));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            editor.commit();
                            toast = Toast.makeText(getActivity(), "Successfully updated the server list!", Toast.LENGTH_SHORT);
                        }
                        toast.show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        Toast toast = Toast.makeText(getActivity(), "Discarded changes to the server list!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_RESULT_CODE) { // Step 1: When a result has been received, check if it is the result for READ_IN_FILE
            if (resultCode == Activity.RESULT_OK) { // Step 2: Check if the operation to retrieve the activity's result is successful
                fileUri = data.getData();
                filePath = getRealPathFromUri();
                tvItemPath.setText(filePath);
            }
        }
    }


    /**
     * Helper function to get the real file path from the Uri
     * @return String with the real path of the selected file
     */
    private String getRealPathFromUri() {
        Cursor cursor = null;
        String filePath = "";
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getContext().getContentResolver().query(fileUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(column_index);
        } catch (Exception e) {
            Log.d("EditFwServerDialog", "getRealPathFromUri Exception : " + e.toString());
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Could not get file path so get at least the file name. This happens if user choose file from recent files
        if (null == filePath || filePath.isEmpty()) {
            try {
                String[] proj = { MediaStore.Images.Media.DISPLAY_NAME };
                cursor = getContext().getContentResolver().query(fileUri, proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                cursor.moveToFirst();
                filePath = cursor.getString(column_index);
            } catch (Exception e) {
                Log.d("EditFwServerDialog", "getRealPathFromUri Exception : " + e.toString());
                return "";
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return filePath;
    }

    /**
     * Helper function to parse selected file
     */
    private ArrayList<FirmwareServer> getFirmwareServerList() {
        ArrayList<FirmwareServer> serverArray = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getActivity().getContentResolver().openInputStream(fileUri)));
            String line = reader.readLine();
            while (line != null) {
                String[] splitLine = line.split("[, ]+");
                serverArray.add(new FirmwareServer(splitLine[0], splitLine[1]));
                line = reader.readLine();
            }
            reader.close();
        } catch(IOException ioe){
            // Unable to parse file
            Log.d("FwServerDialog", "Unable to parse server file: " + ioe.toString());
        }

        return serverArray;
    }
}

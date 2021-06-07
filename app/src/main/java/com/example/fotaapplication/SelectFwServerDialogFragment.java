package com.example.fotaapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.io.IOException;
import java.util.ArrayList;

public class SelectFwServerDialogFragment extends DialogFragment {
    private int mSelectedItem = -1;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface SelectFwServerDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    SelectFwServerDialogListener listener;

    // Override the Fragment.onAttach() method to instantiate the SelectFwServerDialogListener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the SelectFwServerDialogListener so we can send events to the host
            listener = (SelectFwServerDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement SelectFwServerDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ArrayList<FirmwareServer> serverArray = new ArrayList<>();
        ArrayList<String> serverAddresses = new ArrayList<>();

        // Load servers from preference
        SharedPreferences prefs = getContext().getSharedPreferences(getString(R.string.preferences_file_fw_servers), Context.MODE_PRIVATE);

        try {
            serverArray = (ArrayList<FirmwareServer>) ObjectSerializer.deserialize(prefs.getString(getString(R.string.preferences_fw_servers_key), ObjectSerializer.serialize(new ArrayList<FirmwareServer>())));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < serverArray.size(); ++i) {
            serverAddresses.add(serverArray.get(i).getServerAddress());
        }

        // Convert String list to CharSequence
        final CharSequence[] charSeqList = serverAddresses.toArray(new CharSequence[serverAddresses.size()]);

        mSelectedItem = -1;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle(getString(R.string.fw_server_select))
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(charSeqList, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSelectedItem = which;
                            }
                        })
                // Set the action buttons
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the selectedItems results somewhere
                        // or return them to the component that opened the dialog
                        listener.onDialogPositiveClick(SelectFwServerDialogFragment.this);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mSelectedItem = -1;
                        listener.onDialogNegativeClick(SelectFwServerDialogFragment.this);
                    }
                });

        return builder.create();
    }

    public int getSelectedItem() {
        return mSelectedItem;
    }
}

package com.example.fotaapplication;

import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.innovationzed.fotalibrary.CommonUtils.Utils;
import com.innovationzed.fotalibrary.FotaApi;

import static com.example.fotaapplication.MainActivity.UPDATE_FRAGMENT;
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
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_PROGRESS_UPDATE;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_SUCCESS;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_TIMEOUT;
import static com.innovationzed.fotalibrary.CommonUtils.Utils.OTA_PROGRESS;


public class UpdateFragment extends Fragment implements View.OnClickListener {

    private FotaApi mFotaApi;
    private static String mCurrentText = "";
    private TableLayout mFirmwareTableLayout;
    private TableLayout mDeviceDetailsTableLayout;
    private EditFwServerDialogFragment mEditFwServerDialog;
    private SelectFwServerDialogFragment mSelectFwServerDialog;
    private ProgressBar mFotaProgressBar;
    private TextView mFotaProgressPercent;
    private View mView;

    // Receiver for the possible actions that can be broadcasted from the FotaApi
    private BroadcastReceiver mOTAStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String action = intent.getAction();
                if (action.equals(ACTION_FOTA_BLE_CONNECTION_FAILED)){
                    setTextInformation(getString(R.string.fota_ble_connection_failed));
                } else if (action.equals(ACTION_FOTA_COULD_NOT_BE_STARTED)){
                    setTextInformation(getString(R.string.fota_could_not_start));
                } else if (action.equals(ACTION_FOTA_FAIL)){
                    setTextInformation(getString(R.string.fota_failed));
                } else if (action.equals(ACTION_FOTA_FILE_DOWNLOAD_FAILED)){
                    setTextInformation(getString(R.string.fota_file_download_failed));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ)){
                    setTextInformation(getString(R.string.fota_not_possible_device_battery_not_read));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ)){
                    setTextInformation(getString(R.string.fota_not_possible_device_info_not_read));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE)){
                    setTextInformation(getString(R.string.fota_not_possible_device_battery_low));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE)){
                    setTextInformation(getString(R.string.fota_not_possible_phone_battery_low));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS)){
                    setTextInformation(getString(R.string.fota_not_possible_already_updated));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION)){
                    setTextInformation(getString(R.string.fota_not_possible_no_wifi));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED)){
                    setTextInformation(getString(R.string.fota_not_possible_permissions_not_granted));
                } else if (action.equals(ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED)){
                    setTextInformation(getString(R.string.fota_not_possible_version_check_failed));
                } else if (action.equals(ACTION_FOTA_POSSIBLE)){
                    setTextInformation(getString(R.string.fota_possible));
                } else if (action.equals(ACTION_FOTA_SUCCESS)){
                    setTextInformation(getString(R.string.fota_finished));
                } else if (action.equals(ACTION_FOTA_TIMEOUT)){
                    setTextInformation(getString(R.string.fota_timeout));
                } else if (action.equals(ACTION_FOTA_PROGRESS_UPDATE)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle.containsKey(OTA_PROGRESS)) {
                        String progressAndMax = bundle.getString(OTA_PROGRESS);
                        String[] splitProgress = progressAndMax.split("[,]");
                        int progress = Integer.parseInt(splitProgress[0]);
                        int maxProgress = Integer.parseInt(splitProgress[1]);
                        mFotaProgressBar.setProgress(progress);
                        mFotaProgressBar.setMax(maxProgress);
                        String progressText = "" + (int) (((float)progress / (float)maxProgress) * 100) + "%";
                        mFotaProgressPercent.setText(progressText);
                    }
                }
            }
        }
    };

    public UpdateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFotaApi = new FotaApi(this.getContext(), FotaApi.macAddress);

        // Register receiver
        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(mOTAStatusReceiver, Utils.make3rdPartyIntentFilter());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_update, container, false);
        MainActivity.currentFragment = UPDATE_FRAGMENT;
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((TextView) view.findViewById(R.id.tvDeviceAddress)).setText(FotaApi.macAddress);
        TextView tv = view.findViewById(R.id.tvDeviceName);
        String deviceName = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getRemoteDevice(FotaApi.macAddress).getName();
        if (deviceName == null){
            deviceName = getString(R.string.unknown_device);
        }
        tv.setText(deviceName);
        setTextInformation(mCurrentText);

        // Attach onClickListeners
        getView().findViewById(R.id.btnChangeDevice).setOnClickListener(this);
        getView().findViewById(R.id.btnUpdatePossible).setOnClickListener(this);
        getView().findViewById(R.id.btnUpdateFirmware).setOnClickListener(this);
        getView().findViewById(R.id.btnRefreshAvailableFirmware).setOnClickListener(this);
        getView().findViewById(R.id.btnRefreshDeviceDetails).setOnClickListener(this);
        getView().findViewById(R.id.btnChangeFirmwareServer).setOnClickListener(this);
        getView().findViewById(R.id.btnSelectFirmwareServer).setOnClickListener(this);

        mFirmwareTableLayout = getView().findViewById(R.id.tableAvailableFirmware);
        mDeviceDetailsTableLayout = getView().findViewById(R.id.tableDeviceDetails);
        mFotaProgressBar = mView.findViewById(R.id.progressBar);
        mFotaProgressPercent = mView.findViewById(R.id.tvProgressBar);
        mEditFwServerDialog = new EditFwServerDialogFragment();
        mSelectFwServerDialog = new SelectFwServerDialogFragment();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        TextView textView = getView().findViewById(R.id.tvDeviceAddress);
        textView.requestFocus();
        setTextInformation(mCurrentText);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Unregister receiver
        LocalBroadcastManager.getInstance(this.getContext()).unregisterReceiver(mOTAStatusReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnChangeDevice:
                getFragmentManager().popBackStack();
                break;
            case R.id.btnUpdatePossible:
                mFotaApi.isFirmwareUpdatePossible();
                setTextInformation(getString(R.string.fota_checking_update_possible));
                break;
            case R.id.btnUpdateFirmware:
                setTextInformation(getString(R.string.fota_in_progress));
                mFotaApi.doFirmwareUpdate(true);
                break;
            case R.id.btnRefreshAvailableFirmware:
                getAllFirmwares();
                break;
            case R.id.btnRefreshDeviceDetails:
                getDeviceDetails();
                break;
            case R.id.btnChangeFirmwareServer:
                mEditFwServerDialog.show(getFragmentManager(),getString(R.string.edit_fw_server_fragment_tag));
                break;
            case R.id.btnSelectFirmwareServer:
                mSelectFwServerDialog.show(getFragmentManager(),getString(R.string.select_fw_server_fragment_tag));
                break;
            default:
                break;
        }
        TextView textView = getView().findViewById(R.id.tvDeviceAddress);
        textView.requestFocus();
    }

    public void setFirmwareServer(String urlAddress, String signKey) {
        mFotaApi.setFirmwareServer(urlAddress, signKey);
    }

    private void setTextInformation(String s){
        mCurrentText = s;
        ((TextView) mView.findViewById(R.id.tvStatusBox)).setText(s);
    }

    private void getAllFirmwares() {
        mFotaApi.getAllFirmwares(mFirmwareTableLayout);
    }

    private void getDeviceDetails() {
        mFotaApi.getDeviceDetails(mDeviceDetailsTableLayout);
    }
}
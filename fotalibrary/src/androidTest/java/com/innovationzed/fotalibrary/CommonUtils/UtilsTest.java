package com.innovationzed.fotalibrary.CommonUtils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.FotaApi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_BATTERY_READ;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_INFO_READ;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FILE_DOWNLOADED;
import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_FILE_DOWNLOAD_FAILED;
import static com.innovationzed.fotalibrary.CommonUtils.Utils.OTA_REASON;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    public Context testContext;
    private String mReason;
    private String mTestAction;
    private FotaBroadcastReceiver mReceiver;

    @Before
    public void setUp() throws Exception {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        mReason = "OK";
        mTestAction = "TestAction";
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Creates a temporary file and asserts that it it deleted after calling Utils.deleteFirmwareFile
     */
    @Test
    public void deleteFirmwareFile() {
        File tempFile = testContext.getExternalCacheDir();
        FotaApi.downloadedFirmwareDir = tempFile.getAbsolutePath();
        assertTrue(tempFile.exists());
        Utils.deleteFirmwareFile();
        assertTrue(!tempFile.exists());
    }

    /**
     * Tests that a correct broadcast is sent
     */
    @Test
    public void broadcastOTAFinished() {
        mReceiver = new FotaBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Bundle bundle = intent.getExtras();

                assertTrue(action.equals(mTestAction));
                assertTrue(bundle.containsKey(OTA_REASON));
                assertTrue(bundle.getString(OTA_REASON).equals(mReason));

                // Unregister receiver
                BluetoothLeService.unregisterBroadcastReceiver(testContext, mReceiver);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(mTestAction);
        BluetoothLeService.registerBroadcastReceiver(testContext, mReceiver, filter);

        Utils.broadcastOTAFinished(testContext, mTestAction, mReason);
    }

    /**
     * Tests the 3 combinations of input to compareVersion
     */
    @Test
    public void compareVersion() {
        String oldVersion = "0.1.0";
        String newVersion = "1.2.0";

        int firstParameterOld = Utils.compareVersion(oldVersion, newVersion);
        int firstParameterNew = Utils.compareVersion(newVersion, oldVersion);
        int identicalParameters = Utils.compareVersion(newVersion, newVersion);

        assertTrue(firstParameterOld == -1);
        assertTrue(firstParameterNew == 1);
        assertTrue(identicalParameters == 0);
    }

    /**
     * Tests the 3 combinations of input to compareVersion
     */
    @Test
    public void isCyacd2File() {
        String isCyacd2 = "file.cyacd2";
        String isNotCyacd2 = "file.abc";

        assertTrue(Utils.isCyacd2File(isCyacd2));
        assertFalse(Utils.isCyacd2File(isNotCyacd2));
    }

    /**
     * Tests that makeOTAFilter adds the correct filter
     */
    @Test
    public void makeOTADataFilter() {
        int nbrActions = 0;
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE_V1);

        IntentFilter filter = Utils.makeOTADataFilter();
        Iterator<String> it = filter.actionsIterator();
        while (it.hasNext()) {
            String s = it.next();
            assertTrue(actionList.contains(s));
            ++nbrActions;
        }
        assertTrue(nbrActions == actionList.size());
    }

    /**
     * Tests that make3rdPartyIntentFilter adds all the correct filters
     */
    @Test
    public void make3rdPartyIntentFilter() {
        int nbrActions = 0;
        ArrayList<String> actionList = get3rdPartyActions();

        IntentFilter filter = Utils.make3rdPartyIntentFilter();
        Iterator<String> it = filter.actionsIterator();
        while (it.hasNext()) {
            String s = it.next();
            assertTrue(actionList.contains(s));
            ++nbrActions;
        }
        assertTrue(nbrActions == actionList.size());
    }

    /**
     * Tests that makeFotaApiIntentFilter adds all the correct filters
     */
    @Test
    public void makeFotaApiIntentFilter() {
        int nbrActions = 0;
        ArrayList<String> actionList = getFotaApiActions();

        IntentFilter filter = Utils.makeFotaApiIntentFilter();
        Iterator<String> it = filter.actionsIterator();
        while (it.hasNext()) {
            String s = it.next();
            assertTrue(actionList.contains(s));
            ++nbrActions;
        }
        assertTrue(nbrActions == actionList.size());
    }

    /**
     * Tests that makeAppModeIntentFilter adds all the correct filters
     */
    @Test
    public void makeAppModeIntentFilter() {
        int nbrActions = 0;
        ArrayList<String> actionList = getAppModeActions();

        IntentFilter filter = Utils.makeAppModeIntentFilter();
        Iterator<String> it = filter.actionsIterator();
        while (it.hasNext()) {
            String s = it.next();
            assertTrue(actionList.contains(s));
            ++nbrActions;
        }
        assertTrue(nbrActions == actionList.size());
    }

    /**
     * Tests that makeBootModeIntentFilter adds all the correct filters
     */
    @Test
    public void makeImmediateAlertIntentFilter() {
        int nbrActions = 0;
        ArrayList<String> actionList = getImmediateAlertActions();

        IntentFilter filter = Utils.makeImmediateAlertIntentFilter();
        Iterator<String> it = filter.actionsIterator();
        while (it.hasNext()) {
            String s = it.next();
            assertTrue(actionList.contains(s));
            ++nbrActions;
        }
        assertTrue(nbrActions == actionList.size());
    }

    /**
     * Tests that makeBootModeIntentFilter adds all the correct filters
     */
    @Test
    public void makeBootModeIntentFilter() {
        int nbrActions = 0;
        ArrayList<String> actionList = getBootModeActions();

        IntentFilter filter = Utils.makeBootModeIntentFilter();
        Iterator<String> it = filter.actionsIterator();
        while (it.hasNext()) {
            String s = it.next();
            assertTrue(actionList.contains(s));
            ++nbrActions;
        }
        assertTrue(nbrActions == actionList.size());
    }

    private ArrayList<String> get3rdPartyActions(){
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(Constants.ACTION_FOTA_BLE_CONNECTION_FAILED);
        actionList.add(Constants.ACTION_FOTA_COULD_NOT_BE_STARTED);
        actionList.add(Constants.ACTION_FOTA_FAIL);
        actionList.add(Constants.ACTION_FOTA_FILE_DOWNLOAD_FAILED);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_DEVICE_BATTERY_NOT_READ);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_DEVICE_INFO_NOT_READ);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_NO_UPDATE_EXISTS);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED);
        actionList.add(Constants.ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED);
        actionList.add(Constants.ACTION_FOTA_POSSIBLE);
        actionList.add(Constants.ACTION_FOTA_SUCCESS);
        actionList.add(Constants.ACTION_FOTA_TIMEOUT);
        return actionList;
    }

    private ArrayList<String> getFotaApiActions(){
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(Constants.ACTION_FOTA_FAIL);
        actionList.add(Constants.ACTION_FOTA_SUCCESS);
        actionList.add(Constants.ACTION_FOTA_TIMEOUT);
        return actionList;
    }

    private ArrayList<String> getAppModeActions(){
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(BluetoothAdapter.ACTION_STATE_CHANGED);
        actionList.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        actionList.add(BluetoothDevice.ACTION_PAIRING_REQUEST);
        actionList.add(BluetoothLeService.ACTION_PAIRING_CANCEL);
        actionList.add(BluetoothLeService.ACTION_OTA_STATUS);//CYACD
        actionList.add(BluetoothLeService.ACTION_OTA_STATUS_V1);//CYACD2
        actionList.add(BluetoothLeService.ACTION_GATT_CONNECTED);
        actionList.add(BluetoothLeService.ACTION_GATT_CONNECTING);
        actionList.add(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        actionList.add(BluetoothLeService.ACTION_GATT_DISCONNECTING);
        actionList.add(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        actionList.add(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
        actionList.add(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_ERROR);
        actionList.add(BluetoothLeService.ACTION_GATT_INSUFFICIENT_ENCRYPTION);
        actionList.add(BluetoothLeService.ACTION_DATA_AVAILABLE);
        actionList.add(BluetoothLeService.ACTION_WRITE_SUCCESS);
        actionList.add(BluetoothLeService.ACTION_WRITE_FAILED);
        actionList.add(BluetoothLeService.ACTION_WRITE_COMPLETED);
        actionList.add(LocationManager.PROVIDERS_CHANGED_ACTION);

        actionList.add(ACTION_FOTA_DEVICE_BATTERY_READ);
        actionList.add(ACTION_FOTA_DEVICE_INFO_READ);
        return actionList;
    }

    private ArrayList<String> getBootModeActions(){
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(BluetoothAdapter.ACTION_STATE_CHANGED);
        actionList.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        actionList.add(BluetoothDevice.ACTION_PAIRING_REQUEST);
        actionList.add(BluetoothLeService.ACTION_PAIRING_CANCEL);
        actionList.add(BluetoothLeService.ACTION_OTA_STATUS);//CYACD
        actionList.add(BluetoothLeService.ACTION_OTA_STATUS_V1);//CYACD2
        actionList.add(BluetoothLeService.ACTION_GATT_CONNECTED);
        actionList.add(BluetoothLeService.ACTION_GATT_CONNECTING);
        actionList.add(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        actionList.add(BluetoothLeService.ACTION_GATT_DISCONNECTING);
        actionList.add(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        actionList.add(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
        actionList.add(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_ERROR);
        actionList.add(BluetoothLeService.ACTION_GATT_INSUFFICIENT_ENCRYPTION);
        actionList.add(BluetoothLeService.ACTION_DATA_AVAILABLE);
        actionList.add(BluetoothLeService.ACTION_WRITE_SUCCESS);
        actionList.add(BluetoothLeService.ACTION_WRITE_FAILED);
        actionList.add(BluetoothLeService.ACTION_WRITE_COMPLETED);
        actionList.add(LocationManager.PROVIDERS_CHANGED_ACTION);

        actionList.add(BluetoothDevice.ACTION_FOUND);
        return actionList;
    }

    private ArrayList<String> getImmediateAlertActions(){
        ArrayList<String> actionList = new ArrayList<>();
        actionList.add(BluetoothAdapter.ACTION_STATE_CHANGED);
        actionList.add(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        actionList.add(BluetoothDevice.ACTION_PAIRING_REQUEST);
        actionList.add(BluetoothLeService.ACTION_PAIRING_CANCEL);
        actionList.add(BluetoothLeService.ACTION_OTA_STATUS);//CYACD
        actionList.add(BluetoothLeService.ACTION_OTA_STATUS_V1);//CYACD2
        actionList.add(BluetoothLeService.ACTION_GATT_CONNECTED);
        actionList.add(BluetoothLeService.ACTION_GATT_CONNECTING);
        actionList.add(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        actionList.add(BluetoothLeService.ACTION_GATT_DISCONNECTING);
        actionList.add(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        actionList.add(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
        actionList.add(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_ERROR);
        actionList.add(BluetoothLeService.ACTION_GATT_INSUFFICIENT_ENCRYPTION);
        actionList.add(BluetoothLeService.ACTION_DATA_AVAILABLE);
        actionList.add(BluetoothLeService.ACTION_WRITE_SUCCESS);
        actionList.add(BluetoothLeService.ACTION_WRITE_FAILED);
        actionList.add(BluetoothLeService.ACTION_WRITE_COMPLETED);
        actionList.add(LocationManager.PROVIDERS_CHANGED_ACTION);

        actionList.add(ACTION_FOTA_FILE_DOWNLOADED);
        actionList.add(ACTION_FOTA_FILE_DOWNLOAD_FAILED);
        return actionList;
    }
}
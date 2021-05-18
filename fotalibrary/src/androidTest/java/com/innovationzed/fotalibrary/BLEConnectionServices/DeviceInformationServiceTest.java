package com.innovationzed.fotalibrary.BLEConnectionServices;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.innovationzed.fotalibrary.CommonUtils.Constants;
import com.innovationzed.fotalibrary.CommonUtils.FotaBroadcastReceiver;
import com.innovationzed.fotalibrary.CommonUtils.UUIDDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_INFO_READ;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeviceInformationServiceTest {

    public Context testContext;
    private Dictionary mDeviceInfo;
    private FotaBroadcastReceiver mReceiver;
    private DeviceInformationService mDeviceInformationService;
    private String mFirmwareRevision;
    private String mManufacturerName;

    @Before
    public void setUp() throws Exception {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests that DeviceInformationService reads the correct device info
     */
    @Test
    public void readDeviceInfo() {
        // Register receiver to react when device info has been read
        mReceiver = new FotaBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertTrue(intent.getAction().equals(ACTION_FOTA_DEVICE_INFO_READ));

                mDeviceInfo = mDeviceInformationService.getDeviceInformation();
                assertTrue(mDeviceInfo.get("FirmwareRevision").equals(mFirmwareRevision));
                assertTrue(mDeviceInfo.get("ManufacturerName").equals(mManufacturerName));

                // Unregister receiver
                BluetoothLeService.unregisterBroadcastReceiver(testContext, mReceiver);
                mDeviceInformationService.stop(testContext);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FOTA_DEVICE_INFO_READ);
        BluetoothLeService.registerBroadcastReceiver(testContext, mReceiver, filter);

        // Mock gatt service
        BluetoothGattService mockedService = mock(BluetoothGattService.class);
        BluetoothGattCharacteristic characteristic1 = new BluetoothGattCharacteristic(UUIDDatabase.UUID_MANUFACTURER_NAME, 1, 1);
        BluetoothGattCharacteristic characteristic2 = new BluetoothGattCharacteristic(UUIDDatabase.UUID_FIRMWARE_REVISION, 1, 1);
        List<BluetoothGattCharacteristic> gattCharacteristics = new ArrayList<>();
        gattCharacteristics.add(characteristic1);
        gattCharacteristics.add(characteristic2);
        when(mockedService.getCharacteristics()).thenReturn(gattCharacteristics);

        // Start reading battery info
        mDeviceInformationService = DeviceInformationService.create(mockedService);
        mDeviceInformationService.startReadingDeviceInfo(testContext);

        // Explicitly send the info instead of reading from device via BLE
        mFirmwareRevision = "5.7.2";
        mManufacturerName = "Unit test";
        Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_FIRMWARE_REVISION, mFirmwareRevision);
        bundle.putString(Constants.EXTRA_MANUFACTURER_NAME, mManufacturerName);
        intent.putExtras(bundle);
        BluetoothLeService.sendLocalBroadcastIntent(testContext, intent);

    }

}
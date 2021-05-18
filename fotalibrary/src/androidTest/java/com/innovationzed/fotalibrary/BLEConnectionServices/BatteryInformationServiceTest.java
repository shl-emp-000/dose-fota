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
import java.util.List;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_DEVICE_BATTERY_READ;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatteryInformationServiceTest {
    public Context testContext;
    private int mBatteryLevel;
    private FotaBroadcastReceiver mReceiver;
    private BatteryInformationService mBatteryInformationService;

    @Before
    public void setUp() throws Exception {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests that BatteryInformationService reads the correct battery level
     */
    @Test
    public void readBatteryLevel() {
        // Register receiver to react when battery has been read
        mReceiver = new FotaBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertTrue(intent.getAction().equals(ACTION_FOTA_DEVICE_BATTERY_READ));
                assertTrue(mBatteryInformationService.getBatteryLevel() == mBatteryLevel);

                // Unregister receiver
                BluetoothLeService.unregisterBroadcastReceiver(testContext, mReceiver);
                mBatteryInformationService.stopReadingBatteryInfo(testContext);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_FOTA_DEVICE_BATTERY_READ);
        BluetoothLeService.registerBroadcastReceiver(testContext, mReceiver, filter);

        // Mock gatt service
        BluetoothGattService mockedService = mock(BluetoothGattService.class);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUIDDatabase.UUID_BATTERY_LEVEL, 1, 1);
        List<BluetoothGattCharacteristic> gattCharacteristics = new ArrayList<>();
        gattCharacteristics.add(characteristic);
        when(mockedService.getCharacteristics()).thenReturn(gattCharacteristics);

        // Start reading battery info
        mBatteryInformationService = BatteryInformationService.create(mockedService);
        mBatteryInformationService.startReadingBatteryInfo(testContext);

        // Explicitly send the info instead of reading from device via BLE
        mBatteryLevel = 92;
        Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_BTL_VALUE, String.valueOf(mBatteryLevel));
        intent.putExtras(bundle);
        BluetoothLeService.sendLocalBroadcastIntent(testContext, intent);

    }

}
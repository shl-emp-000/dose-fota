package com.innovationzed.fotalibrary;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.test.platform.app.InstrumentationRegistry;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;
import com.innovationzed.fotalibrary.CommonUtils.FotaBroadcastReceiver;
import com.innovationzed.fotalibrary.CommonUtils.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.innovationzed.fotalibrary.CommonUtils.Constants.ACTION_FOTA_COULD_NOT_BE_STARTED;
import static org.junit.Assert.assertTrue;

public class FotaApiTest {

    public Context testContext;
    private FotaApi mFotaApi;
    private String mMacAddress;
    private FotaBroadcastReceiver mReceiver;

    @Before
    public void setUp() throws Exception {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        mMacAddress = "00:AA:11:BB:22:CC";

    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests that it's not possible to do firmware update without user confirmation
     */
    @Test
    public void doFirmwareUpdateWithoutUserConfirmation() {
        Looper.prepare();
        mFotaApi = new FotaApi(testContext, mMacAddress);
        mReceiver = new FotaBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                assertTrue(intent.getAction().equals(ACTION_FOTA_COULD_NOT_BE_STARTED));

                // Unregister receiver
                BluetoothLeService.unregisterBroadcastReceiver(testContext, mReceiver);
            }
        };
        BluetoothLeService.registerBroadcastReceiver(testContext, mReceiver, Utils.makeFotaApiIntentFilter());
        mFotaApi.doFirmwareUpdate(false);
    }

    /**
     * Tests that the MAC address actually changes
     */
    @Test
    public void changeDevice() {
        mFotaApi = new FotaApi(testContext, mMacAddress);
        String previousMac = mMacAddress;
        mFotaApi.changeDevice("00:00:00:00:00:00");
        assertTrue(!FotaApi.macAddress.equals(previousMac));
    }

}
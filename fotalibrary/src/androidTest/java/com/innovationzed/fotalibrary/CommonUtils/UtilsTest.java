package com.innovationzed.fotalibrary.CommonUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.innovationzed.fotalibrary.BLEConnectionServices.BluetoothLeService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.innovationzed.fotalibrary.CommonUtils.Utils.OTA_REASON;
import static com.innovationzed.fotalibrary.FotaApi.DOWNLOADED_FIRMWARE_DIR;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    public Context testContext;
    private String mReason;
    private String mTestAction;
    private BroadcastReceiver mReceiver;

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
        DOWNLOADED_FIRMWARE_DIR = tempFile.getAbsolutePath();
        assertTrue(tempFile.exists());
        Utils.deleteFirmwareFile();
        assertTrue(!tempFile.exists());
    }

    /**
     * Tests that a correct broadcast is sent
     */
    @Test
    public void broadcastOTAFinished() {
        mReceiver = new BroadcastReceiver() {
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
}
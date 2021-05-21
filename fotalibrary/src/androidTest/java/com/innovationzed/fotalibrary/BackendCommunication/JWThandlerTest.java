package com.innovationzed.fotalibrary.BackendCommunication;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.innovationzed.fotalibrary.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JWThandlerTest {
    public Context testContext;
    public Dictionary testDeviceInformation;

    @Before
    public void setUp() throws Exception {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        testDeviceInformation = new Hashtable();
        testDeviceInformation.put("ManufacturerName", "ManufacturerName");
        testDeviceInformation.put("ModelNumber", "ModelNumber");
        testDeviceInformation.put("SerialNumber", "12345");
        testDeviceInformation.put("HardwareRevision", "v5");
        testDeviceInformation.put("FirmwareRevision", "FirmwareRevision");
        testDeviceInformation.put("SoftwareRevision", "SoftwareRevision");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getAuthToken() {
        String authToken = JWThandler.getAuthToken(testContext, testDeviceInformation);
        Claims claims = decodeJWT(authToken.replace("Bearer ", ""), testContext);
        assertNotNull(claims.get("jti"));
        assertEquals("access", claims.get("token_type"));
        assertEquals(testDeviceInformation.get("SerialNumber").toString(), claims.get("user_id"));
        assertEquals(testDeviceInformation.get("HardwareRevision").toString(), claims.get("hw_rev"));

        Date jwtDate = claims.getExpiration();
        Date timeNow = new Date();
        // Check that expiration time is at least 35 min in the future!
        assertTrue(((jwtDate.getTime() - timeNow.getTime())/1000) > (35*60));

    }

    public static Claims decodeJWT(String jwt, Context testContext) {
        //This line will throw an exception if it is not a signed JWS (as expected)
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(testContext.getString(R.string.api_secret_key).getBytes(StandardCharsets.UTF_8)).build()
                .parseClaimsJws(jwt).getBody();
        return claims;
    }
}
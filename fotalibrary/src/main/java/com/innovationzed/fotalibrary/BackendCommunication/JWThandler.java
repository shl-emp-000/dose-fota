package com.innovationzed.fotalibrary.BackendCommunication;

import android.content.Context;
import android.util.Log;

import com.innovationzed.fotalibrary.R;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Dictionary;
import java.util.Map;
import java.util.UUID;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public final class JWThandler {
    private static String mJwtString = "";
    private static String mAuthToken = "";
    private static String mSerialNumber = "";
    private static String mHardwareRevision = "";

    public static String getAuthToken(Context current, Dictionary deviceInfo) {
        if (mAuthToken.isEmpty() || isAuthTokenExpired(current) || !mSerialNumber.equals(deviceInfo.get("SerialNumber").toString()) || !mHardwareRevision.equals(deviceInfo.get("HardwareRevision").toString())) {
            mSerialNumber = deviceInfo.get("SerialNumber").toString();
            mHardwareRevision = deviceInfo.get("HardwareRevision").toString();
            buildNewAuthToken(current);
        }
        return mAuthToken;
    }

    private JWThandler() {} // Make constructor private to make it a static class

    private static void buildNewAuthToken(Context current) {
        Header header = Jwts.header();
        header.setType("JWT"); // Works without setting the type
        Date expTime = new Date();
        expTime.setTime(System.currentTimeMillis()+(40*60*1000)); // Token valid for 40 min
        mJwtString = Jwts.builder().setHeader((Map<String, Object>) header).setId(UUID.randomUUID().toString()).setExpiration(expTime).claim("token_type", "access").claim("user_id", mSerialNumber).claim("hw_rev", mHardwareRevision).signWith(Keys.hmacShaKeyFor(current.getString(R.string.api_secret_key).getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
        mAuthToken = "Bearer " + mJwtString;
    }

    private static boolean isAuthTokenExpired(Context current) {
        boolean retVal = false;
        Date jwtDate = Jwts.parserBuilder().setSigningKey(current.getString(R.string.api_secret_key).getBytes(StandardCharsets.UTF_8)).build().parseClaimsJws(mJwtString).getBody().getExpiration();
        Date timeNow = new Date();
        if ((jwtDate.getTime() - timeNow.getTime())/1000 < (10*60)) { // If it is less than 10 min remaining on the token
            retVal = true;
        }
        return retVal;
    }
}

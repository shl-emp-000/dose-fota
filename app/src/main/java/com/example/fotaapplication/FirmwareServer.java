package com.example.fotaapplication;

import java.io.Serializable;

public class FirmwareServer implements Serializable {
    private String mServerAddress;
    private String mServerSigningKey;

    public FirmwareServer () {
        this.mServerAddress = "";
        this.mServerSigningKey = "";
    }

    public FirmwareServer (String serverAddress, String serverSigningKey) {
        this.mServerAddress = serverAddress;
        this.mServerSigningKey = serverSigningKey;
    }

    public String getServerAddress() {
        return mServerAddress;
    }

    public String getServerSigningKey() {
        return mServerSigningKey;
    }

    public void setServerAddress(String address) {
        this.mServerAddress = address;
    }

    public void setServerSigningKey(String key) {
        this.mServerSigningKey = key;
    }

}

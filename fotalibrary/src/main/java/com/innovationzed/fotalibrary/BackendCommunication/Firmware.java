package com.innovationzed.fotalibrary.BackendCommunication;

import com.google.gson.annotations.SerializedName;

public class Firmware {
    @SerializedName("fw_version")
    private String firmwareVersion;

    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
}
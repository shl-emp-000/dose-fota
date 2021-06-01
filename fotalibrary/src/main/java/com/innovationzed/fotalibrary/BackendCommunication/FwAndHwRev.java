package com.innovationzed.fotalibrary.BackendCommunication;

import com.google.gson.annotations.SerializedName;

public class FwAndHwRev {

    @SerializedName("fw_version")
    private String firmwareVersion;

    @SerializedName("hw_compatibility")
    private String hardwareCompatibility;

    // Getters
    public String getFirmwareVersion() {
        return this.firmwareVersion;
    }
    public String getHardwareCompatibility() {
        return this.hardwareCompatibility;
    }

    // Setters
    public void setFirmwareVersion(String fwVersion) {
        this.firmwareVersion = fwVersion;
    }

    public void setHardwareCompability(String hardwareCompatibility) {
        this.hardwareCompatibility = hardwareCompatibility;
    }
}

package com.innovationzed.fotalibrary.BackendCommunication;

public class HistoryRequest {

    final private String device;
    final private String fw_update_started;
    final private Boolean fw_update_success;
    final private String firmware;
    final private String device_firmware;
    final private String reason;

    public HistoryRequest(String device, String fwUpdateStarted, Boolean fwUpdateSuccess, String firmware, String deviceFirmware, String reason){
        this.device = device;
        this.fw_update_started = fwUpdateStarted;
        this.fw_update_success = fwUpdateSuccess;
        this.firmware = firmware;
        this.device_firmware = deviceFirmware;
        this.reason = reason;
    }

}
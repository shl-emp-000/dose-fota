package com.innovationzed.fotalibrary.BackendCommunication;

public class HistoryRequest {

    final private String device;
    final private String fw_update_started;
    final private Boolean fw_update_success;
    final private String firmware;
    final private String device_firmware;
    final private String reason;
    final private String manufacturer_name;
    final private String model_number;
    final private String hardware_revision;
    final private String software_revision;

    public HistoryRequest(String device, String fwUpdateStarted, Boolean fwUpdateSuccess, String firmware, String deviceFirmware, String reason,
                          String manufacturerName, String modelNumber, String hardwareRevision, String softwareRevision){
        this.device = device;
        this.fw_update_started = fwUpdateStarted;
        this.fw_update_success = fwUpdateSuccess;
        this.firmware = firmware;
        this.device_firmware = deviceFirmware;
        this.reason = reason;
        this.manufacturer_name = manufacturerName;
        this.model_number = modelNumber;
        this.hardware_revision = hardwareRevision;
        this.software_revision = softwareRevision;
    }

}
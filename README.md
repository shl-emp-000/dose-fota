# Introduction 
The application app is an example 3rd party app that has three buttons to demonstrate the functionality of the library. This has not been tested with real hardware.

# 3rd party example app
The example app consists of only one java file, MainActivity.java and a very simple GUI with three buttons. All the functions that are needed for FOTA are located in FotaApi.java. An instance of this class needs to be created and the current context + MAC address to the device should be passed as parameters.

Buttons:
1. "Check if firmware update is possible": This button calls the function isFirmwareUpdatepossible in FotaApi.java which performs some checks to make sure it's possible to update the firmware at this point. This function should be called before asking the user if they want to update. When the checks are done one of the following actions will be broadcasted:
- FotaApi.ACTION_FOTA_POSSIBLE
- FotaApi.ACTION_FOTA_NO_UPDATE_EXISTS
- FotaApi.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_DEVICE
- FotaApi.ACTION_FOTA_NOT_POSSIBLE_LOW_BATTERY_PHONE
- FotaApi.ACTION_FOTA_NOT_POSSIBLE_PERMISSIONS_NOT_GRANTED
- FotaApi.ACTION_FOTA_NOT_POSSIBLE_FILE_DOWNLOAD_FAILED
- FotaApi.ACTION_FOTA_NOT_POSSIBLE_NO_WIFI_CONNECTION
- FotaApi.ACTION_FOTA_NOT_POSSIBLE_VERSION_CHECK_FAILED
The example app contains an example of a receiver that listens to those actions.
2. "Confirm that user wants to update": This button confirms that the user wants to perform the update (this confirmation can be done in any way the 3rd party app prefers).
3. "Do firmware update": this button calls doFirmwareUpdate(boolean userConfirmation) in FotaApi.java. The device needs to be in boot mode before calling this function and that is achieved by using the reset buttons on the eval board. If userConfirmation is true and it has been checked that FOTA is possible, it starts the FOTA process. During this process it needs to pair with the device in boot mode to be able to discover the OTA bluetooth service. For newer Android versions (>= 10) the system might show a user dialog asking the user to confirm that they want to pair with the device, if that happens press pair/accept. When the FOTA process is done an action will be broadcasted, FotaApi.ACTION_FOTA_SUCCESS, FotaApi.ACTION_FOTA_COULD_NOT_BE_STARTED or FotaApi.ACTION_FOTA_FAIL. The example app contains an example of a receiver that listens to those actions.

# Tools
Android Studio 4.1 is used to build and run the example app and the library.

# Introduction 
The application app is an example 3rd party app to demonstrate the functionality of the library. The example app consists of only one java file, MainActivity.java and a very simple GUI with buttons. All the functions that are needed for FOTA are located in FotaApi.java. An instance of this class needs to be created and the current context + MAC address to the device should be passed as parameters. For more details regarding the implementation see the Software Architecture and Design Specification.

# How to do FOTA
1. Make sure you are paired and connected to the device you want to update (this is done in the Bluetooth settings on the mobile phone).
2. Enter the MAC address of the device and press "Change device".
3. Press "Check if firmware update is possible" and wait for a response.
4. If firmware update is possible, press "Confirm that user wants to update".
5. Press "Do firmware update". When a pairing request is shown you need to accept it (this request is to pair with the device in boot mode). The LED on the device will double blink in white during the firmware update (it can take 2-3 minutes).

# Tools
Android Studio 4.1 is used to build and run the example app and the library.

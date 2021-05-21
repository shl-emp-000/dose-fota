package com.innovationzed.fotalibrary.BackendCommunication;

import android.content.Context;

import com.innovationzed.fotalibrary.FotaApi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BackendApiRequest {
    private final int MAX_RETRIES = 5;
    private final String BASE_URL = "https://iz-test-app.azurewebsites.net/api/";

    private Retrofit mRetrofit;
    private IzFotaApi mIzFotaApi;
    private SimpleDateFormat mSdf;
    private Context mContext;
    private Callback<Void> mCallback;
    private Call<Void> mPostCall;
    private int mRetryCounter;

    public BackendApiRequest(Context context){
        mContext = context;
        mSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        // Set up retrofit
        mRetrofit = RetrofitClient.getClient(BASE_URL);
        mIzFotaApi = mRetrofit.create(IzFotaApi.class);
    }

    /**
     * Gets the latest available firmware version. A callback is provided so that the caller can handle
     * the response in any way they like
     * @param callback
     */
    public void getLatestFirmwareVersion(Callback<Firmware> callback, Dictionary deviceInfo){
        Call<Firmware> call = mIzFotaApi.getLatestFirmwareVersion(JWThandler.getAuthToken(mContext, deviceInfo));
        call.enqueue(callback);
    }

    /**
     * Downloads the latest available firmware file. A callback is provided so that the caller can handle
     * the response in any way they like
     * @param callback
     */
    public void downloadLatestFirmwareFile(Callback<ResponseBody> callback, Dictionary deviceInfo){
        Call<ResponseBody> call = mIzFotaApi.downloadLatestFirmware(JWThandler.getAuthToken(mContext, deviceInfo));
        call.enqueue(callback);
    }

    /**
     * Posts info to the backend about an attempted update
     * @param success boolean that is true if firmware update was successful, else false
     * @param reason String with a description how the fimware update went, mainly useful when the update failed
     * @param deviceInfo a Dictionary containing information about the device, like batteryLevel and serialNumber
     */
    public void postFotaResult(boolean success, String reason, Dictionary deviceInfo) {
        String date = mSdf.format(new Date(System.currentTimeMillis()));
        //Dictionary deviceInfo = Utils.getDeviceInformation();
        HistoryRequest history = new HistoryRequest((String) FotaApi.macAddress, date, success, FotaApi.latestFirmwareVersion, (String) deviceInfo.get("FirmwareRevision"), reason,
                (String) deviceInfo.get("ManufacturerName"), (String) deviceInfo.get("ModelNumber"), (String) deviceInfo.get("HardwareRevision"), (String) deviceInfo.get("SoftwareRevision"));
        mRetryCounter = 0;

        // Call API
        Call<Void> call = mIzFotaApi.postData(JWThandler.getAuthToken(mContext, deviceInfo), history);
        // Clone the call so we can resend it
        mPostCall = call.clone();

        call.enqueue(mCallback = new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (!response.isSuccessful()) {
                    if (mRetryCounter <=  MAX_RETRIES) {
                        ++mRetryCounter;
                        call = mPostCall.clone();
                        call.enqueue(mCallback);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (mRetryCounter <=  MAX_RETRIES) {
                    ++mRetryCounter;
                    call = mPostCall.clone();
                    call.enqueue(mCallback);
                }
            }
        });
    }

}

package com.innovationzed.fotalibrary.BackendCommunication;

import android.content.Context;

import com.innovationzed.fotalibrary.CommonUtils.Utils;
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
        mRetrofit = RetrofitClient.getClient("https://iz-test-app.azurewebsites.net/api/");
        mIzFotaApi = mRetrofit.create(IzFotaApi.class);
    }

    public void getLatestFirmwareVersion(Callback<List<Firmware>> callback){
        Call<List<Firmware>> call = mIzFotaApi.getLatestFirmwareVersion(JWThandler.getAuthToken(mContext));
        call.enqueue(callback);
    }

    public void downloadLatestFirmwareFile(Callback<ResponseBody> callback){
        Call<ResponseBody> call = mIzFotaApi.downloadLatestFirmware(JWThandler.getAuthToken(mContext));
        call.enqueue(callback);
    }

    public void postFotaResult(boolean success, String reason) {
        String date = mSdf.format(new Date(System.currentTimeMillis()));
        Dictionary deviceInfo = Utils.getDeviceInformation();
        HistoryRequest history = new HistoryRequest((String) deviceInfo.get("deviceSN"), date, success, FotaApi.latestFirmwareVersion, (String) deviceInfo.get("firmwareVersion"), reason,
                (String) deviceInfo.get("manufacturerName"), (String) deviceInfo.get("modelNumber"), (String) deviceInfo.get("hardwareRevision"), (String) deviceInfo.get("softwareRevision"));
        mRetryCounter = 0;

        // Call API
        Call<Void> call = mIzFotaApi.postData(JWThandler.getAuthToken(mContext), history);
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

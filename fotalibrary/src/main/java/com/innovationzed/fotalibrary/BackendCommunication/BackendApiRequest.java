package com.innovationzed.fotalibrary.BackendCommunication;

import android.content.Context;

import com.innovationzed.fotalibrary.CommonUtils.JWThandler;
import com.innovationzed.fotalibrary.FotaApi;

import java.text.SimpleDateFormat;
import java.util.Date;
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
    private String mLatestFirmwareVersion;
    private String mNewFirmwarePath;
    private Context mContext;
    private Callback<Void> mCallback;
    private Call<Void> mPostCall;
    private int mRetryCounter;

    public BackendApiRequest(Context context){

        mContext = context;

        mSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        mLatestFirmwareVersion = "";
        mNewFirmwarePath = "";

        // Set up retrofit
        mRetrofit = RetrofitClient.getClient("https://iz-test-app.azurewebsites.net/api/");
        mIzFotaApi = mRetrofit.create(IzFotaApi.class);
    }

    public void getLatestFirmwareVersion(Callback<List<Firmware>> callback){

        Call<List<Firmware>> call = mIzFotaApi.getLatestFirmwareVersion(JWThandler.getAuthToken(mContext));
        call.enqueue(callback);
//        call.enqueue(new Callback<List<Firmware>>() {
//            @Override
//            public void onResponse(Call<List<Firmware>> call, Response<List<Firmware>> response) {
//
//                if (!response.isSuccessful()) {
//                    return;
//                }
//                List<Firmware> versions = response.body();
//                mLatestFirmwareVersion = versions.get(0).getFirmwareVersion();
//            }
//
//            @Override
//            public void onFailure(Call<List<Firmware>> call, Throwable t) {
//            }
//        });
    }

    public void downloadLatestFirmwareFile(Callback<ResponseBody> callback){
        Call<ResponseBody> call = mIzFotaApi.downloadLatestFirmware(JWThandler.getAuthToken(mContext));
        call.enqueue(callback);//new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//
//                if (!response.isSuccessful()) {
//                    return;
//                }
//                String contentDisposition = response.raw().header("Content-Disposition");
//                String strFilename = "filename=";
//                int startIndex = contentDisposition.indexOf(strFilename);
//                String filename = contentDisposition.substring(startIndex + strFilename.length());
//                mNewFirmwarePath =  ROOT_DIR + File.separator + filename;
//                boolean writtenToDisk = writeResponseBodyToDisk(response.body(), mNewFirmwarePath);
//
//                if (writtenToDisk) {
//
//                } else {
//                }
//
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//            }
//        });
    }

    public void postFotaResult(boolean success, String reason) {
        String date = mSdf.format(new Date(System.currentTimeMillis()));
        HistoryRequest history = new HistoryRequest(FotaApi.getDeviceSN(), date, success, FotaApi.latestFirmwareVersion, FotaApi.getDeviceFirmwareVersion(), reason);
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

    private void postHardCodedData(){

        // Set parameters
        String device = "1122";
        boolean fwUpdateSuccess = true;
        String newFirmware = "2.0.0";
        String deviceFirmware = "1.0.0";
        String reason = "Test from new android app with authentication";
        String date = mSdf.format(new Date(System.currentTimeMillis()));

        HistoryRequest history = new HistoryRequest(device, date, fwUpdateSuccess, newFirmware, deviceFirmware, reason);

        // Call API
        Call<Void> call = mIzFotaApi.postData(JWThandler.getAuthToken(mContext), history);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (!response.isSuccessful()) {
                    return;
                }

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

}

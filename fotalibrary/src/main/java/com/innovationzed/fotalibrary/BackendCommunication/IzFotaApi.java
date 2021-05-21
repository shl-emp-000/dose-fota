package com.innovationzed.fotalibrary.BackendCommunication;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IzFotaApi {

    // a resource relative to your base URL
    @Headers({"Content-Type: application/json;charset=UTF-8"})
    @GET("dl_latest_fw/")
    Call<ResponseBody> downloadLatestFirmware(@Header("Authorization") String authHeader);

    @Headers({"Content-Type: application/json;charset=UTF-8"})
    @GET("latest_fw_version/")
    Call<Firmware> getLatestFirmwareVersion(@Header("Authorization") String authHeader);

    @Headers({"Content-Type: application/json;charset=UTF-8"})
    @POST("post_results/")
    Call<Void> postData(@Header("Authorization") String authHeader,
                        @Body HistoryRequest body
    );
}

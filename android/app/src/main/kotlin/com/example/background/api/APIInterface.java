package com.example.background.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.POST;
import retrofit2.http.FormUrlEncoded;

public interface APIInterface{
    @POST("/")
    @FormUrlEncoded
    Call<ResponseBody> sendCoords(
            @Field("lat") double lat,
            @Field("lng") double lng
    );
}

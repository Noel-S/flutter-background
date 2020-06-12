package com.example.background.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIClient {
    private final String URL = "http://ec2-18-222-56-161.us-east-2.compute.amazonaws.com";
    Retrofit retrofit;

    public APIClient() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        retrofit = new Retrofit.Builder()
                .baseUrl(this.URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public Call<ResponseBody> sendCoords(double lat, double lng) {
        APIInterface api = retrofit.create(APIInterface.class);
        return api.sendCoords(lat, lng);
    }
}

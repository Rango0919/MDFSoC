package com.example.mdfsoc.network;

import android.content.Context;
import android.util.Log;

import com.example.mdfsoc.BuildConfig;
import com.example.mdfsoc.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String TAG = "RetrofitClient";
    private static volatile RetrofitClient instance;
    private final ApiService apiService;

    private RetrofitClient(Context context) {
        SessionManager session = SessionManager.getInstance(context.getApplicationContext());

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(session))
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static RetrofitClient getInstance(Context context) {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient(context);
                }
            }
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
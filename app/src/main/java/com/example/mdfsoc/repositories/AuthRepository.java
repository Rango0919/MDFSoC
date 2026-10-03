package com.example.mdfsoc.repositories;

import android.content.Context;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.LoginRequest;
import com.example.mdfsoc.models.LoginResponse;
import com.example.mdfsoc.network.ApiService;
import com.example.mdfsoc.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final ApiService apiService;

    public AuthRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void login(String username, String password, final RepositoryCallback<LoginResponse> callback) {
        apiService.login(new LoginRequest(username, password))
                .enqueue(new Callback<ApiEnvelope<LoginResponse>>() {
                    @Override
                    public void onResponse(Call<ApiEnvelope<LoginResponse>> call,
                                           Response<ApiEnvelope<LoginResponse>> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onFailure(call, response, null);
                            return;
                        }
                        callback.onSuccess(call, response.body());
                    }

                    @Override
                    public void onFailure(Call<ApiEnvelope<LoginResponse>> call, Throwable t) {
                        callback.onFailure(call, null, t);
                    }
                });
    }
}
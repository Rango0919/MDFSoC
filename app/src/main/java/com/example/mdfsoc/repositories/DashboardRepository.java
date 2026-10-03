package com.example.mdfsoc.repositories;

import android.content.Context;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.DashboardData;
import com.example.mdfsoc.network.ApiService;
import com.example.mdfsoc.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardRepository {

    private final ApiService apiService;

    public DashboardRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void loadDashboard(final RepositoryCallback<DashboardData> callback) {
        apiService.getDashboard().enqueue(new Callback<ApiEnvelope<DashboardData>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<DashboardData>> call,
                                   Response<ApiEnvelope<DashboardData>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(call, response, null);
                    return;
                }
                callback.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<DashboardData>> call, Throwable t) {
                callback.onFailure(call, null, t);
            }
        });
    }
}
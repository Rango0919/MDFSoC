package com.example.mdfsoc.repositories;

import android.content.Context;

import com.example.mdfsoc.models.Alert;
import com.example.mdfsoc.models.AlertsPage;
import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.network.ApiService;
import com.example.mdfsoc.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertsRepository {

    private final ApiService apiService;

    public AlertsRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void loadAlerts(String severity, int limit, int offset,
                           final RepositoryCallback<AlertsPage> callback) {
        apiService.getAlerts(severity, limit, offset).enqueue(new Callback<ApiEnvelope<AlertsPage>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AlertsPage>> call,
                                   Response<ApiEnvelope<AlertsPage>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(call, response, null);
                    return;
                }
                callback.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AlertsPage>> call, Throwable t) {
                callback.onFailure(call, null, t);
            }
        });
    }

    public void loadAlert(long id, final RepositoryCallback<Alert> callback) {
        apiService.getAlert(id).enqueue(new Callback<ApiEnvelope<Alert>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<Alert>> call,
                                   Response<ApiEnvelope<Alert>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(call, response, null);
                    return;
                }
                callback.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<Alert>> call, Throwable t) {
                callback.onFailure(call, null, t);
            }
        });
    }
}
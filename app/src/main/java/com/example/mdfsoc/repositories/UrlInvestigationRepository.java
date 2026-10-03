package com.example.mdfsoc.repositories;

import android.content.Context;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.UrlReputation;
import com.example.mdfsoc.network.ApiService;
import com.example.mdfsoc.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UrlInvestigationRepository {

    private final ApiService apiService;

    public UrlInvestigationRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void loadUrlReputation(String url, final RepositoryCallback<UrlReputation> callback) {
        apiService.getUrlReputation(url).enqueue(new Callback<ApiEnvelope<UrlReputation>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<UrlReputation>> call,
                                   Response<ApiEnvelope<UrlReputation>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(call, response, null);
                    return;
                }
                callback.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<UrlReputation>> call, Throwable t) {
                callback.onFailure(call, null, t);
            }
        });
    }
}
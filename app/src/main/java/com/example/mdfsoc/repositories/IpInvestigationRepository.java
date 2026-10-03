package com.example.mdfsoc.repositories;

import android.content.Context;

import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.IpReputation;
import com.example.mdfsoc.models.ThreatIntelligence;
import com.example.mdfsoc.network.ApiService;
import com.example.mdfsoc.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IpInvestigationRepository {

    private final ApiService apiService;

    public IpInvestigationRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void loadThreatIntelligence(String ip, final RepositoryCallback<ThreatIntelligence> callback) {
        apiService.getThreatIntelligence(ip).enqueue(new Callback<ApiEnvelope<ThreatIntelligence>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<ThreatIntelligence>> call,
                                   Response<ApiEnvelope<ThreatIntelligence>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(call, response, null);
                    return;
                }
                callback.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<ThreatIntelligence>> call, Throwable t) {
                callback.onFailure(call, null, t);
            }
        });
    }

    public void loadIpReputation(String ip, final RepositoryCallback<IpReputation> callback) {
        apiService.getIpReputation(ip).enqueue(new Callback<ApiEnvelope<IpReputation>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<IpReputation>> call,
                                   Response<ApiEnvelope<IpReputation>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure(call, response, null);
                    return;
                }
                callback.onSuccess(call, response.body());
            }

            @Override
            public void onFailure(Call<ApiEnvelope<IpReputation>> call, Throwable t) {
                callback.onFailure(call, null, t);
            }
        });
    }
}
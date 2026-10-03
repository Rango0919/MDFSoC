package com.example.mdfsoc.repositories;

import androidx.annotation.Nullable;

import com.example.mdfsoc.models.ApiEnvelope;

import retrofit2.Call;
import retrofit2.Response;

public interface RepositoryCallback<T> {

    void onSuccess(Call<ApiEnvelope<T>> call, ApiEnvelope<T> envelope);

    void onFailure(Call<ApiEnvelope<T>> call, @Nullable Response<?> response, Throwable throwable);
}
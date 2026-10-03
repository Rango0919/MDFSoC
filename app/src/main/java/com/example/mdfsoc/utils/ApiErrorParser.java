package com.example.mdfsoc.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.mdfsoc.R;
import com.example.mdfsoc.models.ApiEnvelope;
import com.example.mdfsoc.models.ApiError;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;
import retrofit2.Response;

public final class ApiErrorParser {

    private static final Gson GSON = new Gson();

    private ApiErrorParser() {
    }

    public static String messageFor(Context context, @Nullable Throwable t, @Nullable Response<?> response) {
        if (t instanceof UnknownHostException
                || t instanceof ConnectException
                || t instanceof SocketTimeoutException) {
            return isOffline(context)
                    ? context.getString(R.string.error_no_internet)
                    : context.getString(R.string.error_backend_unavailable);
        }
        if (t instanceof SocketTimeoutException) {
            return context.getString(R.string.error_backend_unavailable);
        }
        if (response != null && !response.isSuccessful()) {
            ApiError apiError = extractApiError(response);
            if (apiError != null) {
                return mapBackendError(context, apiError);
            }
            return mapHttpStatus(context, response.code());
        }
        if (t instanceof HttpException) {
            HttpException http = (HttpException) t;
            ApiError apiError = extractApiError(http.response());
            if (apiError != null) {
                return mapBackendError(context, apiError);
            }
            return mapHttpStatus(context, http.code());
        }
        if (t != null && !(t instanceof IOException)) {
            return context.getString(R.string.error_server);
        }
        return context.getString(R.string.error_unknown);
    }

    @Nullable
    public static ApiError extractApiError(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return null;
            }
            ApiEnvelope<?> envelope = GSON.fromJson(response.errorBody().string(), ApiEnvelope.class);
            return envelope != null ? envelope.getError() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String mapBackendError(Context context, ApiError apiError) {
        if ("auth_error".equals(apiError.getCode())) {
            return context.getString(R.string.error_auth_failed);
        }
        if ("rate_limited".equals(apiError.getCode())) {
            if (apiError.isSource("abuseipdb")) {
                return context.getString(R.string.error_abuseipdb_rate_limit);
            }
            if (apiError.isSource("virustotal")) {
                return context.getString(R.string.error_virustotal_rate_limit);
            }
        }
        if ("validation_error".equals(apiError.getCode())) {
            return apiError.getMessage() != null
                    ? apiError.getMessage()
                    : context.getString(R.string.error_invalid_ip);
        }
        if ("upstream_error".equals(apiError.getCode())) {
            if (apiError.isSource("wazuh")) {
                return context.getString(R.string.error_wazuh_unavailable);
            }
            if (apiError.isSource("abuseipdb")) {
                return context.getString(R.string.error_server);
            }
            if (apiError.isSource("virustotal")) {
                return context.getString(R.string.error_server);
            }
        }
        if (apiError.getMessage() != null && !apiError.getMessage().isEmpty()) {
            return apiError.getMessage();
        }
        return context.getString(R.string.error_server);
    }

    private static String mapHttpStatus(Context context, int code) {
        if (code == 401 || code == 403) {
            return context.getString(R.string.error_auth_failed);
        }
        if (code == 429) {
            return context.getString(R.string.error_server);
        }
        if (code >= 500) {
            return context.getString(R.string.error_backend_unavailable);
        }
        return context.getString(R.string.error_server);
    }

    private static boolean isOffline(Context context) {
        return !NetworkUtils.isOnline(context);
    }
}
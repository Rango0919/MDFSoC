package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiEnvelope<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private T data;

    @SerializedName("error")
    private ApiError error;

    @SerializedName("warnings")
    private List<String> warnings;

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
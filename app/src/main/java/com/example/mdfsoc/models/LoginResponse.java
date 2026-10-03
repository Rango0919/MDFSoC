package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("expires_at")
    private String expiresAt;

    public String getToken() {
        return token;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
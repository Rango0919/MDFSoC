package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class ApiError {

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;

    @SerializedName("source")
    private String source;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public boolean isSource(String expected) {
        return source != null && source.equalsIgnoreCase(expected);
    }
}
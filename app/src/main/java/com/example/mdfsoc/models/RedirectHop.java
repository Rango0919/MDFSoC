package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class RedirectHop {

    @SerializedName("url")
    private String url;

    @SerializedName("host")
    private String host;

    @SerializedName("resolved_ips")
    private java.util.List<String> resolvedIps;

    @SerializedName("status")
    private int status;

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public java.util.List<String> getResolvedIps() {
        return resolvedIps;
    }

    public int getStatus() {
        return status;
    }
}
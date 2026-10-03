package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class RecentAlert {

    @SerializedName("id")
    private long id;

    @SerializedName("severity")
    private String severity;

    @SerializedName("title")
    private String title;

    @SerializedName("source_ip")
    private String sourceIp;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("status")
    private String status;

    public long getId() {
        return id;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }
}
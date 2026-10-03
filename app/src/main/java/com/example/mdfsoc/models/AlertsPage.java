package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AlertsPage {

    @SerializedName("alerts")
    private List<Alert> alerts;

    @SerializedName("total")
    private int total;

    @SerializedName("page")
    private int page;

    @SerializedName("limit")
    private int limit;

    public List<Alert> getAlerts() {
        return alerts;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }
}
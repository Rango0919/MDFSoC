package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DashboardData {

    @SerializedName("total_alerts")
    private int totalAlerts;

    @SerializedName("critical_alerts")
    private int criticalAlerts;

    @SerializedName("high_alerts")
    private int highAlerts;

    @SerializedName("medium_alerts")
    private int mediumAlerts;

    @SerializedName("low_alerts")
    private int lowAlerts;

    @SerializedName("recent_alerts")
    private List<RecentAlert> recentAlerts;

    public int getTotalAlerts() {
        return totalAlerts;
    }

    public int getCriticalAlerts() {
        return criticalAlerts;
    }

    public int getHighAlerts() {
        return highAlerts;
    }

    public int getMediumAlerts() {
        return mediumAlerts;
    }

    public int getLowAlerts() {
        return lowAlerts;
    }

    public List<RecentAlert> getRecentAlerts() {
        return recentAlerts;
    }
}
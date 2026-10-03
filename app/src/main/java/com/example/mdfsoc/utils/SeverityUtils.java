package com.example.mdfsoc.utils;

import android.content.Context;

import androidx.annotation.ColorInt;

import com.example.mdfsoc.R;

public final class SeverityUtils {

    private SeverityUtils() {
    }

    @ColorInt
    public static int colorFor(Context context, String severity) {
        if (severity == null) {
            return context.getColor(R.color.severity_low);
        }
        switch (severity.toLowerCase()) {
            case "critical":
            case "crit":
                return context.getColor(R.color.severity_critical);
            case "high":
                return context.getColor(R.color.severity_high);
            case "medium":
            case "med":
                return context.getColor(R.color.severity_medium);
            case "low":
                return context.getColor(R.color.severity_low);
            case "info":
                return context.getColor(R.color.severity_info);
            default:
                return context.getColor(R.color.severity_low);
        }
    }

    public static String labelFor(String severity) {
        if (severity == null || severity.trim().isEmpty()) {
            return "Low";
        }
        switch (severity.toLowerCase()) {
            case "critical":
            case "crit":
                return "Critical";
            case "high":
                return "High";
            case "medium":
            case "med":
                return "Medium";
            case "low":
                return "Low";
            case "info":
                return "Info";
            default:
                return severity;
        }
    }

    @ColorInt
    public static int statusColor(Context context, String status) {
        if (status == null) {
            return context.getColor(R.color.status_open);
        }
        switch (status.toLowerCase()) {
            case "closed":
            case "resolved":
                return context.getColor(R.color.status_closed);
            case "in_progress":
            case "investigating":
                return context.getColor(R.color.status_in_progress);
            case "active":
            case "open":
            default:
                return context.getColor(R.color.status_open);
        }
    }
}
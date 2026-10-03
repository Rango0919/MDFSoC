package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class VirusTotalInfo {

    @SerializedName("ip")
    private String ip;

    @SerializedName("malicious")
    private int malicious;

    @SerializedName("suspicious")
    private int suspicious;

    @SerializedName("undetected")
    private int undetected;

    @SerializedName("harmless")
    private int harmless;

    @SerializedName("total_engine_verdicts")
    private int totalEngineVerdicts;

    @SerializedName("reputation")
    private int reputation;

    @SerializedName("vt_link")
    private String vtLink;

    @SerializedName("last_analysis")
    private String lastAnalysis;

    @SerializedName("verdict")
    private String verdict;

    public String getIp() {
        return ip;
    }

    public int getMalicious() {
        return malicious;
    }

    public int getSuspicious() {
        return suspicious;
    }

    public int getUndetected() {
        return undetected;
    }

    public int getHarmless() {
        return harmless;
    }

    public int getTotalEngineVerdicts() {
        return totalEngineVerdicts;
    }

    public int getReputation() {
        return reputation;
    }

    public String getVtLink() {
        return vtLink;
    }

    public String getLastAnalysis() {
        return lastAnalysis;
    }

    public String getVerdict() {
        return verdict;
    }
}
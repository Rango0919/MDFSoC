package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VirusTotalUrlInfo {

    @SerializedName("url")
    private String url;

    @SerializedName("final_url")
    private String finalUrl;

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

    @SerializedName("categories")
    private List<String> categories;

    @SerializedName("redirect_chain")
    private List<String> redirectChain;

    @SerializedName("expanded_report")
    private VirusTotalUrlInfo expandedReport;

    @SerializedName("vt_link")
    private String vtLink;

    @SerializedName("last_analysis")
    private String lastAnalysis;

    @SerializedName("verdict")
    private String verdict;

    public String getUrl() {
        return url;
    }

    public String getFinalUrl() {
        return finalUrl;
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

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getRedirectChain() {
        return redirectChain;
    }

    public VirusTotalUrlInfo getExpandedReport() {
        return expandedReport;
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
package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ThreatIntelligence {

    @SerializedName("ip")
    private String ip;

    @SerializedName("threat_status")
    private String threatStatus;

    @SerializedName("abuseipdb")
    private IpReputation abuseIpdb;

    @SerializedName("virustotal")
    private VirusTotalInfo virusTotal;

    @SerializedName("mitre")
    private MitreInfo mitre;

    @SerializedName("sources")
    private List<String> sources;

    public String getIp() {
        return ip;
    }

    public String getThreatStatus() {
        return threatStatus;
    }

    public IpReputation getAbuseIpdb() {
        return abuseIpdb;
    }

    public VirusTotalInfo getVirusTotal() {
        return virusTotal;
    }

    public MitreInfo getMitre() {
        return mitre;
    }

    public List<String> getSources() {
        return sources;
    }
}
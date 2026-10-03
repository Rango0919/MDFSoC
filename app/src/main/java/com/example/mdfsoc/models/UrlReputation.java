package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UrlReputation {

    @SerializedName("url")
    private String url;

    @SerializedName("final_url")
    private String finalUrl;

    @SerializedName("host")
    private String host;

    @SerializedName("resolved_ips")
    private List<String> resolvedIps;

    @SerializedName("redirect_chain")
    private List<RedirectHop> redirectChain;

    @SerializedName("redirect_count")
    private int redirectCount;

    @SerializedName("shortened")
    private boolean shortened;

    @SerializedName("reachable")
    private boolean reachable;

    @SerializedName("final_status")
    private int finalStatus;

    @SerializedName("threat_status")
    private String threatStatus;

    @SerializedName("virustotal")
    private VirusTotalUrlInfo virusTotal;

    @SerializedName("sources")
    private List<String> sources;

    public String getUrl() {
        return url;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public String getHost() {
        return host;
    }

    public List<String> getResolvedIps() {
        return resolvedIps;
    }

    public List<RedirectHop> getRedirectChain() {
        return redirectChain;
    }

    public int getRedirectCount() {
        return redirectCount;
    }

    public boolean isShortened() {
        return shortened;
    }

    public boolean isReachable() {
        return reachable;
    }

    public int getFinalStatus() {
        return finalStatus;
    }

    public String getThreatStatus() {
        return threatStatus;
    }

    public VirusTotalUrlInfo getVirusTotal() {
        return virusTotal;
    }

    public List<String> getSources() {
        return sources;
    }

    public String getPrimaryIp() {
        if (resolvedIps == null || resolvedIps.isEmpty()) {
            return null;
        }
        for (String ip : resolvedIps) {
            if (ip != null && !ip.contains(":")) {
                return ip;
            }
        }
        return resolvedIps.get(0);
    }

    public boolean wasRedirected() {
        return finalUrl != null && url != null && !finalUrl.equals(url);
    }
}
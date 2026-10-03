package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class IpReputation {

    @SerializedName("ip")
    private String ip;

    @SerializedName("is_public")
    private boolean isPublic;

    @SerializedName("abuse_confidence_score")
    private int abuseConfidenceScore;

    @SerializedName("total_reports")
    private int totalReports;

    @SerializedName("country_code")
    private String countryCode;

    @SerializedName("country_name")
    private String countryName;

    @SerializedName("isp")
    private String isp;

    @SerializedName("domain")
    private String domain;

    @SerializedName("usage_type")
    private String usageType;

    @SerializedName("is_whitelisted")
    private Boolean isWhitelisted;

    @SerializedName("last_reported_at")
    private String lastReportedAt;

    @SerializedName("num_distinct_users")
    private int numDistinctUsers;

    public String getIp() {
        return ip;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public int getAbuseConfidenceScore() {
        return abuseConfidenceScore;
    }

    public int getTotalReports() {
        return totalReports;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getIsp() {
        return isp;
    }

    public String getDomain() {
        return domain;
    }

    public String getUsageType() {
        return usageType;
    }

    public Boolean getIsWhitelisted() {
        return isWhitelisted;
    }

    public String getLastReportedAt() {
        return lastReportedAt;
    }

    public int getNumDistinctUsers() {
        return numDistinctUsers;
    }

    public String getCountryLabel() {
        if (countryName != null && !countryName.isEmpty()) {
            return countryName;
        }
        if (countryCode != null && !countryCode.isEmpty()) {
            return countryCode;
        }
        return null;
    }

    public String getIspLabel() {
        if (isp != null && !isp.isEmpty()) {
            return isp;
        }
        return null;
    }
}
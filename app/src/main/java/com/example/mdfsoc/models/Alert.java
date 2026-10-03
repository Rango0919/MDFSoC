package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class Alert {

    @SerializedName("id")
    private long id;

    @SerializedName("severity")
    private String severity;

    @SerializedName("title")
    private String title;

    @SerializedName("source_ip")
    private String sourceIp;

    @SerializedName("dest_ip")
    private String destIp;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("rule_id")
    private String ruleId;

    @SerializedName("rule_level")
    private int ruleLevel;

    @SerializedName("agent")
    private String agent;

    @SerializedName("mitre")
    private MitreInfo mitre;

    @SerializedName("mitre_technique")
    private String mitreTechnique;

    @SerializedName("investigation_status")
    private String investigationStatus;

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

    public String getDestIp() {
        return destIp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getRuleId() {
        return ruleId;
    }

    public int getRuleLevel() {
        return ruleLevel;
    }

    public String getAgent() {
        return agent;
    }

    public MitreInfo getMitre() {
        return mitre;
    }

    public String getMitreTechnique() {
        if (mitreTechnique != null && !mitreTechnique.isEmpty()) {
            return mitreTechnique;
        }
        if (mitre != null) {
            return mitre.getTechniqueId() + " \u2014 " + mitre.getTechniqueName();
        }
        return null;
    }

    public String getInvestigationStatus() {
        return investigationStatus;
    }
}
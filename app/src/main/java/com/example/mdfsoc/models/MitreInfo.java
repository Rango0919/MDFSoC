package com.example.mdfsoc.models;

import com.google.gson.annotations.SerializedName;

public class MitreInfo {

    @SerializedName("technique_id")
    private String techniqueId;

    @SerializedName("technique_name")
    private String techniqueName;

    @SerializedName("tactic")
    private String tactic;

    @SerializedName("link")
    private String link;

    public String getTechniqueId() {
        return techniqueId;
    }

    public String getTechniqueName() {
        return techniqueName;
    }

    public String getTactic() {
        return tactic;
    }

    public String getLink() {
        return link;
    }
}
package com.example.mobilneaplikacije.data.model;

public class AllianceInvite {
    public String allianceId;
    public String allianceName;
    public String from;
    public String status;


    public AllianceInvite() {}
    public AllianceInvite(String allianceId, String from, String status) {
        this.allianceId = allianceId;
        this.from = from;
        this.status = status;
    }
}
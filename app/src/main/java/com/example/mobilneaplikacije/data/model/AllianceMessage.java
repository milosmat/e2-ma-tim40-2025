package com.example.mobilneaplikacije.data.model;

import java.util.Date;

public class AllianceMessage {
    public String id;
    public String allianceId;
    public String senderUid;
    public String senderUsername;
    public String text;
    public Date createdAt;

    public AllianceMessage() {}

    public AllianceMessage(String id, String allianceId, String senderUid, String senderUsername, String text, Date createdAt) {
        this.id = id;
        this.allianceId = allianceId;
        this.senderUid = senderUid;
        this.senderUsername = senderUsername;
        this.text = text;
        this.createdAt = createdAt;
    }
}

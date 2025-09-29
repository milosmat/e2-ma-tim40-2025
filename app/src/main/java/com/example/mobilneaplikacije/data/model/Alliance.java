package com.example.mobilneaplikacije.data.model;

public class Alliance {
    public String id;
    public String name;
    public String leaderUid;
    public String IsSpecialMissionActive; // samo privremeno jer trenutno nema spec misija

    public Alliance() {}
    public Alliance(String id, String name, String leaderUid, String isSpecialMissionActive) {
        this.id = id;
        this.name = name;
        this.leaderUid = leaderUid;
        this.IsSpecialMissionActive = isSpecialMissionActive;
    }
}

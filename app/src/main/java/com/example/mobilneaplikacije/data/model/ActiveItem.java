package com.example.mobilneaplikacije.data.model;

import com.google.firebase.Timestamp;

public class ActiveItem {
    public String id;
    public String itemId;
    public double valuePct;
    public int remainingBattles;
    public Timestamp activatedAt;

    public ActiveItem() {}
}

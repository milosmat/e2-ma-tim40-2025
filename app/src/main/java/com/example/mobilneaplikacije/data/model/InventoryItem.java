package com.example.mobilneaplikacije.data.model;

import com.google.firebase.Timestamp;

public class InventoryItem {
    public String id;
    public String itemId;
    public int quantity;
    public boolean active;
    public int remainingBattles;
    public int upgradeLevel;
    public double dropChance;
    public Timestamp acquiredAt;

    public InventoryItem() {}
}
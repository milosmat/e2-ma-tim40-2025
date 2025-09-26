package com.example.mobilneaplikacije.data.model;

public class Item {
    public enum Type { POTION, CLOTHES, WEAPON }
    public enum Effect { INCREASE_PP, INCREASE_SUCCESS, EXTRA_ATTACK, EXTRA_COINS }

    public String id;
    public String name;
    public Type type;
    public Effect effect;
    public double valuePct;
    public int durationBattles;
    public boolean consumable;
    public double pricePctOfPrevBossReward;
    public String availability;
    public boolean stackable;
    public String imageResName;

    public Item() {}
}
package com.example.mobilneaplikacije.data.model;

public class Equipment {
    public enum Type { POTION, CLOTHES, WEAPON }
    public enum Effect { INCREASE_PP, INCREASE_SUCCESS, EXTRA_ATTACK, EXTRA_COINS }

    private String name;
    private Type type;
    private Effect effect;
    private double value;      // npr. +0.1 za +10%
    private int duration;      // -1 = trajno, >0 = broj borbi
    private boolean consumable;
    private int upgradeLevel = 0; // za oružje
    private boolean active = false;
    public Equipment(String name, Type type, Effect effect,
                     double value, int duration, boolean consumable) {
        this.name = name;
        this.type = type;
        this.effect = effect;
        this.value = value;
        this.duration = duration;
        this.consumable = consumable;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public Effect getEffect() { return effect; }
    public double getValue() { return value; }
    public int getDuration() { return duration; }
    public void setDuration(int d) { this.duration = d; }
    public boolean isConsumable() { return consumable; }
    public int getUpgradeLevel() { return upgradeLevel; }
    public void upgrade() { upgradeLevel++; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

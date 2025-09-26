package com.example.mobilneaplikacije.data.model;

import java.sql.Time;
import java.sql.Timestamp;

public class Player {
    private String username;
    private String avatar;
    private int level;
    private String title;
    private int pp;
    private int xp;
    private int coins;
    private double successRate;
    private long createdAt;
    private long lastLevelUpAt;
    public Player() {}
    public Player(String username, String avatar, int level, String title,
                  int pp, int xp, int coins, double successRate) {
        this.username = username;
        this.avatar = avatar;
        this.level = level;
        this.title = title;
        this.pp = pp;
        this.xp = xp;
        this.coins = coins;
        this.successRate = successRate;
        this.createdAt = System.currentTimeMillis();
        this.lastLevelUpAt = 0L;
    }

    // --- getteri i setteri ---
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPp() { return pp; }
    public void setPp(int pp) { this.pp = pp; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public long getLastLevelUpAt() {
        return lastLevelUpAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setLastLevelUpAt(long lastLevelUpAt) {
        this.lastLevelUpAt = lastLevelUpAt;
    }
}

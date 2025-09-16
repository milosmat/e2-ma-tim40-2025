package com.example.mobilneaplikacije.data.model;

public class Player {
    private String username;
    private String avatar;       // path ili ime fajla avatara
    private int level;           // trenutni nivo
    private String title;        // titula (npr. “Početnik”)
    private int pp;              // Power Points
    private int xp;              // Experience Points
    private int coins;           // broj novčića
    private double successRate;  // uspešnost u %

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

    // Hook za kasniju opremu
    public void applyEquipmentEffects() {
        // TODO kad se implementira oprema
    }

}

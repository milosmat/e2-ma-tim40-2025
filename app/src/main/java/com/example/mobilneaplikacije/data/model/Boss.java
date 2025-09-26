package com.example.mobilneaplikacije.data.model;

public class Boss {
    private int id;
    private int level;
    private int hp;
    private int maxHp;
    private int coinsReward;

    public Boss(int level, int prevHp, int prevCoins) {
        this.level = level;
        this.id = level;

        if (level == 1) {
            this.hp = 200;
            this.maxHp = 200;
            this.coinsReward = 200;
        } else {
            this.hp = prevHp * 2 + prevHp / 2;
            this.maxHp = this.hp;
            this.coinsReward = (int) Math.round(prevCoins * 1.2);
        }
    }

    public int getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public void setCoinsReward(int coinsReward) {
        this.coinsReward = coinsReward;
    }

    public void takeDamage(int damage) {
        hp = Math.max(0, hp - damage);
    }

    public boolean isDefeated() {
        return hp <= 0;
    }

    public int getCoinsReward() {
        return coinsReward;
    }

    public int getMaxHp() {
        return maxHp;
    }
}

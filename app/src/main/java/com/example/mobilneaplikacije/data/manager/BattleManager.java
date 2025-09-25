package com.example.mobilneaplikacije.data.manager;

import com.example.mobilneaplikacije.data.model.Boss;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;

import java.util.List;
import java.util.Random;

public class BattleManager {
    private Player player;
    private Boss boss;
    private int attemptsLeft = 5;
    private int maxAttempts = 5;   // default
    private double coinsMultiplier = 1.0; // za oružje (luk i strela)

    private boolean finished = false;

    // nova polja za nagrade
    private int finalCoins = 0;
    private boolean equipmentDropped = false;
    private boolean weapon = false;

    public BattleManager(Player player, Boss boss, List<Equipment> activeItems) {
        this.player = player;
        this.boss = boss;
        applyEquipment(player, activeItems);
        this.attemptsLeft = maxAttempts;
    }


    public String attack() {
        if (finished) return "Borba je završena!";
        if (attemptsLeft <= 0) return "Nema više napada!";

        Random rand = new Random();
        int roll = rand.nextInt(101); // 0-100
        String result;

        if (roll < player.getSuccessRate()) {
            boss.takeDamage(player.getPp());
            result = "Uspešan napad! Bosu je oduzeto " + player.getPp() + " HP.";
        } else {
            result = "Promašaj!";
        }

        attemptsLeft--;

        if (boss.isDefeated() || attemptsLeft == 0) {
            finished = true;
            result += "\n" + resolveRewards();
        }

        return result;
    }

    private String resolveRewards() {
        Random rand = new Random();

        if (boss.isDefeated()) {
            finalCoins = (int) (boss.getCoinsReward() * coinsMultiplier);

            // 20% šanse za opremu
            if (rand.nextInt(100) < 20) {
                equipmentDropped = true;
                // 5% šansa za weapon, 95% za odeću
                weapon = rand.nextInt(100) < 5;
            }

            return "Pobeda! Osvojeno " + finalCoins + " novčića.";
        } else if ((double) boss.getHp() <= boss.getMaxHp() / 2) {
            finalCoins = boss.getCoinsReward() / 2;

            // 10% šanse za opremu (prepolovljeno)
            if (rand.nextInt(100) < 10) {
                equipmentDropped = true;
                weapon = rand.nextInt(100) < 5;
            }

            return "Boss nije poražen, ali je oslabljen. Osvojeno " + finalCoins + " novčića.";
        } else {
            finalCoins = 0;
            return "Poraz! Nema nagrade.";
        }
    }

    private void applyEquipment(Player player, List<Equipment> activeItems) {
        for (Equipment e : activeItems) {
            switch (e.getEffect()) {
                case INCREASE_PP:
                    player.setPp((int)(player.getPp() * (1 + e.getValue())));
                    break;
                case INCREASE_SUCCESS:
                    player.setSuccessRate(player.getSuccessRate() + e.getValue() * 100);
                    break;
                case EXTRA_ATTACK:
                    maxAttempts += 1; // čizme daju +1 napad
                    break;
                case EXTRA_COINS:
                    coinsMultiplier += e.getValue(); // luk i strela
                    break;
            }

            // smanji trajanje ako je privremeno
            if (e.getDuration() > 0) e.setDuration(e.getDuration() - 1);
        }

        // očisti potrošene ili istekle
        activeItems.removeIf(e -> e.isConsumable() || e.getDuration() == 0);

    }

    // ---- Getteri za UI ----
    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getFinalCoins() {
        return finalCoins;
    }

    public boolean hasEquipment() {
        return equipmentDropped;
    }

    public boolean isWeapon() {
        return weapon;
    }
    public int getMaxAttempts() {
        return maxAttempts;
    }
}

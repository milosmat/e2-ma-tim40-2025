package com.example.mobilneaplikacije.data.manager;
// KLASA KOJA SE BRISE KAD SE DODA REGISTRACIJA LOGIN ITD..

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mobilneaplikacije.data.model.Boss;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.lang.reflect.Type;
import java.util.List;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PP = "pp";
    private static final String KEY_XP = "xp";
    private static final String KEY_COINS = "coins";
    private static final String KEY_SUCCESS_RATE = "successRate";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_ACTIVE_EQUIPMENT = "active_equipment";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void savePlayer(Player player) {
        editor.putString(KEY_USERNAME, player.getUsername());
        editor.putString(KEY_AVATAR, player.getAvatar());
        editor.putInt(KEY_LEVEL, player.getLevel());
        editor.putString(KEY_TITLE, player.getTitle());
        editor.putInt(KEY_PP, player.getPp());
        editor.putInt(KEY_XP, player.getXp());
        editor.putInt(KEY_COINS, player.getCoins());
        editor.putFloat(KEY_SUCCESS_RATE, (float) player.getSuccessRate());
        editor.apply();
    }

    public Player getPlayer() {
        String username = prefs.getString(KEY_USERNAME, "Guest");
        String avatar = prefs.getString(KEY_AVATAR, "default_avatar");
        int level = prefs.getInt(KEY_LEVEL, 1);
        String title = prefs.getString(KEY_TITLE, "Početnik");
        int pp = prefs.getInt(KEY_PP, 40);
        int xp = prefs.getInt(KEY_XP, 0);
        int coins = prefs.getInt(KEY_COINS, 0);
        double successRate = prefs.getFloat(KEY_SUCCESS_RATE, 67f);
        return new Player(username, avatar, level, title, pp, xp, coins, successRate);
    }

    public void clear() {
        editor.clear().apply();
    }

    public void saveBossState(Boss boss) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("boss_id", boss.getId());
        editor.putInt("boss_hp", boss.getHp());
        editor.putInt("boss_max_hp", boss.getMaxHp());
        editor.putInt("boss_reward", boss.getCoinsReward());
        editor.apply();
    }

    public Boss getBossState(Player player) {
        int id = prefs.getInt("boss_id", -1);
        int hp = prefs.getInt("boss_hp", -1);
        int maxHp = prefs.getInt("boss_max_hp", -1);
        int reward = prefs.getInt("boss_reward", -1);

        // Ako nema sačuvanog bossa → kreiraj novog za trenutni level
        if (id == -1 || hp <= 0) {
            int level = player.getLevel();
            // prvi boss
            if (level == 1) {
                return new Boss(1, 0, 0);
            } else {
                // generiši novog bossa prema formuli
                Boss prevBoss = new Boss(level - 1, 0, 0);
                return new Boss(level, prevBoss.getMaxHp(), prevBoss.getCoinsReward());
            }
        }

        // Ako postoji sačuvan, vrati njegovo stanje
        Boss b = new Boss(id, 0, 0);
        b.setHp(hp);
        b.setMaxHp(maxHp);
        b.setCoinsReward(reward);
        return b;
    }

    public void clearBossState() {
        prefs.edit()
                .remove("boss_id")
                .remove("boss_hp")
                .remove("boss_max_hp")
                .remove("boss_reward")
                .apply();
    }

    // spremi inventory
    public void saveInventory(List<Equipment> items) {
        String json = new Gson().toJson(items);
        prefs.edit().putString(KEY_INVENTORY, json).apply();
    }

    public List<Equipment> getInventory() {
        String json = prefs.getString(KEY_INVENTORY, "[]");
        Type listType = new TypeToken<List<Equipment>>(){}.getType();
        return new Gson().fromJson(json, listType);
    }

    // aktivna oprema
    public void saveActiveEquipment(List<Equipment> active) {
        String json = new Gson().toJson(active);
        prefs.edit().putString(KEY_ACTIVE_EQUIPMENT, json).apply();
    }

    public List<Equipment> getActiveEquipment() {
        String json = prefs.getString(KEY_ACTIVE_EQUIPMENT, "[]");
        Type listType = new TypeToken<List<Equipment>>(){}.getType();
        return new Gson().fromJson(json, listType);
    }
    public void giveTestPlayer() {
        Player player = getPlayer();
        player.setXp(198); // odmah na ivici prvog levela
        player.setLevel(1);
        player.setTitle("Početnik");
        player.setPp(40); // default PP
        savePlayer(player);
    }
    public void giveAllTestEquipment() {
        List<Equipment> items = getInventory();

        // --- Napici ---
        items.add(new Equipment("Napitak +20% PP", Equipment.Type.POTION,
                Equipment.Effect.INCREASE_PP, 0.2, 1, true));

        items.add(new Equipment("Napitak +40% PP", Equipment.Type.POTION,
                Equipment.Effect.INCREASE_PP, 0.4, 1, true));

        items.add(new Equipment("Napitak trajno +5% PP", Equipment.Type.POTION,
                Equipment.Effect.INCREASE_PP, 0.05, -1, false));

        items.add(new Equipment("Napitak trajno +10% PP", Equipment.Type.POTION,
                Equipment.Effect.INCREASE_PP, 0.10, -1, false));

        // --- Odeća ---
        items.add(new Equipment("Rukavice (+10% PP)", Equipment.Type.CLOTHES,
                Equipment.Effect.INCREASE_PP, 0.1, 2, false));

        items.add(new Equipment("Štit (+10% success)", Equipment.Type.CLOTHES,
                Equipment.Effect.INCREASE_SUCCESS, 0.1, 2, false));

        items.add(new Equipment("Čizme (+1 napad)", Equipment.Type.CLOTHES,
                Equipment.Effect.EXTRA_ATTACK, 1.0, 2, false));

        // --- Oružje ---
        items.add(new Equipment("Mač (+5% PP)", Equipment.Type.WEAPON,
                Equipment.Effect.INCREASE_PP, 0.05, -1, false));

        items.add(new Equipment("Luk i strela (+5% coins)", Equipment.Type.WEAPON,
                Equipment.Effect.EXTRA_COINS, 0.05, -1, false));

        saveInventory(items);
    }


    public void updateActiveEquipment(List<Equipment> updated) {
        saveActiveEquipment(updated); // overwrite stara lista
    }
}
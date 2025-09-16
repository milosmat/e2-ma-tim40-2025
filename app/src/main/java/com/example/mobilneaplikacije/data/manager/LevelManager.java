package com.example.mobilneaplikacije.data.manager;

import com.example.mobilneaplikacije.data.model.Player;

public class LevelManager {

    // XP potreban za level
    public static int getXpForLevel(int level) {
        if (level == 1) return 200; // prvi prag
        int prevXp = getXpForLevel(level - 1);
        int raw = prevXp * 2 + prevXp / 2;
        return ((raw + 99) / 100) * 100; // zaokruži na prvu stotinu
    }

    // PP dobijen na ovom nivou
    public static int getPpForLevel(int level) {
        if (level == 1) return 40;
        int prevPp = getPpForLevel(level - 1);
        return Math.round(prevPp + (prevPp * 0.75f));
    }

    // Titule
    public static String getTitleForLevel(int level) {
        switch (level) {
            case 1: return "Početnik";
            case 2: return "Učenik";
            case 3: return "Majstor";
            default: return "Legenda";
        }
    }

    // Proveri da li je igrač prešao nivo
    public static boolean checkLevelUp(Player player) {
        int nextLevelXp = getXpForLevel(player.getLevel());
        if (player.getXp() >= nextLevelXp) {
            player.setLevel(player.getLevel() + 1);
            player.setPp(getPpForLevel(player.getLevel()));
            player.setTitle(getTitleForLevel(player.getLevel()));
            return true;
        }
        return false;
    }
}

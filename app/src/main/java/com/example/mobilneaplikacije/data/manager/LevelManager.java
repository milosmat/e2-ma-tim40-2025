package com.example.mobilneaplikacije.data.manager;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.data.repository.PlayerRepository;

public class LevelManager {

    private PlayerRepository playerRepo;

    public LevelManager(){
        playerRepo = new PlayerRepository();
    }
    public static int getXpForNextLevel(int level) {
        if (level == 1) return 200;
        int prevXp = getXpForNextLevel(level - 1);
        int raw = prevXp * 2 + prevXp / 2;
        return ((raw + 99) / 100) * 100;
    }

    public static int getPpForLevel(int level) {
        if (level == 1) return 40;
        int prevPp = getPpForLevel(level - 1);
        return Math.round(prevPp + (prevPp * 0.75f));
    }

    public static String getTitleForLevel(int level) {
        switch (level) {
            case 1: return "Pocetnik";
            case 2: return "Ucenik";
            case 3: return "Majstor";
            default: return "Legenda";
        }
    }
    public boolean checkLevelUp(Player player) {
        int nextLevelXp = getXpForNextLevel(player.getLevel());
        if (player.getXp() >= nextLevelXp) {
            player.setLevel(player.getLevel() + 1);
            player.setPp(getPpForLevel(player.getLevel()));
            player.setTitle(getTitleForLevel(player.getLevel()));
            return true;
        }
        return false;
    }
    public interface LevelUpCallback {
        void onLevelUp(Player player);
    }
    public void getTaskDifficultyXp(String difficulty, final XpCallback callback) {
        playerRepo.loadPlayer(new PlayerRepository.PlayerCallback() {
            @Override
            public void onSuccess(Player player) {
                int level = player.getLevel();
                int xp = 0;
                switch (difficulty) {
                    case "VEOMA_LAK":
                        xp = 1;
                        break;
                    case "LAK":
                        xp = 3;
                        break;
                    case "TEZAK":
                        xp = 7;
                        break;
                    case "EKSTREMNO_TEZAK":
                        xp = 20;
                        break;
                    default:
                        xp = 0;
                }

                for (int i = 2; i <= level; i++) {
                    xp += Math.round(xp / 2.0f);
                }
                callback.onSucces(xp);
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }

    public void getTaskImportanceXp(String importance, final XpCallback callback) {
        playerRepo.loadPlayer(new PlayerRepository.PlayerCallback() {
            @Override
            public void onSuccess(Player player) {
                int level = player.getLevel();
                int xp = 0;
                switch (importance) {
                    case "NORMALAN":
                        xp = 1;
                        break;
                    case "VAŽAN":
                        xp = 3;
                        break;
                    case "EKSTREMNO_VAŽAN":
                        xp = 10;
                        break;
                    case "SPECIJALAN":
                        xp = 100;
                        break;
                    default:
                        xp = 0;
                }
                for (int i = 2; i <= level; i++) {
                    xp += Math.round(xp / 2.0f);
                }
                callback.onSucces(xp);
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e.getMessage());
            }
        });
    }
    public void addXp(int deltaXp, @Nullable LevelUpCallback callback) {
        playerRepo.loadPlayer(new PlayerRepository.PlayerCallback() {
            @Override
            public void onSuccess(Player currentPlayer) {
                int newXp = currentPlayer.getXp() + deltaXp;
                currentPlayer.setXp(newXp);

                boolean leveledUp = checkLevelUp(currentPlayer);
                playerRepo.syncWithDatabase();

                if (leveledUp && callback != null) {
                    callback.onLevelUp(currentPlayer);
                }
            }
            @Override
            public void onFailure(Exception e) {}
        });
    }
    public interface XpCallback {
        void onSucces(int xp);
        void onFailure(String errorMessage);
    }

}

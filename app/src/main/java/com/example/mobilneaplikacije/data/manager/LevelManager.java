package com.example.mobilneaplikacije.data.manager;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.data.repository.PlayerRepository;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

import java.util.Date;

public class LevelManager {

    private PlayerRepository playerRepo;
    private final TaskRepository taskRepo;
    private final BattleManager battleManager;
    public LevelManager(){
        playerRepo = new PlayerRepository();
        taskRepo  = new TaskRepository();
        battleManager = new BattleManager();
    }
    public static int getXpForNextLevel(int level) {
        if (level == 1) return 200;
        int prevXp = getXpForNextLevel(level - 1);
        int raw = prevXp * 2 + prevXp / 2;
        return ((raw + 99) / 100) * 100;
    }

    public static int getPpForLevel(int level) {
        if (level == 2) return 40;
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
    private boolean checkLevelUpAndFinalizeStage(Player p) {
        boolean leveled = false;
        while (p.getXp() >= getXpForNextLevel(p.getLevel())) {
            long now = System.currentTimeMillis();
            long stageStart = (p.getLevel() <= 1 )
                    ? p.getCreatedAt()
                    : p.getLastLevelUpAt();
            Log.d("LevelUp", "Stage window: " + new Date(stageStart) + " → " + new Date(now));
            if (p.getLevel() <= 1) {
                Log.d("LevelUp", "(Level 1) Using createdAt=" + p.getCreatedAt() + " as stage start");
            } else {
                Log.d("LevelUp", "(Level>1) Using lastLevelUpAt=" + p.getLastLevelUpAt());
            }
            taskRepo.calculateSuccessRate(stageStart, now, new TaskRepository.Callback<Double>() {
                @Override public void onSuccess(Double successPercent) {
                    int pct = (successPercent == null) ? 50 : (int) Math.round(successPercent);
                    battleManager.prepareBossAfterLevelUp(pct, now, new BattleManager.Callback<Void>() {
                        @Override public void onSuccess(Void v) { }
                        @Override public void onError(Exception e) { }
                    });
                }
                @Override public void onError(Exception e) {
                    battleManager.prepareBossAfterLevelUp(50, now, new BattleManager.Callback<Void>() {
                        @Override public void onSuccess(Void v) { }
                        @Override public void onError(Exception ex) { }
                    });
                }
            });

            p.setLevel(p.getLevel() + 1);
            p.setPp(getPpForLevel(p.getLevel()));
            p.setTitle(getTitleForLevel(p.getLevel()));
            p.setLastLevelUpAt(now);

            leveled = true;
        }
        return leveled;
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
                    case "VAZAN":
                        xp = 3;
                        break;
                    case "EKSTREMNO_VAZAN":
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
                currentPlayer.setXp(currentPlayer.getXp() + deltaXp);

                boolean leveledUp = checkLevelUpAndFinalizeStage(currentPlayer);
                playerRepo.syncWithDatabase();

                if (leveledUp && callback != null) callback.onLevelUp(currentPlayer);
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    public interface XpCallback {
        void onSucces(int xp);
        void onFailure(String errorMessage);
    }

}

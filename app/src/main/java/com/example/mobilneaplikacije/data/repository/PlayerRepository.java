package com.example.mobilneaplikacije.data.repository;

import com.example.mobilneaplikacije.data.manager.LevelManager;
import com.example.mobilneaplikacije.data.model.Player;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

public class PlayerRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static Player cachedPlayer = null;
    private static String cachedUid = null;

    public interface LongCallback { void onResult(long v); void onError(Exception e); }
    public interface VoidCallback { void onSuccess(); void onError(Exception e); }
    public interface PlayerCallback { void onSuccess(Player player); void onFailure(Exception e); }

    public PlayerRepository(){
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static void invalidateCache() {
        cachedPlayer = null;
        cachedUid = null;
    }

    private String currentUid() throws IllegalStateException {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("Nema ulogovanog korisnika");
        return u.getUid();
    }

    public void loadPlayer(final PlayerCallback callback) {
        String uid;
        try {
            uid = currentUid();
        } catch (IllegalStateException e) {
            callback.onFailure(e);
            return;
        }

        if (cachedPlayer != null && uid.equals(cachedUid)) {
            callback.onSuccess(cachedPlayer);
            return;
        }

        db.collection("users").document(uid)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Player p = new Player(
                                doc.getString("username"),
                                doc.getString("avatar"),
                                doc.getLong("level") == null ? 1 : doc.getLong("level").intValue(),
                                doc.getString("title"),
                                doc.getLong("pp") == null ? 0 : doc.getLong("pp").intValue(),
                                doc.getLong("xp") == null ? 0 : doc.getLong("xp").intValue(),
                                doc.getLong("coins") == null ? 0 : doc.getLong("coins").intValue(),
                                doc.getDouble("successRate") == null ? 0.0 : doc.getDouble("successRate")
                        );
                        cachedPlayer = p;
                        cachedUid = uid;
                        callback.onSuccess(p);
                    } else {
                        callback.onFailure(new Exception("Korisnicki podaci nisu pronadjeni"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void syncWithDatabase() {
        String uid;
        try {
            uid = currentUid();
        } catch (IllegalStateException e) {
            return;
        }
        if (cachedPlayer == null) return;

        db.collection("users").document(uid)
                .update("username", cachedPlayer.getUsername(),
                        "avatar", cachedPlayer.getAvatar(),
                        "level", cachedPlayer.getLevel(),
                        "title", cachedPlayer.getTitle(),
                        "pp", cachedPlayer.getPp(),
                        "xp", cachedPlayer.getXp(),
                        "coins", cachedPlayer.getCoins(),
                        "successRate", cachedPlayer.getSuccessRate());
    }
  
    public void getCurrentPP(LongCallback cb) {
        String uid;
        try { uid = currentUid(); } catch (IllegalStateException e) { cb.onError(e); return; }

        if (cachedPlayer != null && uid.equals(cachedUid) && cachedPlayer.getPp() > 0) {
            cb.onResult(cachedPlayer.getPp());
            return;
        }

        db.collection("users").document(uid)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onError(new Exception("User not found")); return; }

                    Long lvlL = d.getLong("level");
                    int level = (lvlL == null) ? 1 : lvlL.intValue();

                    Long ppL = d.getLong("pp");
                    int pp = (ppL == null) ? 0 : ppL.intValue();

                    if (pp <= 0) {
                        int fixed = LevelManager.getPpForLevel(level);
                        d.getReference().update("pp", fixed, "updatedAt", FieldValue.serverTimestamp())
                                .addOnSuccessListener(v -> {
                                    if (cachedPlayer != null && uid.equals(cachedUid)) cachedPlayer.setPp(fixed);
                                    cb.onResult(fixed);
                                })
                                .addOnFailureListener(cb::onError);
                    } else {
                        if (cachedPlayer != null && uid.equals(cachedUid)) cachedPlayer.setPp(pp);
                        cb.onResult(pp);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    
    public void refreshSuccessRate(TaskRepository taskRepo, final PlayerCallback callback) {
        loadPlayer(new PlayerCallback() {
            @Override
            public void onSuccess(Player player) {
                taskRepo.calculateSuccessRate(new TaskRepository.Callback<Double>() {
                    @Override
                    public void onSuccess(Double rate) {
                        if (cachedPlayer != null) {
                            cachedPlayer.setSuccessRate(rate);
                            syncWithDatabase();
                            callback.onSuccess(cachedPlayer);
                        } else {
                            // fallback: učitaj opet, pa postavi
                            loadPlayer(new PlayerCallback() {
                                @Override public void onSuccess(Player p2) {
                                    p2.setSuccessRate(rate);
                                    cachedPlayer = p2;
                                    cachedUid = currentUid();
                                    syncWithDatabase();
                                    callback.onSuccess(p2);
                                }
                                @Override public void onFailure(Exception e) { callback.onFailure(e); }
                            });
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void addCoins(long delta, VoidCallback cb) {
        String uid;
        try { uid = currentUid(); } catch (IllegalStateException e) { cb.onError(e); return; }

        db.runTransaction(tr -> {
            var ref = db.collection("users").document(uid);
            var snap = tr.get(ref);
            Long coins = snap.getLong("coins");
            if (coins == null) coins = 0L;
            tr.update(ref, "coins", coins + delta);
            return null;
        }).addOnSuccessListener(v -> {
            if (cachedPlayer != null && uid.equals(cachedUid)) {
                cachedPlayer.setCoins(cachedPlayer.getCoins() + (int) delta);
            }
            cb.onSuccess();
        }).addOnFailureListener(cb::onError);
    }
}

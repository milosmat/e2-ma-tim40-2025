package com.example.mobilneaplikacije.data.repository;

import com.example.mobilneaplikacije.data.model.Player;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PlayerRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static Player cachedPlayer = null;
    private static String cachedUid = null;

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

    public Player getCurrentPlayer(){
        return cachedPlayer;
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

    public void refreshSuccessRate(TaskRepository taskRepo, final PlayerCallback callback) {
        loadPlayer(new PlayerCallback() {
            @Override
            public void onSuccess(Player player) {
                taskRepo.calculateSuccessRate(new TaskRepository.Callback<Double>() {
                    @Override
                    public void onSuccess(Double rate) {
                        cachedPlayer.setSuccessRate(rate);
                        syncWithDatabase();
                        callback.onSuccess(cachedPlayer);
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

    public interface PlayerCallback {
        void onSuccess(Player player);
        void onFailure(Exception e);
    }
}

package com.example.mobilneaplikacije.data.repository;


import com.example.mobilneaplikacije.data.model.Player;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PlayerRepository {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static Player currPlayer;
    private static boolean isLoaded =false;

    public PlayerRepository(){
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public Player getCurrentPlayer(){
        return currPlayer;
    }

    public void loadPlayer(final PlayerCallback callback) {
        if (isLoaded) {
            callback.onSuccess(currPlayer);
            return;
        }

        FirebaseUser usr = auth.getCurrentUser();
        if (usr != null) {
            db.collection("users").document(usr.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currPlayer = new Player(
                                    doc.getString("username"),
                                    doc.getString("avatar"),
                                    doc.getLong("level").intValue(),
                                    doc.getString("title"),
                                    doc.getLong("pp").intValue(),
                                    doc.getLong("xp").intValue(),
                                    doc.getLong("coins").intValue(),
                                    doc.getDouble("successRate")
                            );
                            isLoaded = true;
                            callback.onSuccess(currPlayer);
                        } else {
                            callback.onFailure(new Exception("Korisnicki podaci nisu pronadjenni"));
                        }
                    })
                    .addOnFailureListener(e -> callback.onFailure(e));
        } else {
            callback.onFailure(new Exception("Nema ulogovanog korisnika"));
        }
    }
    public void syncWithDatabase() {
        if (currPlayer != null) {
            FirebaseUser usr = auth.getCurrentUser();
            if (usr != null) {
                db.collection("users").document(usr.getUid())
                        .update("username", currPlayer.getUsername(),
                                "avatar", currPlayer.getAvatar(),
                                "level", currPlayer.getLevel(),
                                "title", currPlayer.getTitle(),
                                "pp", currPlayer.getPp(),
                                "xp", currPlayer.getXp(),
                                "coins", currPlayer.getCoins(),
                                "successRate", currPlayer.getSuccessRate());
            }
        }
    }

    public interface PlayerCallback {
        void onSuccess(Player player);
        void onFailure(Exception e);
    }

}

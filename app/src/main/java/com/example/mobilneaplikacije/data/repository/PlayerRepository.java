package com.example.mobilneaplikacije.data.repository;

import com.example.mobilneaplikacije.data.manager.LevelManager;
import com.example.mobilneaplikacije.data.model.Item;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.data.model.PublicProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static Player cachedPlayer = null;
    private static String cachedUid = null;

    public interface LongCallback { void onResult(long v); void onError(Exception e); }
    public interface VoidCallback { void onSuccess(); void onError(Exception e); }
    public interface PlayerCallback { void onSuccess(Player player); void onFailure(Exception e); }
    public interface PublicProfileCallback { void onSuccess(com.example.mobilneaplikacije.data.model.PublicProfile p); void onFailure(Exception e); }

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

    public void loadPublicProfile(String targetUid, final PublicProfileCallback cb) {
        db.collection("users").document(targetUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        if (cb != null) cb.onFailure(new Exception("User not found"));
                        return;
                    }

                    PublicProfile p = new PublicProfile();
                    p.uid = targetUid != null ? targetUid : "";
                    p.username = doc.getString("username") != null ? doc.getString("username") : "";
                    p.avatar = doc.getString("avatar") != null ? doc.getString("avatar") : "";
                    Long lvl = doc.getLong("level");
                    p.level = (lvl != null) ? lvl.intValue() : 1;
                    p.title = doc.getString("title") != null ? doc.getString("title") : "";

                    Long xp = doc.getLong("xp");
                    p.xp = (xp != null) ? xp.intValue() : 0;
                    db.collection("users").document(targetUid).collection("activeItems").get()
                            .addOnSuccessListener(as -> {
                                List<String> activeItemIds = new ArrayList<>();
                                for (DocumentSnapshot a : as) {
                                    String iid = a.getString("itemId");
                                    if (iid != null) activeItemIds.add(iid);
                                }
                                if (activeItemIds.isEmpty()) {
                                    if (cb != null) cb.onSuccess(p);
                                    return;
                                }
                                new CatalogRepository().getAll(new CatalogRepository.Callback<List<Item>>() {
                                    @Override public void onSuccess(@androidx.annotation.Nullable List<Item> data) {
                                        Map<String, Item> map = new HashMap<>();
                                        if (data != null) for (Item it : data) map.put(it.id, it);
                                        for (String iid : activeItemIds) {
                                            Item it = map.get(iid);
                                            if (it != null) p.activeItems.add(it);
                                        }
                                        if (cb != null) cb.onSuccess(p);
                                    }
                                    @Override public void onError(Exception e) {
                                        if (cb != null) cb.onSuccess(p);
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                if (cb != null) cb.onSuccess(p);
                            });
                })
                .addOnFailureListener(e -> {
                    if (cb != null) cb.onFailure(e);
                });
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

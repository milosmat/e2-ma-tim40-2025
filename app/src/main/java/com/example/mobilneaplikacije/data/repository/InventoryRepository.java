package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.ActiveItem;
import com.example.mobilneaplikacije.data.model.InventoryItem;
import com.example.mobilneaplikacije.data.model.Item;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class InventoryRepository {

    public interface Callback<T> { void onSuccess(@Nullable T data); void onError(Exception e); }
    private final FirebaseFirestore db;
    private final String uid;
    private static final double WEAPON_UPGRADE_STEP = 0.01;

    public InventoryRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser usr = FirebaseAuth.getInstance().getCurrentUser();
        if (usr == null) throw new IllegalStateException("User not logged in");
        this.uid = usr.getUid();
    }

    private DocumentReference userDoc() { return db.collection("users").document(uid); }
    private CollectionReference invCol() { return userDoc().collection("inventory"); }
    private CollectionReference activeCol() { return userDoc().collection("activeItems"); }
    private DocumentReference battleDoc() { return userDoc().collection("battle").document("current"); }

    public void listInventory(Callback<List<InventoryItem>> cb) {
        invCol().get().addOnSuccessListener(snap -> {
            List<InventoryItem> inventoryItems = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                InventoryItem itm = new InventoryItem();
                itm.id = d.getId();
                itm.itemId = d.getString("itemId");
                Long quantity = d.getLong("quantity");
                itm.quantity = quantity == null ? 0 : quantity.intValue();
                Boolean act = d.getBoolean("active");
                itm.active = Boolean.TRUE.equals(act);
                Long remBattles = d.getLong("remainingBattles");
                itm.remainingBattles = remBattles == null ? 0 : remBattles.intValue();
                Long up = d.getLong("upgradeLevel");
                itm.upgradeLevel = up == null ? 0 : up.intValue();
                Double dc = d.getDouble("dropChance");
                itm.dropChance = dc == null ? 0.0 : dc;
                inventoryItems.add(itm);
            }
            cb.onSuccess(inventoryItems);
        }).addOnFailureListener(cb::onError);
    }

    public void listActive(Callback<List<ActiveItem>> cb) {
        activeCol().get().addOnSuccessListener(snap -> {
            List<ActiveItem> activeItems = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                ActiveItem itm = new ActiveItem();
                itm.id = d.getId();
                itm.itemId = d.getString("itemId");
                Double vp = d.getDouble("valuePct");
                itm.valuePct = vp == null ? 0 : vp;
                Long rem = d.getLong("remainingBattles");
                itm.remainingBattles = rem == null ? 0 : rem.intValue();
                activeItems.add(itm);
            }
            cb.onSuccess(activeItems);
        }).addOnFailureListener(cb::onError);
    }

    public void getPriceAnchorCoins(Callback<Long> cb) {
        battleDoc().get().addOnSuccessListener(d -> {
            Long v = d.getLong("priceAnchorCoins");
            cb.onSuccess(v == null ? 0 : v);
        }).addOnFailureListener(cb::onError);
    }

    private long calculatePrice(Item item, long anchorCoins) {
        double pct = item.pricePctOfPrevBossReward;
        return Math.max(0, Math.round(anchorCoins * pct));
    }

    public void purchaseItem(Item item, Callback<Void> cb) {
        getPriceAnchorCoins(new Callback<Long>() {
            @Override public void onSuccess(@Nullable Long anchor) {
                if (anchor == null) anchor = 0L;
                final long price = calculatePrice(item, anchor);
                if (item.type == Item.Type.WEAPON) {
                    cb.onError(new IllegalStateException("WEAPON_ONLY_FROM_BOSS"));
                    return;
                }
                db.runTransaction(tr -> {
                    DocumentReference uref = userDoc();
                    DocumentSnapshot user = tr.get(uref);
                    Long coins = user.getLong("coins");
                    if (coins == null) coins = 0L;
                    if (coins < price) throw new IllegalStateException("NOT_ENOUGH_COINS");

                    DocumentReference invRef = invCol().document(item.id);
                    DocumentSnapshot existing = tr.get(invRef);

                    if (!existing.exists()) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("itemId", item.id);
                        m.put("quantity", 1);
                        m.put("active", false);
                        m.put("remainingBattles", item.durationBattles);
                        m.put("upgradeLevel", 0);
                        m.put("dropChance", 0.0);
                        tr.set(invRef, m);
                    } else {
                        Long qnty = existing.getLong("quantity");
                        if (qnty == null) qnty = 0L;
                        tr.update(invRef, "quantity", qnty + 1);
                    }

                    tr.update(uref, "coins", coins - price);
                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    public void activateItem(InventoryItem inv, Callback<Void> cb) {
        db.runTransaction(tr -> {
            DocumentReference inventoryReference = invCol().document(inv.itemId);
            DocumentSnapshot inventorySnapshot = tr.get(inventoryReference);
            Long quantity = inventorySnapshot.getLong("quantity");
            if (quantity == null) quantity = 0L;
            if (quantity <= 0) throw new IllegalStateException("NO_QUANTITY");

            Map<String, Object> activeItem = new HashMap<>();
            activeItem.put("itemId", inv.itemId);
            activeItem.put("remainingBattles", inventorySnapshot.getLong("remainingBattles"));
            activeItem.put("activatedAt", FieldValue.serverTimestamp());
            tr.set(activeCol().document(), activeItem);

            tr.update(inventoryReference, "quantity", Math.max(0, quantity - 1));
            return null;
        }).addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
    }

    public void updateDurationOfActiveItems(Callback<Void> cb) {
        activeCol().get().addOnSuccessListener(snap -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot docSnapshot : snap.getDocuments()) {
                Long rem = docSnapshot.getLong("remainingBattles");
                int remainedBattles = rem.intValue();
                if (remainedBattles > 0) {
                    int newRemainedBattles = remainedBattles - 1;
                    if (newRemainedBattles <= 0) batch.delete(docSnapshot.getReference());
                    else batch.update(docSnapshot.getReference(), "remainingBattles", newRemainedBattles);
                }
            }
            batch.commit().addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }
    public void onBossWeaponDrop(String itemId, Callback<Void> cb) {
        db.runTransaction(tr -> {
            DocumentReference invRef = invCol().document(itemId);
            DocumentSnapshot doc = tr.get(invRef);
            if (!doc.exists()) {
                Map<String, Object> m = new HashMap<>();
                m.put("itemId", itemId);
                m.put("quantity", 1);
                m.put("active", false);
                m.put("remainingBattles", 0);
                m.put("upgradeLevel", 0);
                m.put("dropChance", 0.0);
                tr.set(invRef, m);
            } else {
                Double ch = doc.getDouble("dropChance");
                double cur = ch == null ? 0.0 : ch;
                tr.update(invRef, "dropChance", cur + 0.0002);
            }
            return null;
        }).addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
    }

    public void upgradeWeapon(String itemId, Callback<Void> cb) {
        getPriceAnchorCoins(new Callback<Long>() {
            @Override public void onSuccess(@Nullable Long anchor) {
                if (anchor == null) anchor = 0L;
                long price = Math.round(anchor * 0.60);
                db.runTransaction(tr -> {
                    DocumentReference uref = userDoc();
                    DocumentSnapshot u = tr.get(uref);
                    Long coins = u.getLong("coins");
                    if (coins == null) coins = 0L;
                    if (coins < price) throw new IllegalStateException("NOT_ENOUGH_COINS");

                    DocumentReference invRef = invCol().document(itemId);
                    DocumentSnapshot doc = tr.get(invRef);
                    if (!doc.exists()) throw new IllegalStateException("WEAPON_NOT_OWNED");
                    Long lvl = doc.getLong("upgradeLevel");
                    int currentLevel = lvl == null ? 0 : lvl.intValue();
                    Double dc = doc.getDouble("dropChance");
                    double currDropChance = dc == null ? 0.0 : dc;
                    tr.update(invRef, new java.util.HashMap<String, Object>() {{
                        put("upgradeLevel", currentLevel + 1);
                        put("dropChance", currDropChance + 0.0001);
                    }});
                    tr.update(uref, "coins", coins - price);
                    return null;
                }).addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    public static class CombatBonuses {
        public double ppMultiplier = 1.0;
        public int successAddPct = 0;
        public double extraAttackChance = 0.0;
        public double coinsMultiplier = 1.0;
    }

    public void getCombatBonuses(Callback<CombatBonuses> cb) {
        final CombatBonuses bonuses = new CombatBonuses();
        new CatalogRepository().getAll(new CatalogRepository.Callback<List<Item>>() {
            @Override public void onSuccess(@Nullable List<Item> data) {
                Map<String, Item> catalog = new HashMap<>();
                if (data != null) {
                    for (Item item : data) {
                        if (item != null && item.id != null) catalog.put(item.id, item);
                    }
                }
                activeCol().get().addOnSuccessListener(aSnap -> {
                    for (DocumentSnapshot a : aSnap.getDocuments()) {
                        String iid = a.getString("itemId");
                        Double valuePct = a.getDouble("valuePct");
                        Item catalogItem = catalog.get(iid);
                            if (catalogItem == null || catalogItem.type == Item.Type.WEAPON)
                                continue;
                            double val = valuePct == null ? catalogItem.valuePct : valuePct;
                        if (catalogItem != null) {
                            switch (catalogItem.effect) {
                                case INCREASE_PP:
                                    bonuses.ppMultiplier += val;
                                    break;
                                case INCREASE_SUCCESS:
                                    bonuses.successAddPct += (int) Math.round(val * 100);
                                    break;
                                case EXTRA_ATTACK:
                                    bonuses.extraAttackChance += val;
                                    break;
                                case EXTRA_COINS:
                                    bonuses.coinsMultiplier += val;
                                    break;
                            }
                        }
                    }
                    // zbog upgrade oruzija i jer ne stoje u active items
                    invCol().get().addOnSuccessListener(iSnap -> {
                        for (DocumentSnapshot d : iSnap.getDocuments()) {
                            String iid = d.getString("itemId");
                            Item catalogItem = catalog.get(iid);
                            if (catalogItem != null && catalogItem.type == Item.Type.WEAPON) {
                                Long lvl = d.getLong("upgradeLevel");
                                int upgradeLevel = (lvl == null) ? 0 : lvl.intValue();
                                double bonus = catalogItem.valuePct + (upgradeLevel * WEAPON_UPGRADE_STEP);
                                if (catalogItem.effect == Item.Effect.INCREASE_PP) bonuses.ppMultiplier += bonus;
                                if (catalogItem.effect == Item.Effect.EXTRA_COINS) bonuses.coinsMultiplier += bonus;
                            }
                        }
                        cb.onSuccess(bonuses);
                    }).addOnFailureListener(cb::onError);
                }).addOnFailureListener(cb::onError);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }
}

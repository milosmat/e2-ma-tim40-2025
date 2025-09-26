package com.example.mobilneaplikacije.data.manager;

import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;
import com.example.mobilneaplikacije.data.model.dto.AttackResult;
import com.example.mobilneaplikacije.data.model.dto.VictoryResult;
import com.example.mobilneaplikacije.data.model.dto.BattleState;

public class BattleManager {

    public interface Callback<T> {
        void onSuccess(@Nullable T data);
        void onError(Exception e);
    }

    private final FirebaseFirestore db;
    private final String uid;

    public BattleManager() {
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private DocumentReference stateDoc() {
        return db.collection("users").document(uid).collection("battle").document("current");
    }

    public static long bossHpForIndex(int idx) {
        double hp = 200.0;
        for (int i=2;i<=idx;i++) hp *= 2.5;
        return Math.round(hp);
    }

    public static long coinsForIndex(int idx) {
        return Math.round(200.0 * Math.pow(1.2, idx - 1));
    }

    public void loadOrInit(long stageStartMillis, Callback<BattleState> cb) {
        stateDoc().get().addOnSuccessListener(d -> {
            if (!d.exists()) {
                BattleState s = new BattleState();
                s.currentBossIndex = 1;
                s.bossMaxHp = bossHpForIndex(1);
                s.bossHp = s.bossMaxHp;
                s.attemptsLeft = 5;
                s.halvedReward = false;
                s.carryOverPending = false;
                s.lastStageStart = (stageStartMillis == 0L) ? System.currentTimeMillis() : stageStartMillis;
                long priceAnchorCoins = coinsForIndex(Math.max(1, s.currentBossIndex - 1));
                save(s, new Callback<Void>() {
                    @Override public void onSuccess(@Nullable Void v) {
                        Map<String, Object> up = new HashMap<>();
                        up.put("priceAnchorCoins", priceAnchorCoins);
                        stateDoc().set(up, SetOptions.merge());
                        cb.onSuccess(s);
                    }
                    @Override public void onError(Exception e) { cb.onError(e); }
                });
            } else {
                BattleState s = fromDoc(d);
                if (stageStartMillis > s.lastStageStart) {
                    s.lastStageStart = stageStartMillis;

                    if (s.bossHp > 0 && s.attemptsLeft == 0) s.carryOverPending = true;
                    save(s, new Callback<Void>() {
                        @Override public void onSuccess(@Nullable Void v) {
                            long priceAnchorCoins = coinsForIndex(Math.max(1, s.currentBossIndex - 1));
                            Map<String, Object> up = new HashMap<>();
                            up.put("priceAnchorCoins", priceAnchorCoins);
                            stateDoc().set(up, SetOptions.merge());
                            cb.onSuccess(s);
                        }
                        @Override public void onError(Exception e) { cb.onError(e); }
                    });
                } else cb.onSuccess(s);
            }
        }).addOnFailureListener(cb::onError);
    }
    public ListenerRegistration addStateListener(Callback<BattleState> cb) {
        return stateDoc().addSnapshotListener((snap, e) -> {
            if (e != null) { cb.onError(e); return; }
            if (snap == null || !snap.exists()) {
                cb.onError(new IllegalStateException("NO_BATTLE_STATE"));
                return;
            }
            cb.onSuccess(fromDoc(snap));
        });
    }
    public void prepareBossAfterLevelUp(int frozenHitChance, long stageStartMillis, Callback<Void> cb) {
        stateDoc().get().addOnSuccessListener(d -> {
            BattleState s = d.exists() ? fromDoc(d) : new BattleState();
            if (!d.exists()) {
                s.currentBossIndex = 1;
                s.bossMaxHp = bossHpForIndex(1);
                s.bossHp = s.bossMaxHp;
            }
            s.lastStageStart = stageStartMillis;
            s.attemptsLeft = 5;
            s.hitChance = Math.max(0, Math.min(100, frozenHitChance));

            if (s.bossHp > 0 && d.exists()) {
                s.carryOverPending = true;
            } else {
                s.carryOverPending = false;
                s.halvedReward = false;
            }

            long priceAnchorCoins = coinsForIndex(Math.max(1, s.currentBossIndex - 1));

            save(s, new Callback<Void>() {
                @Override public void onSuccess(@Nullable Void v) {
                    Map<String, Object> up = new HashMap<>();
                    up.put("priceAnchorCoins", priceAnchorCoins);
                    stateDoc().set(up, SetOptions.merge());
                    cb.onSuccess(null);
                }
                @Override public void onError(Exception e) { cb.onError(e); }
            });
        }).addOnFailureListener(cb::onError);
    }

    public void performAttack(int successPercent, long playerPP, Callback<AttackResult> cb) {
        final int p  = Math.max(0, Math.min(100, successPercent));
        final long pp = Math.max(0L, playerPP);

        db.runTransaction(tr -> {
                    DocumentSnapshot sdoc = tr.get(stateDoc());
                    if (!sdoc.exists()) throw new IllegalStateException("NO_BATTLE_STATE");
                    BattleState s = fromDoc(sdoc);

                    AttackResult ar = new AttackResult();
                    if (s.attemptsLeft <= 0) {
                        ar.fightEnded = true;
                        ar.bossHpAfter = s.bossHp;
                        ar.attemptsLeftAfter = 0;
                        return ar;
                    }

                    boolean hit = (new java.util.Random().nextInt(100) < p);
                    ar.hit = hit;

                    if (hit) {
                        long newHp = s.bossHp - pp;
                        ar.damageApplied = Math.min(pp, s.bossHp);
                        s.bossHp = Math.max(0, newHp);
                    } else {
                        ar.damageApplied = 0;
                    }

                    s.attemptsLeft -= 1;

                    ar.bossHpAfter = s.bossHp;
                    ar.attemptsLeftAfter = s.attemptsLeft;
                    ar.bossDefeated = (s.bossHp <= 0);
                    ar.fightEnded = ar.bossDefeated || s.attemptsLeft == 0;

                    if (ar.fightEnded && !ar.bossDefeated) {
                        if (s.bossHp <= s.bossMaxHp / 2) s.halvedReward = true;
                        s.carryOverPending = true;
                    }

                    Map<String, Object> up = new HashMap<>();
                    up.put("bossHp", s.bossHp);
                    up.put("attemptsLeft", s.attemptsLeft);
                    up.put("halvedReward", s.halvedReward);
                    up.put("carryOverPending", s.carryOverPending);
                    up.put("updatedAt", FieldValue.serverTimestamp());
                    tr.set(stateDoc(), up, SetOptions.merge());

                    return ar;
                }).addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }



    public void resolveVictoryAndAdvance(Callback<VictoryResult> cb) {
        final DocumentReference userDoc = db.collection("users").document(uid);
        final CollectionReference eqCol = db.collection("users").document(uid).collection("equipment");

        db.runTransaction(tr -> {
                    DocumentSnapshot sd = tr.get(stateDoc());
                    if (!sd.exists()) throw new IllegalStateException("NO_BATTLE_STATE");
                    BattleState s = fromDoc(sd);
                    if (s.bossHp > 0) throw new IllegalStateException("BOSS_NOT_DEFEATED");

                    long priceAnchorCoins = coinsForIndex(Math.max(1, s.currentBossIndex - 1));

                    long base = coinsForIndex(s.currentBossIndex);
                    long reward = s.halvedReward ? Math.round(base / 2.0) : base;

                    DocumentSnapshot ud = tr.get(userDoc);
                    Long coins = ud.getLong("coins");
                    if (coins == null) coins = 0L;
                    tr.update(userDoc, "coins", coins + reward);

                    double dropChance = 0.20;
                    if (s.halvedReward) dropChance *= 0.5;

                    boolean drop = new java.util.Random().nextDouble() < dropChance;

                    VictoryResult out = new VictoryResult();
                    out.coinsAwarded = reward;

                    if (drop) {
                        boolean weapon = new java.util.Random().nextInt(100) < 5;
                        String name, type;
                        double bonus;
                        int charges;

                        if (weapon) {
                            name = "Mač (+5% PP)";
                            type = "WEAPON";
                            bonus = 0.05;
                            charges = -1;
                        } else {
                            name = "Rukavice (+10% PP)";
                            type = "CLOTHES";
                            bonus = 0.10;
                            charges = 2;
                        }

                        DocumentReference newEq = eqCol.document();
                        Map<String, Object> m = new HashMap<>();
                        m.put("name", name);
                        m.put("type", type);
                        m.put("effect", "INCREASE_PP");
                        m.put("ppBonus", bonus);
                        m.put("charges", charges);
                        m.put("isActive", false);
                        m.put("createdAt", FieldValue.serverTimestamp());
                        tr.set(newEq, m);

                        out.equipmentDropped = true;
                        out.equipmentDocId = newEq.getId();
                        out.equipmentName = name;
                        out.equipmentType = type;
                        out.ppBonus = bonus;
                        out.charges = charges;
                    }

                    int nextIdx = s.currentBossIndex + 1;
                    long nextHp = bossHpForIndex(nextIdx);

                    Map<String, Object> nm = new HashMap<>();
                    nm.put("currentBossIndex", nextIdx);
                    nm.put("bossMaxHp", nextHp);
                    nm.put("bossHp", nextHp);
                    nm.put("attemptsLeft", 5);
                    nm.put("halvedReward", false);
                    nm.put("carryOverPending", false);
                    nm.put("lastStageStart", s.lastStageStart);
                    nm.put("updatedAt", FieldValue.serverTimestamp());
                    nm.put("priceAnchorCoins", priceAnchorCoins);
                    tr.set(stateDoc(), nm, SetOptions.merge());

                    out.nextBossIndex = nextIdx;
                    out.nextBossMaxHp = nextHp;
                    return out;
                }).addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    public void save(BattleState s, Callback<Void> cb) {
        Map<String, Object> m = new HashMap<>();
        m.put("currentBossIndex", s.currentBossIndex);
        m.put("bossMaxHp", s.bossMaxHp);
        m.put("bossHp", s.bossHp);
        m.put("attemptsLeft", s.attemptsLeft);
        m.put("halvedReward", s.halvedReward);
        m.put("carryOverPending", s.carryOverPending);
        m.put("lastStageStart", s.lastStageStart);
        m.put("updatedAt", FieldValue.serverTimestamp());
        m.put("hitChance", s.hitChance);
        stateDoc().set(m, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    private static BattleState fromDoc(DocumentSnapshot d) {
        BattleState s = new BattleState();
        Long idx = d.getLong("currentBossIndex");
        s.currentBossIndex = idx==null?1:idx.intValue();
        Long max = d.getLong("bossMaxHp");
        s.bossMaxHp = max==null? bossHpForIndex(s.currentBossIndex) : max;
        Long hp = d.getLong("bossHp");
        s.bossHp = hp==null? s.bossMaxHp : hp;
        Long att = d.getLong("attemptsLeft");
        s.attemptsLeft = att==null?5:att.intValue();
        Boolean half = d.getBoolean("halvedReward");
        s.halvedReward = Boolean.TRUE.equals(half);
        Boolean carry = d.getBoolean("carryOverPending");
        s.carryOverPending = Boolean.TRUE.equals(carry);
        Long ls = d.getLong("lastStageStart");
        s.lastStageStart = ls==null?0:ls;
        Integer hc = d.getLong("hitChance") == null ? null : d.getLong("hitChance").intValue();
        s.hitChance = hc == null ? 0 : hc;
        return s;
    }
}

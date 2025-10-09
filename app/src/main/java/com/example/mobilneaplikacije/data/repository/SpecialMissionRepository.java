package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.manager.BattleManager;
import com.example.mobilneaplikacije.data.model.Item;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.*;

public class SpecialMissionRepository {

    public interface Callback<T> { void onSuccess(@Nullable T data); void onError(Exception e); }

    private final FirebaseFirestore db;
    private final String uid;

    public SpecialMissionRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("User not logged in");
        this.uid = u.getUid();
    }

    private DocumentReference allianceDoc(String allianceId) {
        return db.collection("alliances").document(allianceId);
    }
    private DocumentReference missionDoc(String allianceId) {
        return allianceDoc(allianceId).collection("specialMissions").document("current");
    }
    private CollectionReference progressCol(String allianceId) {
        return missionDoc(allianceId).collection("progress");
    }

    public static class MissionState {
        public boolean active;
        public long startedAt;
        public long endsAt;
        public long bossMaxHp;
        public long bossHp;
        public boolean rewardsGiven;
        public boolean rewardsComputed;
    }
    public static class UserProgress {
        public String uid;
        public int purchases;
        public int hits;
        public int groupA;
        public int groupB;
        public Map<String, Boolean> chatDays;
        public long dealtHp;
        public boolean noUnresolvedAwarded;
    }

    public void getCurrent(String allianceId, Callback<MissionState> cb) {
        missionDoc(allianceId).get().addOnSuccessListener(d -> {
            if (!d.exists()) { cb.onSuccess(null); return; }
            MissionState s = new MissionState();
            Boolean active = d.getBoolean("active");
            s.active = Boolean.TRUE.equals(active);
            Long st = d.getLong("startedAt"); s.startedAt = st == null ? 0 : st;
            Long en = d.getLong("endsAt"); s.endsAt = en == null ? 0 : en;
            Long mx = d.getLong("bossMaxHp"); s.bossMaxHp = mx == null ? 0 : mx;
            Long hp = d.getLong("bossHp"); s.bossHp = hp == null ? 0 : hp;
            Boolean r = d.getBoolean("rewardsGiven"); s.rewardsGiven = Boolean.TRUE.equals(r);
            Boolean rc = d.getBoolean("rewardsComputed"); s.rewardsComputed = Boolean.TRUE.equals(rc);
            long now = System.currentTimeMillis();
            if (s.active && s.endsAt > 0 && now > s.endsAt) {
                Map<String, Object> up = new HashMap<>();
                up.put("active", false);
                missionDoc(allianceId).set(up, SetOptions.merge());
                db.collection("alliances").document(allianceId).update("isSpecialMissionActive", false);
                s.active = false;
                markRewardsComputedIfEligible(allianceId);
            }
            cb.onSuccess(s);
        }).addOnFailureListener(cb::onError);
    }

    public void listProgress(String allianceId, Callback<List<UserProgress>> cb) {
        progressCol(allianceId).get().addOnSuccessListener(qs -> {
            List<UserProgress> out = new ArrayList<>();
            for (DocumentSnapshot d : qs.getDocuments()) out.add(fromProgressDoc(d));
            cb.onSuccess(out);
        }).addOnFailureListener(cb::onError);
    }

    private static UserProgress fromProgressDoc(DocumentSnapshot d) {
        UserProgress up = new UserProgress();
        up.uid = d.getId();
        Long p = d.getLong("purchases"); up.purchases = p == null ? 0 : p.intValue();
        Long h = d.getLong("hits"); up.hits = h == null ? 0 : h.intValue();
        Long a = d.getLong("groupA"); up.groupA = a == null ? 0 : a.intValue();
        Long b = d.getLong("groupB"); up.groupB = b == null ? 0 : b.intValue();
        Object cd = d.get("chatDays");
        if (cd instanceof Map) up.chatDays = (Map<String, Boolean>) cd; else up.chatDays = new HashMap<>();
        Long dealt = d.getLong("dealtHp"); up.dealtHp = dealt == null ? 0 : dealt;
        Boolean bonus = d.getBoolean("noUnresolvedAwarded"); up.noUnresolvedAwarded = Boolean.TRUE.equals(bonus);
        return up;
    }

    public void startMission(String allianceId, Callback<Void> cb) {
        allianceDoc(allianceId).get().addOnSuccessListener(snap -> {
            if (!snap.exists()) { cb.onError(new IllegalStateException("ALLIANCE_NOT_FOUND")); return; }
            String leaderUid = snap.getString("leaderUid");
            if (!uid.equals(leaderUid)) { cb.onError(new IllegalStateException("NOT_LEADER")); return; }
            Boolean active = snap.getBoolean("isSpecialMissionActive");
            if (Boolean.TRUE.equals(active)) { cb.onError(new IllegalStateException("MISSION_ALREADY_ACTIVE")); return; }

            db.collection("users").whereEqualTo("allianceId", allianceId).get().addOnSuccessListener(users -> {
                int members = users.size();
                long hp = Math.max(0, 100L * Math.max(1, members));
                long now = System.currentTimeMillis();
                long ends = now + 14L * 24 * 60 * 60 * 1000;

                Map<String, Object> mission = new HashMap<>();
                mission.put("active", true);
                mission.put("startedAt", now);
                mission.put("endsAt", ends);
                mission.put("bossMaxHp", hp);
                mission.put("bossHp", hp);
                mission.put("membersCountAtStart", members);
                mission.put("rewardsGiven", false);
                mission.put("rewardsComputed", false);
                mission.put("createdAt", FieldValue.serverTimestamp());

                com.google.firebase.firestore.WriteBatch batch = db.batch();
                batch.set(missionDoc(allianceId), mission, SetOptions.merge());
                for (DocumentSnapshot u : users.getDocuments()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("purchases", 0);
                    p.put("hits", 0);
                    p.put("groupA", 0);
                    p.put("groupB", 0);
                    p.put("chatDays", new HashMap<String, Boolean>());
                    p.put("dealtHp", 0);
                    p.put("updatedAt", FieldValue.serverTimestamp());
                    batch.set(progressCol(allianceId).document(u.getId()), p, SetOptions.merge());
                }
                batch.update(allianceDoc(allianceId), "isSpecialMissionActive", true);
                batch.commit().addOnSuccessListener(v -> cb.onSuccess(null)).addOnFailureListener(cb::onError);
            }).addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void recordShopPurchaseForMyAlliance(Callback<Void> cb) {
        new AllianceRepository().getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(@Nullable AllianceRepository.AllianceInfo data) {
                if (data == null || data.allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
                recordShopPurchase(data.allianceId, cb);
            }
            @Override public void onError(Exception e) { if (cb!=null) cb.onError(e); }
        });
    }

    public void recordShopPurchase(String allianceId, @Nullable Callback<Void> cb) {
        if (allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
        applyCappedDelta(allianceId, uid, "purchases", 5, 2, 1, cb);
    }

    public void recordRegularBossHit(String allianceId, @Nullable Callback<Void> cb) {
        if (allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
        applyCappedDelta(allianceId, uid, "hits", 10, 2, 1, cb);
    }

    public void recordTaskCompletionAuto(String difficulty, String importance, @Nullable Callback<Void> cb) {
        new AllianceRepository().getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(@Nullable AllianceRepository.AllianceInfo data) {
                if (data == null || data.allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
                recordTaskCompletion(data.allianceId, difficulty, importance, cb);
            }
            @Override public void onError(Exception e) { if (cb!=null) cb.onError(e); }
        });
    }

    public void recordTaskCompletion(String allianceId, String difficulty, String importance, @Nullable Callback<Void> cb) {
        if (allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
        final boolean isGroupA = isGroupATask(difficulty, importance);
        final int unitIncrements;
        final int hpPerIncrement;
        final String field;
        final int cap;

        if (isGroupA) {
            unitIncrements = ("LAK".equals(difficulty) && "NORMALAN".equals(importance)) ? 2 : 1;
            hpPerIncrement = 1;
            field = "groupA";
            cap = 10;
        } else {
            unitIncrements = 1;
            hpPerIncrement = 4;
            field = "groupB";
            cap = 6;
        }

        applyCappedDelta(allianceId, uid, field, cap, hpPerIncrement, unitIncrements, cb);
    }

    private boolean isGroupATask(String difficulty, String importance) {
        return "VEOMA_LAK".equals(difficulty) || "LAK".equals(difficulty)
                || "NORMALAN".equals(importance) || "VAZAN".equals(importance);
    }

    public void recordAllianceMessageDay(String allianceId, @Nullable Callback<Void> cb) {
        if (allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
        final String dayKey = dayKey(System.currentTimeMillis());
        db.runTransaction(tr -> {
            DocumentSnapshot md = tr.get(missionDoc(allianceId));
            if (!md.exists()) return null;
            Boolean active = md.getBoolean("active");
            Long endsAt = md.getLong("endsAt");
            if (!Boolean.TRUE.equals(active) || (endsAt != null && System.currentTimeMillis() > endsAt)) return null;

            DocumentReference pRef = progressCol(allianceId).document(uid);
            DocumentSnapshot pd = tr.get(pRef);
            Map<String, Object> chatDays = new HashMap<>();
            if (pd.exists()) {
                Object cd = pd.get("chatDays");
                if (cd instanceof Map) chatDays.putAll((Map<String, Object>) cd);
            }
            if (Boolean.TRUE.equals(chatDays.get(dayKey))) return null;

            chatDays.put(dayKey, true);
            tr.set(pRef, new HashMap<String, Object>() {{
                put("chatDays", chatDays);
                put("updatedAt", FieldValue.serverTimestamp());
            }}, SetOptions.merge());

            Long curHp = md.getLong("bossHp");
            long newHp = Math.max(0, (curHp == null ? 0 : curHp) - 4);
            tr.update(missionDoc(allianceId), "bossHp", newHp, "updatedAt", FieldValue.serverTimestamp());
            tr.set(pRef, new HashMap<String, Object>() {{
                Long dealt = pd.getLong("dealtHp");
                put("dealtHp", (dealt == null ? 0 : dealt) + 4);
            }}, SetOptions.merge());
            return null;
        }).addOnSuccessListener(v -> { if (cb!=null) cb.onSuccess(null); })
          .addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
    }

    public void finalizeIfDue(String allianceId, @Nullable Callback<Void> cb) {
        if (allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
        missionDoc(allianceId).get().addOnSuccessListener(md -> {
            if (!md.exists()) { if (cb!=null) cb.onSuccess(null); return; }
            Boolean active = md.getBoolean("active");
            Boolean rewardsGiven = md.getBoolean("rewardsGiven");
            Boolean rewardsComputed = md.getBoolean("rewardsComputed");
            Long endsAt = md.getLong("endsAt");
            Long bossHpL = md.getLong("bossHp");
            long now = System.currentTimeMillis();
            long bossHp = bossHpL == null ? 0 : bossHpL;
            boolean timeUp = endsAt != null && now >= endsAt;
            boolean bossDead = bossHp <= 0;
            if (Boolean.TRUE.equals(rewardsGiven) || Boolean.TRUE.equals(rewardsComputed)) { if (cb!=null) cb.onSuccess(null); return; }
            if (!Boolean.TRUE.equals(active) && !timeUp && !bossDead) { if (cb!=null) cb.onSuccess(null); return; }

            if (Boolean.TRUE.equals(active) && (timeUp || bossDead)) {
                Map<String,Object> up = new HashMap<>();
                up.put("active", false);
                up.put("updatedAt", FieldValue.serverTimestamp());
                missionDoc(allianceId).set(up, SetOptions.merge());
                allianceDoc(allianceId).update("isSpecialMissionActive", false);
            }

            // Fetch members and attempt bonus for each
            db.collection("users").whereEqualTo("allianceId", allianceId).get().addOnSuccessListener(users -> {
                if (users.isEmpty()) {
                    // No members -> just attempt rewards if boss dead
                    missionDoc(allianceId).get().addOnSuccessListener(md2 -> {
                        Long hp2 = md2.getLong("bossHp");
                        if (hp2 != null && hp2 <= 0) {
                            distributeRewardsIfNeeded(allianceId, new Callback<Void>() { @Override public void onSuccess(@Nullable Void data) { if (cb!=null) cb.onSuccess(null); } @Override public void onError(Exception e) { if (cb!=null) cb.onError(e); } });
                        } else { if (cb!=null) cb.onSuccess(null); }
                    }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
                    return;
                }
                final int total = users.size();
                if (total == 0) { if (cb!=null) cb.onSuccess(null); return; }
                final int[] done = {0};
                final int[] failed = {0};
                for (DocumentSnapshot u : users.getDocuments()) {
                    String memberUid = u.getId();
                    tryApplyNoUnresolvedBonus(allianceId, memberUid, new Callback<Void>() {
                        @Override public void onSuccess(@Nullable Void data) {
                            if (++done[0] + failed[0] == total) {
                                // Re-read mission for final HP and maybe distribute rewards
                                missionDoc(allianceId).get().addOnSuccessListener(md3 -> {
                                    Long hp3 = md3.getLong("bossHp");
                                    if (hp3 != null && hp3 <= 0) {
                                        markRewardsComputedIfEligible(allianceId);
                                    }
                                    if (cb!=null) cb.onSuccess(null);
                                }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
                            }
                        }
                        @Override public void onError(Exception e) {
                            failed[0]++;
                            if (done[0] + failed[0] == total) {
                                missionDoc(allianceId).get().addOnSuccessListener(md3 -> {
                                    Long hp3 = md3.getLong("bossHp");
                                    if (hp3 != null && hp3 <= 0) {
                                        markRewardsComputedIfEligible(allianceId);
                                    }
                                    if (cb!=null) cb.onSuccess(null);
                                }).addOnFailureListener(ex -> { if (cb!=null) cb.onError(ex); });
                            }
                        }
                    });
                }
            }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
        }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
    }

    private void applyCappedDelta(String allianceId, String forUid, String field, int cap, int hpPerIncrement, int incrementsRequested, @Nullable Callback<Void> cb) {
        db.runTransaction(tr -> {
            DocumentSnapshot md = tr.get(missionDoc(allianceId));
            if (!md.exists()) return null;
            Boolean active = md.getBoolean("active");
            Long endsAt = md.getLong("endsAt");
            if (!Boolean.TRUE.equals(active) || (endsAt != null && System.currentTimeMillis() > endsAt)) return null;

            DocumentReference pRef = progressCol(allianceId).document(forUid);
            DocumentSnapshot pd = tr.get(pRef);
            Long curL = pd.getLong(field);
            int cur = curL == null ? 0 : curL.intValue();
            if (cur >= cap) return null;

            int canApply = Math.min(incrementsRequested, Math.max(0, cap - cur));
            if (canApply <= 0) return null;
            int hpDelta = hpPerIncrement * canApply;

            Long curHp = md.getLong("bossHp");
            long newHp = Math.max(0, (curHp == null ? 0 : curHp) - hpDelta);

            Map<String, Object> pUp = new HashMap<>();
            pUp.put(field, cur + canApply);
            pUp.put("updatedAt", FieldValue.serverTimestamp());
            Long dealt = pd.getLong("dealtHp");
            pUp.put("dealtHp", (dealt == null ? 0 : dealt) + hpDelta);
            tr.set(pRef, pUp, SetOptions.merge());

            Map<String, Object> missionUpd = new HashMap<>();
            missionUpd.put("bossHp", newHp);
            missionUpd.put("updatedAt", FieldValue.serverTimestamp());
            if (newHp == 0) missionUpd.put("active", false);
            tr.update(missionDoc(allianceId), missionUpd);

            if (newHp == 0) {
                tr.update(allianceDoc(allianceId), "isSpecialMissionActive", false);
            }

            return null;
                }).addOnSuccessListener(v -> {
                    // If boss died, just mark rewards computed (users will claim themselves)
                    try { markRewardsComputedIfEligible(allianceId); } catch (Exception ignored) {}
                    if (cb!=null) cb.onSuccess(null);
                })
          .addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
    }

    private static String dayKey(long now) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(now);
        int y=c.get(Calendar.YEAR), m=c.get(Calendar.MONTH)+1, d=c.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.US, "%04d%02d%02d", y, m, d);
    }

    // Legacy method now converted to simply marking rewards computed (kept to avoid crashes where still referenced)
    public void distributeRewardsIfNeeded(String allianceId, Callback<Void> cb) {
        markRewardsComputedIfEligible(allianceId);
        cb.onSuccess(null);
    }

    private void markRewardsComputedIfEligible(String allianceId) {
        missionDoc(allianceId).get().addOnSuccessListener(md -> {
            if (!md.exists()) return;
            Boolean active = md.getBoolean("active");
            Boolean rc = md.getBoolean("rewardsComputed");
            Long bossHp = md.getLong("bossHp");
            if (Boolean.TRUE.equals(rc)) return;
            if (Boolean.TRUE.equals(active)) return; // still active
            if (bossHp != null && bossHp > 0) return; // boss not dead
            missionDoc(allianceId).set(new HashMap<String,Object>(){{
                put("rewardsComputed", true);
                // Keep legacy flag for UI backward compatibility
                put("rewardsGiven", true);
                put("updatedAt", FieldValue.serverTimestamp());
            }}, SetOptions.merge());
        });
    }

    /**
     * User-initiated reward claim. Gives coins, increments badge, adds one potion & one clothes item if available.
     * Idempotent: if already claimed (progress.rewardClaimed==true) it becomes a no-op.
     */
    public void claimReward(String allianceId, Callback<Void> cb) {
        if (allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
        // Preload catalog (optional) to pick first potion & clothes ids
        new CatalogRepository().getAll(new CatalogRepository.Callback<List<Item>>() {
            @Override public void onSuccess(@Nullable List<Item> catalog) {
                final String[] potionId = {null};
                final String[] clothesId = {null};
                if (catalog != null) {
                    for (Item it : catalog) {
                        if (it == null || it.id == null) continue;
                        if (potionId[0] == null && it.type == Item.Type.POTION) potionId[0] = it.id;
                        if (clothesId[0] == null && it.type == Item.Type.CLOTHES) clothesId[0] = it.id;
                    }
                }
                android.util.Log.d("SpecMission", "claimReward start alliance=" + allianceId + " potionId=" + potionId[0] + " clothesId=" + clothesId[0]);
                db.runTransaction(tr -> {
                    // ===== READ PHASE (all reads before writes) =====
                    DocumentReference missionRef = missionDoc(allianceId);
                    DocumentSnapshot md = tr.get(missionRef);
                    if (!md.exists()) { android.util.Log.w("SpecMission","claimReward: mission missing"); return null; }
                    Boolean active = md.getBoolean("active");
                    Boolean rc = md.getBoolean("rewardsComputed");
                    Long bossHp = md.getLong("bossHp");
                    if (Boolean.TRUE.equals(active)) { android.util.Log.d("SpecMission","claimReward: mission still active"); return null; }
                    if (!Boolean.TRUE.equals(rc) && (bossHp != null && bossHp > 0)) { android.util.Log.d("SpecMission","claimReward: rewards not ready"); return null; }

                    DocumentReference pRef = progressCol(allianceId).document(uid);
                    DocumentSnapshot pd = tr.get(pRef);
                    Boolean already = pd.getBoolean("rewardClaimed");
                    if (Boolean.TRUE.equals(already)) { android.util.Log.d("SpecMission","claimReward: already claimed"); return null; }

                    DocumentReference userRef = db.collection("users").document(uid);
                    DocumentReference battleRef = userRef.collection("battle").document("current");
                    DocumentSnapshot battle = tr.get(battleRef);
                    DocumentSnapshot userSnap = tr.get(userRef);

                    DocumentSnapshot invPotionSnap = null;
                    DocumentSnapshot invClothesSnap = null;
                    DocumentReference invPotionRef = null;
                    DocumentReference invClothesRef = null;
                    if (potionId[0] != null) {
                        invPotionRef = userRef.collection("inventory").document(potionId[0]);
                        invPotionSnap = tr.get(invPotionRef);
                    }
                    if (clothesId[0] != null) {
                        invClothesRef = userRef.collection("inventory").document(clothesId[0]);
                        invClothesSnap = tr.get(invClothesRef);
                    }

                    // ===== COMPUTE PHASE =====
                    int curIdx = 1;
                    if (battle.exists()) {
                        Long ci = battle.getLong("currentBossIndex");
                        if (ci != null && ci > 0) curIdx = ci.intValue();
                    }
                    long nextReward = BattleManager.coinsForIndex(curIdx + 1);
                    long coinsAward = Math.round(nextReward * 0.5);
                    Long coins = userSnap.getLong("coins"); if (coins == null) coins = 0L;
                    Long smw = userSnap.getLong("specialMissionsWon");
                    int newCount = (smw == null ? 0 : smw.intValue()) + 1;

                    // ===== WRITE PHASE =====
                    Long finalCoins = coins;
                    tr.update(userRef, new HashMap<String,Object>() {{
                        put("coins", finalCoins + coinsAward);
                        put("specialMissionsWon", newCount);
                    }});

                    if (invPotionRef != null) {
                        if (invPotionSnap == null || !invPotionSnap.exists()) {
                            Map<String,Object> m = new HashMap<>();
                            m.put("itemId", potionId[0]);
                            m.put("quantity", 1);
                            m.put("active", false);
                            m.put("remainingBattles", 0);
                            m.put("upgradeLevel", 0);
                            m.put("dropChance", 0.0);
                            tr.set(invPotionRef, m);
                        } else {
                            Long q = invPotionSnap.getLong("quantity");
                            tr.update(invPotionRef, "quantity", (q == null ? 0 : q) + 1);
                        }
                    }
                    if (invClothesRef != null) {
                        if (invClothesSnap == null || !invClothesSnap.exists()) {
                            Map<String,Object> m = new HashMap<>();
                            m.put("itemId", clothesId[0]);
                            m.put("quantity", 1);
                            m.put("active", false);
                            m.put("remainingBattles", 0);
                            m.put("upgradeLevel", 0);
                            m.put("dropChance", 0.0);
                            tr.set(invClothesRef, m);
                        } else {
                            Long q = invClothesSnap.getLong("quantity");
                            tr.update(invClothesRef, "quantity", (q == null ? 0 : q) + 1);
                        }
                    }

                    tr.set(pRef, new HashMap<String,Object>() {{
                        put("rewardClaimed", true);
                        put("claimedAt", FieldValue.serverTimestamp());
                        put("claimedCoins", coinsAward);
                    }}, SetOptions.merge());
                    android.util.Log.d("SpecMission", "Reward claimed (claimReward) coins=" + coinsAward + " curIdx=" + curIdx);
                    return null;
                }).addOnSuccessListener(v -> {
                    PlayerRepository.invalidateCache();
                    if (cb!=null) cb.onSuccess(null);
                }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
            }
            @Override public void onError(Exception e) { if (cb!=null) cb.onError(e); }
        });
    }

    public void tryApplyNoUnresolvedBonusForMyAlliance(@Nullable Callback<Void> cb) {
        new AllianceRepository().getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(@Nullable AllianceRepository.AllianceInfo data) {
                if (data == null || data.allianceId == null) { if (cb!=null) cb.onSuccess(null); return; }
                tryApplyNoUnresolvedBonus(data.allianceId, uid, cb);
            }
            @Override public void onError(Exception e) { if (cb!=null) cb.onError(e); }
        });
    }

    public void tryApplyNoUnresolvedBonus(String allianceId, String targetUid, @Nullable Callback<Void> cb) {
        final DocumentReference mref = missionDoc(allianceId);
        mref.get().addOnSuccessListener(md -> {
            if (!md.exists()) { if (cb!=null) cb.onSuccess(null); return; }
            Boolean active = md.getBoolean("active");
            Long endsAt = md.getLong("endsAt");
            Long startedAt = md.getLong("startedAt");
            Long bossHp = md.getLong("bossHp");
            if (startedAt == null) { if (cb!=null) cb.onSuccess(null); return; }
            long now = System.currentTimeMillis();
            boolean missionEndedByTime = (endsAt != null && now >= endsAt);
            boolean bossDefeated = (bossHp != null && bossHp <= 0) || !Boolean.TRUE.equals(active);
            // Novi uslov: bonus se NE ocenjuje dok misija još traje (aktivna i nije poražen boss i nije istekao rok)
            if (!(missionEndedByTime || bossDefeated)) { if (cb!=null) cb.onSuccess(null); return; }

            final long start = startedAt;
            // Ako je misija završena istekom vremena koristimo planirani endsAt kao kraj prozora, inače trenutni trenutak/boss poraz
            final long end = missionEndedByTime && endsAt != null ? endsAt : now;

            FirebaseFirestore db = this.db;
            final DocumentReference uroot = db.collection("users").document(targetUid);
            uroot.collection("tasks").get().addOnSuccessListener(tasksSnap -> {
                final List<DocumentSnapshot> masters = tasksSnap.getDocuments();

                for (DocumentSnapshot d : masters) {
                    Boolean rec = d.getBoolean("isRecurring");
                    String status = d.getString("status");
                    Long due = d.getLong("dueDateTime");
                    boolean inWindow = (due != null && due >= start && due <= end);
                    if (!Boolean.TRUE.equals(rec)) {
                        if (("ACTIVE".equals(status) || "MISSED".equals(status)) && inWindow) {
                            if (cb!=null) cb.onSuccess(null); return;
                        }
                    }
                }

                scanRecurringOccurrencesForUnresolved(masters, 0, uroot, start, end, new Callback<Boolean>() {
                    @Override public void onSuccess(@Nullable Boolean hasUnresolved) {
                        if (Boolean.TRUE.equals(hasUnresolved)) { if (cb!=null) cb.onSuccess(null); return; }
                        db.runTransaction(tr -> {
                            DocumentSnapshot md2 = tr.get(mref);
                            if (!md2.exists()) return null;
                            Boolean active2 = md2.getBoolean("active");
                            Long endsAt2 = md2.getLong("endsAt");
                            Long bossHp2 = md2.getLong("bossHp");
                            long now2 = System.currentTimeMillis();
                            boolean missionEndedByTime2 = (endsAt2 != null && now2 >= endsAt2);
                            boolean bossDefeated2 = (bossHp2 != null && bossHp2 <= 0) || !Boolean.TRUE.equals(active2);
                            if (!(missionEndedByTime2 || bossDefeated2)) return null; // još traje – ne dodeljuj

                            DocumentReference pref = progressCol(allianceId).document(targetUid);
                            DocumentSnapshot pd = tr.get(pref);
                            Boolean awarded = pd.getBoolean("noUnresolvedAwarded");
                            if (Boolean.TRUE.equals(awarded)) return null;

                            Long curHp = md2.getLong("bossHp");
                            long newHp = Math.max(0, (curHp == null ? 0 : curHp) - 10);

                            Map<String, Object> pup = new HashMap<>();
                            pup.put("noUnresolvedAwarded", true);
                            Long dealt = pd.getLong("dealtHp");
                            pup.put("dealtHp", (dealt == null ? 0 : dealt) + 10);
                            pup.put("updatedAt", FieldValue.serverTimestamp());
                            tr.set(pref, pup, SetOptions.merge());

                            Map<String, Object> mup = new HashMap<>();
                            mup.put("bossHp", newHp);
                            mup.put("updatedAt", FieldValue.serverTimestamp());
                            if (newHp == 0) mup.put("active", false);
                            tr.update(mref, mup);
                            if (newHp == 0) tr.update(allianceDoc(allianceId), "isSpecialMissionActive", false);
                            return null;
                        }).addOnSuccessListener(v -> {
                            try { markRewardsComputedIfEligible(allianceId); } catch (Exception ignored) {}
                            if (cb!=null) cb.onSuccess(null);
                        }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
                    }
                    @Override public void onError(Exception e) { if (cb!=null) cb.onError(e); }
                });
            }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
        }).addOnFailureListener(e -> { if (cb!=null) cb.onError(e); });
    }

    private void scanRecurringOccurrencesForUnresolved(List<DocumentSnapshot> masters, int index,
                                                       DocumentReference uroot, long start, long end,
                                                       Callback<Boolean> cb) {
        for (int i = index; i < masters.size(); i++) {
            DocumentSnapshot d = masters.get(i);
            Boolean rec = d.getBoolean("isRecurring");
            if (!Boolean.TRUE.equals(rec)) continue;
            String taskId = d.getId();
            int finalI = i;
            uroot.collection("tasks").document(taskId).collection("occurrences")
                    .whereGreaterThanOrEqualTo("dueDateTime", start)
                    .whereLessThanOrEqualTo("dueDateTime", end)
                    .get()
                    .addOnSuccessListener(occSnap -> {
                        for (DocumentSnapshot o : occSnap.getDocuments()) {
                            String st = o.getString("status");
                            if ("ACTIVE".equals(st) || "MISSED".equals(st)) { cb.onSuccess(true); return; }
                        }
                        scanRecurringOccurrencesForUnresolved(masters, finalI + 1, uroot, start, end, cb);
                    })
                    .addOnFailureListener(cb::onError);
            return;
        }
        cb.onSuccess(false);
    }
}

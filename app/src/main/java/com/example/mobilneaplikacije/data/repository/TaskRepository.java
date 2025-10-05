package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.model.TaskOccurrence;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class TaskRepository {

    public interface Callback<T> {
        void onSuccess(@Nullable T data);
        void onError(Exception e);
    }

    private final FirebaseFirestore db;
    private final String uid;

    public TaskRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("User not logged in");
        this.uid = u.getUid();
    }

    private CollectionReference tasksCol() {
        return db.collection("users").document(uid).collection("tasks");
    }
    private CollectionReference occCol(String taskId) {
        return tasksCol().document(taskId).collection("occurrences");
    }
    private CollectionReference logsCol() {
        return db.collection("users").document(uid).collection("completionLogs");
    }
    private DocumentReference userDoc() {
        return db.collection("users").document(uid);
    }

    private DocumentReference dayQuotaRef(long now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        int y = c.get(java.util.Calendar.YEAR);
        int m = c.get(java.util.Calendar.MONTH) + 1;
        int d = c.get(java.util.Calendar.DAY_OF_MONTH);
        String key = String.format(java.util.Locale.US, "%04d%02d%02d", y, m, d);
        return db.collection("users").document(uid)
                .collection("quota").document("day_" + key);
    }

    private DocumentReference weekQuotaRef(long now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        int y = c.get(java.util.Calendar.YEAR);
        c.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        int w = c.get(java.util.Calendar.WEEK_OF_YEAR);
        String key = String.format(java.util.Locale.US, "%04d%02d", y, w);
        return db.collection("users").document(uid)
                .collection("quota").document("week_" + key);
    }

    private DocumentReference monthQuotaRef(long now) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(now);
        int y = c.get(java.util.Calendar.YEAR);
        int m = c.get(java.util.Calendar.MONTH) + 1;
        String key = String.format(java.util.Locale.US, "%04d%02d", y, m);
        return db.collection("users").document(uid)
                .collection("quota").document("month_" + key);
    }

    private static int safeInt(Long v) { return v == null ? 0 : v.intValue(); }
    public void insertTask(Task t, Callback<String> cb) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", t.getTitle());
        map.put("description", t.getDescription());
        map.put("categoryId", t.getCategoryIdHash());
        map.put("difficulty", t.getDifficulty());
        map.put("importance", t.getImportance());
        map.put("xpPoints", t.getXpPoints());
        map.put("status", "ACTIVE");
        map.put("isRecurring", t.isRecurring());
        map.put("repeatInterval", t.getRepeatInterval());
        map.put("repeatUnit", t.getRepeatUnit());
        map.put("startDate", t.getStartDate());
        map.put("endDate", t.getEndDate());
        map.put("dueDateTime", t.getDueDateTime());
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("updatedAt", FieldValue.serverTimestamp());

        tasksCol().add(map)
                .addOnSuccessListener(ref -> {
                    if (t.isRecurring()) {
                        generateOccurrences(ref.getId(), t, 90, new Callback<Void>() {
                            @Override public void onSuccess(Void v) { cb.onSuccess(ref.getId()); }
                            @Override public void onError(Exception e) { cb.onError(e); }
                        });
                    } else {
                        cb.onSuccess(ref.getId());
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void getAllTasks(Callback<List<Task>> cb) {
        tasksCol().get()
                .addOnSuccessListener(snap -> {
                    List<Task> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) out.add(fromDoc(d));

                    autoMarkMissedSingles(out);
                    cb.onSuccess(out);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getTaskById(String idHash, Callback<Task> cb) {
        tasksCol().document(idHash).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onSuccess(null); return; }
                    Task t = fromDoc(d);
                    autoMarkMissedSingle(t, new Callback<Void>() {
                        @Override public void onSuccess(Void v) { cb.onSuccess(t); }
                        @Override public void onError(Exception e) { cb.onSuccess(t); }
                    });
                })
                .addOnFailureListener(cb::onError);
    }

    public void updateTask(String taskId, Task updates, Callback<Void> cb) {
        tasksCol().document(taskId).get().addOnSuccessListener(d -> {
            if (!d.exists()) { cb.onError(new IllegalStateException("Task not found")); return; }
            Task cur = fromDoc(d);

            if (!canEditMaster(cur)) {
                cb.onError(new IllegalStateException("Task locked for editing"));
                return;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("title", updates.getTitle());
            m.put("description", updates.getDescription());
            m.put("categoryId", updates.getCategoryIdHash());
            m.put("difficulty", updates.getDifficulty());
            m.put("importance", updates.getImportance());
            m.put("xpPoints", updates.getXpPoints());
            m.put("dueDateTime", updates.getDueDateTime());
            m.put("repeatInterval", updates.getRepeatInterval());
            m.put("repeatUnit", updates.getRepeatUnit());
            m.put("startDate", updates.getStartDate());
            m.put("endDate", updates.getEndDate());
            m.put("updatedAt", FieldValue.serverTimestamp());

            tasksCol().document(taskId).set(m, SetOptions.merge())
                    .addOnSuccessListener(v -> {
                        if (cur.isRecurring()) {
                            long now = System.currentTimeMillis();
                            deleteOccurrencesFrom(taskId, now, new Callback<Void>() {
                                @Override public void onSuccess(Void unused) {
                                    generateOccurrences(taskId, updates, 90, cb);
                                }
                                @Override public void onError(Exception e) { cb.onError(e); }
                            });
                        } else {
                            cb.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(cb::onError);
        }).addOnFailureListener(cb::onError);
    }

    public void updateTaskStatus(String taskId, String status, Callback<Void> cb) {
        tasksCol().document(taskId)
                .update("status", status, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void deleteTask(String taskId, Callback<Void> cb) {
        tasksCol().document(taskId).get().addOnSuccessListener(d -> {
            if (!d.exists()) { cb.onSuccess(null); return; }
            Task t = fromDoc(d);

            if (isFinishedOrMissed(t)) {
                cb.onError(new IllegalStateException("Cannot delete finished/missed"));
                return;
            }

            if (!t.isRecurring()) {
                tasksCol().document(taskId).delete()
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
                return;
            }

            long now = System.currentTimeMillis();

            occCol(taskId).whereGreaterThanOrEqualTo("dueDateTime", now).get()
                    .addOnSuccessListener(snap -> {
                        WriteBatch b = db.batch();
                        for (DocumentSnapshot x : snap.getDocuments()) {
                            b.delete(x.getReference());
                        }

                        b.update(tasksCol().document(taskId),
                                "status", "CANCELLED",
                                "updatedAt", FieldValue.serverTimestamp()
                        );

                        b.commit()
                                .addOnSuccessListener(v -> cb.onSuccess(null))
                                .addOnFailureListener(cb::onError);
                    })
                    .addOnFailureListener(cb::onError);

        }).addOnFailureListener(cb::onError);
    }


    public void getOccurrencesForRange(String taskId, long start, long end, Callback<List<TaskOccurrence>> cb) {
        occCol(taskId)
                .whereGreaterThanOrEqualTo("dueDateTime", start)
                .whereLessThanOrEqualTo("dueDateTime", end)
                .get()
                .addOnSuccessListener(snap -> {
                    List<TaskOccurrence> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        TaskOccurrence o = new TaskOccurrence();
                        o.id = d.getId();
                        Long due = d.getLong("dueDateTime");
                        o.dueDateTime = due == null ? 0 : due;
                        o.status = d.getString("status");
                        Long xps = d.getLong("xpPointsSnapshot");
                        o.xpPointsSnapshot = xps == null ? 0 : xps.intValue();
                        list.add(o);
                    }
                    autoMarkMissedOccurrences(taskId, list, new Callback<Void>() {
                        @Override public void onSuccess(Void v) { cb.onSuccess(list); }
                        @Override public void onError(Exception e) { cb.onSuccess(list); }
                    });
                })
                .addOnFailureListener(cb::onError);
    }

    public void markOccurrenceDone(String taskId, String occId, int baseXp, String difficulty, String importance,
                                   Callback<Void> cb) {
        final long now = System.currentTimeMillis();
        final long threeDaysAgo = now - 3L*24*60*60*1000;

        final String diffN = difficulty;
        final String impN  = importance;

        db.runTransaction((Transaction.Function<Void>) tr -> {
                    DocumentReference occRef = occCol(taskId).document(occId);
                    DocumentSnapshot occ = tr.get(occRef);
                    if (!occ.exists()) throw new IllegalStateException("OCC_NOT_FOUND");

                    String status = occ.getString("status");
                    Long due = occ.getLong("dueDateTime");
                    if (!"ACTIVE".equals(status)) throw new IllegalStateException("NOT_ACTIVE");
                    if (due == null) throw new IllegalStateException("NO_DUE");
                    if (due < threeDaysAgo) throw new IllegalStateException("TOO_OLD");

                    DocumentReference dRef = dayQuotaRef(now);
                    DocumentReference wRef = weekQuotaRef(now);
                    DocumentReference mRef = monthQuotaRef(now);

                    DocumentSnapshot dSnap = tr.get(dRef);
                    DocumentSnapshot wSnap = tr.get(wRef);
                    DocumentSnapshot mSnap = tr.get(mRef);

                    int veryEasy = safeInt(dSnap.getLong("veryEasy"));
                    int easy = safeInt(dSnap.getLong("easy"));
                    int hard = safeInt(dSnap.getLong("hard"));
                    int normal = safeInt(dSnap.getLong("normal"));
                    int important = safeInt(dSnap.getLong("important"));
                    int extImportant = safeInt(dSnap.getLong("extImportant"));
                    int extHard = safeInt(wSnap.getLong("extHard"));
                    int special = safeInt(mSnap.getLong("special"));

                    boolean allowedDiff = true;
                    boolean allowedImp  = true;

                    if ("VEOMA_LAK".equals(diffN) && veryEasy >= 5) allowedDiff = false;
                    if ("LAK".equals(diffN) && easy >= 5) allowedDiff = false;
                    if ("TEZAK".equals(diffN) && hard >= 2) allowedDiff = false;
                    if ("EKSTREMNO_TEZAK".equals(diffN) && extHard >= 1) allowedDiff = false;

                    if ("NORMALAN".equals(impN) && normal >= 5) allowedImp = false;
                    if ("VAZAN".equals(impN) && important >= 5) allowedImp = false;
                    if ("EKSTREMNO_VAZAN".equals(impN) && extImportant >= 2) allowedImp = false;
                    if ("SPECIJALAN".equals(impN) && special >= 1) allowedImp = false;

                    int diffXp = xpForDifficulty(diffN);
                    int impXp  = xpForImportance(impN);
                    int awardXp = (allowedDiff ? diffXp : 0) + (allowedImp ? impXp : 0);

                    tr.update(occRef, "status", "DONE", "updatedAt", FieldValue.serverTimestamp());
                    Map<String, Object> log = new HashMap<>();
                    log.put("taskId", taskId);
                    log.put("occurrenceId", occId);
                    log.put("completedAt", now);
                    log.put("difficulty", difficulty);
                    log.put("importance", importance);
                    log.put("xpAwarded", awardXp);
                    tr.set(logsCol().document(), log);

                    Map<String, Object> dayUpd = new HashMap<>();
                    if (allowedDiff) {
                        if ("VEOMA_LAK".equals(diffN)) dayUpd.put("veryEasy", veryEasy + 1);
                        if ("LAK".equals(diffN)) dayUpd.put("easy", easy + 1);
                        if ("TEZAK".equals(diffN)) dayUpd.put("hard", hard + 1);
                    }
                    if (allowedImp) {
                        if ("NORMALAN".equals(impN)) dayUpd.put("normal", normal + 1);
                        if ("VAZAN".equals(impN)) dayUpd.put("important", important + 1);
                        if ("EKSTREMNO_VAZAN".equals(impN)) dayUpd.put("extImportant", extImportant + 1);
                    }
                    if (!dayUpd.isEmpty()) tr.set(dRef, dayUpd, SetOptions.merge());

                    if (allowedDiff && "EKSTREMNO_TEZAK".equals(diffN)) {
                        Map<String, Object> wUpd = new HashMap<>();
                        wUpd.put("extHard", extHard + 1);
                        tr.set(wRef, wUpd, SetOptions.merge());
                    }
                    if (allowedImp && "SPECIJALAN".equals(impN)) {
                        Map<String, Object> mUpd = new HashMap<>();
                        mUpd.put("special", special + 1);
                        tr.set(mRef, mUpd, SetOptions.merge());
                    }
                    return null;
                }).addOnSuccessListener(v -> {
                    try {
                        new com.example.mobilneaplikacije.data.repository.SpecialMissionRepository()
                                .recordTaskCompletionAuto(difficulty, importance, null);
                        new com.example.mobilneaplikacije.data.repository.SpecialMissionRepository()
                                .tryApplyNoUnresolvedBonusForMyAlliance(null);
                    } catch (Exception ignored) { }
                    cb.onSuccess(v);
                })
                .addOnFailureListener(cb::onError);
    }

    public void markSingleDone(String taskId, int baseXp, String difficulty, String importance,
                               Callback<Void> cb) {
        final long now = System.currentTimeMillis();
        final long threeDaysAgo = now - 3L*24*60*60*1000;

        final String diffN = difficulty;
        final String impN  = importance;

        db.runTransaction((Transaction.Function<Void>) tr -> {
                    DocumentReference tRef = tasksCol().document(taskId);
                    DocumentSnapshot d = tr.get(tRef);
                    if (!d.exists()) throw new IllegalStateException("TASK_NOT_FOUND");

                    String status = d.getString("status");
                    Long due = d.getLong("dueDateTime");
                    Boolean recurring = d.getBoolean("isRecurring");
                    if (Boolean.TRUE.equals(recurring)) throw new IllegalStateException("USE_OCCURRENCE");
                    if (!"ACTIVE".equals(status)) throw new IllegalStateException("NOT_ACTIVE");
                    if (due == null) throw new IllegalStateException("NO_DUE");
                    if (due < threeDaysAgo) throw new IllegalStateException("TOO_OLD");

                    DocumentReference dRef = dayQuotaRef(now);
                    DocumentReference wRef = weekQuotaRef(now);
                    DocumentReference mRef = monthQuotaRef(now);

                    DocumentSnapshot dSnap = tr.get(dRef);
                    DocumentSnapshot wSnap = tr.get(wRef);
                    DocumentSnapshot mSnap = tr.get(mRef);

                    int veryEasy = safeInt(dSnap.getLong("veryEasy"));
                    int easy = safeInt(dSnap.getLong("easy"));
                    int hard = safeInt(dSnap.getLong("hard"));
                    int normal = safeInt(dSnap.getLong("normal"));
                    int important = safeInt(dSnap.getLong("important"));
                    int extImportant = safeInt(dSnap.getLong("extImportant"));
                    int extHard = safeInt(wSnap.getLong("extHard"));
                    int special = safeInt(mSnap.getLong("special"));

                    boolean allowedDiff = true;
                    boolean allowedImp  = true;
                    if ("VEOMA_LAK".equals(diffN) && veryEasy >= 5) allowedDiff = false;
                    if ("LAK".equals(diffN) && easy >= 5) allowedDiff = false;
                    if ("TEZAK".equals(diffN) && hard >= 2) allowedDiff = false;
                    if ("EKSTREMNO_TEZAK".equals(diffN) && extHard >= 1) allowedDiff = false;

                    if ("NORMALAN".equals(impN) && normal >= 5) allowedImp = false;
                    if ("VAZAN".equals(impN) && important >= 5) allowedImp = false;
                    if ("EKSTREMNO_VAZAN".equals(impN) && extImportant >= 2) allowedImp = false;
                    if ("SPECIJALAN".equals(impN) && special >= 1) allowedImp = false;

                    int diffXp = xpForDifficulty(diffN);
                    int impXp  = xpForImportance(impN);
                    int awardXp = (allowedDiff ? diffXp : 0) + (allowedImp ? impXp : 0);

                    tr.update(tRef, "status", "DONE", "updatedAt", FieldValue.serverTimestamp());

                    Map<String, Object> log = new HashMap<>();
                    log.put("taskId", taskId);
                    log.put("occurrenceId", null);
                    log.put("completedAt", now);
                    log.put("difficulty", difficulty);
                    log.put("importance", importance);
                    log.put("xpAwarded", awardXp);
                    tr.set(logsCol().document(), log);

                    Map<String, Object> dayUpd = new HashMap<>();
                    if (allowedDiff) {
                        if ("VEOMA_LAK".equals(diffN)) dayUpd.put("veryEasy", veryEasy + 1);
                        if ("LAK".equals(diffN)) dayUpd.put("easy", easy + 1);
                        if ("TEZAK".equals(diffN)) dayUpd.put("hard", hard + 1);
                    }
                    if (allowedImp) {
                        if ("NORMALAN".equals(impN)) dayUpd.put("normal", normal + 1);
                        if ("VAZAN".equals(impN)) dayUpd.put("important", important + 1);
                        if ("EKSTREMNO_VAZAN".equals(impN)) dayUpd.put("extImportant", extImportant + 1);
                    }
                    if (!dayUpd.isEmpty()) tr.set(dRef, dayUpd, SetOptions.merge());

                    if (allowedDiff && "EKSTREMNO_TEZAK".equals(diffN)) {
                        Map<String, Object> wUpd = new HashMap<>();
                        wUpd.put("extHard", extHard + 1);
                        tr.set(wRef, wUpd, SetOptions.merge());
                    }
                    if (allowedImp && "SPECIJALAN".equals(impN)) {
                        Map<String, Object> mUpd = new HashMap<>();
                        mUpd.put("special", special + 1);
                        tr.set(mRef, mUpd, SetOptions.merge());
                    }

                    return null;
                }).addOnSuccessListener(v -> {
                    try {
                        new com.example.mobilneaplikacije.data.repository.SpecialMissionRepository()
                                .recordTaskCompletionAuto(difficulty, importance, null);
                        new com.example.mobilneaplikacije.data.repository.SpecialMissionRepository()
                                .tryApplyNoUnresolvedBonusForMyAlliance(null);
                    } catch (Exception ignored) { }
                    cb.onSuccess(v);
                })
                .addOnFailureListener(cb::onError);
    }

    private static int xpForDifficulty(String diff) {
        if ("VEOMA_LAK".equals(diff)) return 1;
        if ("LAK".equals(diff)) return 3;
        if ("TEZAK".equals(diff)) return 7;
        if ("EKSTREMNO_TEZAK".equals(diff)) return 20;
        return 0;
    }
    private static int xpForImportance(String imp) {
        if ("NORMALAN".equals(imp)) return 1;
        if ("VAZAN".equals(imp)) return 3;
        if ("EKSTREMNO_VAZAN".equals(imp)) return 10;
        if ("SPECIJALAN".equals(imp)) return 100;
        return 0;
    }
    public void cancelOccurrence(String taskId, String occId, Callback<Void> cb) {
        occCol(taskId).document(occId)
                .update("status", "CANCELLED")
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void pauseRecurringMaster(String taskId, Callback<Void> cb) {
        tasksCol().document(taskId)
                .update("status", "PAUSED", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void activateRecurringMaster(String taskId, Callback<Void> cb) {
        tasksCol().document(taskId)
                .update("status", "ACTIVE", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    private Task fromDoc(DocumentSnapshot d) {
        Task t = new Task();
        t.setIdHash(d.getId());
        t.setTitle(d.getString("title"));
        t.setDescription(d.getString("description"));
        t.setCategoryIdHash(d.getString("categoryId"));
        t.setDifficulty(d.getString("difficulty"));
        t.setImportance(d.getString("importance"));
        Long xp = d.getLong("xpPoints"); t.setXpPoints(xp == null ? 0 : xp.intValue());
        t.setStatus(d.getString("status"));
        Boolean rec = d.getBoolean("isRecurring"); t.setRecurring(Boolean.TRUE.equals(rec));
        Long ri = d.getLong("repeatInterval"); t.setRepeatInterval(ri == null ? 0 : ri.intValue());
        t.setRepeatUnit(d.getString("repeatUnit"));
        Long sd = d.getLong("startDate"); t.setStartDate(sd == null ? 0 : sd);
        Long ed = d.getLong("endDate");   t.setEndDate(ed == null ? 0 : ed);
        Long due = d.getLong("dueDateTime"); t.setDueDateTime(due == null ? 0 : due);
        return t;
    }

    private boolean canEditMaster(Task t) {
        if ("DONE".equals(t.getStatus()) || "MISSED".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus()))
            return false;

        return t.isRecurring() || t.getDueDateTime() >= System.currentTimeMillis();
    }

    private boolean isFinishedOrMissed(Task t) {
        return "DONE".equals(t.getStatus()) || "MISSED".equals(t.getStatus());
    }

    private void generateOccurrences(String taskId, Task t, int upToDays, Callback<Void> cb) {
        if (!t.isRecurring()) { cb.onSuccess(null); return; }
        long now = System.currentTimeMillis();
        long end = t.getEndDate() == 0 ? now + upToDays * 24L*60*60*1000
                : Math.min(t.getEndDate(), now + upToDays * 24L*60*60*1000);
        if (t.getStartDate() == 0 || end < t.getStartDate()) { cb.onSuccess(null); return; }

        WriteBatch b = db.batch();
        long cur = t.getStartDate();
        while (cur <= end) {
            DocumentReference r = occCol(taskId).document();
            Map<String, Object> o = new HashMap<>();
            o.put("dueDateTime", cur);
            o.put("status", "ACTIVE");
            o.put("xpPointsSnapshot", t.getXpPoints());
            o.put("createdAt", FieldValue.serverTimestamp());
            b.set(r, o);
            cur = addUnit(cur, t.getRepeatUnit(), t.getRepeatInterval());
        }
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    private void deleteOccurrencesFrom(String taskId, long fromMillis, Callback<Void> cb) {
        occCol(taskId).whereGreaterThanOrEqualTo("dueDateTime", fromMillis).get()
                .addOnSuccessListener(snap -> {
                    WriteBatch b = db.batch();
                    for (DocumentSnapshot d : snap.getDocuments()) b.delete(d.getReference());
                    b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    private long addUnit(long base, String unit, int step) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(base);
        if ("WEEK".equals(unit)) c.add(Calendar.WEEK_OF_YEAR, step);
        else c.add(Calendar.DAY_OF_YEAR, step);
        return c.getTimeInMillis();
    }

    private void autoMarkMissedSingles(List<Task> list) {
        for (Task t : list) autoMarkMissedSingle(t, null);
    }
    private void autoMarkMissedSingle(Task t, @Nullable Callback<Void> cb) {
        if (t == null || t.isRecurring()) { if (cb!=null) cb.onSuccess(null); return; }
        long threeDaysAgo = System.currentTimeMillis() - 3L*24*60*60*1000;
        if ("ACTIVE".equals(t.getStatus()) && t.getDueDateTime() < threeDaysAgo) {
            tasksCol().document(t.getIdHash())
                    .update("status","MISSED","updatedAt",FieldValue.serverTimestamp())
                    .addOnSuccessListener(v->{ if(cb!=null) cb.onSuccess(null); })
                    .addOnFailureListener(e->{ if(cb!=null) cb.onError(e); });
        } else if (cb!=null) cb.onSuccess(null);
    }
    private void autoMarkMissedOccurrences(String taskId, List<TaskOccurrence> occs, Callback<Void> cb) {
        long threeDaysAgo = System.currentTimeMillis() - 3L*24*60*60*1000;
        WriteBatch b = db.batch();
        boolean any = false;
        for (TaskOccurrence o : occs) {
            if ("ACTIVE".equals(o.status) && o.dueDateTime < threeDaysAgo) {
                b.update(occCol(taskId).document(o.id), "status", "MISSED");
                any = true;
            }
        }
        if (!any) { cb.onSuccess(null); return; }
        b.commit().addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void calculateSuccessRate(long fromMillis, long toMillis, Callback<Double> cb) {
        final int[] totalCreated = {0};
        final int[] donePos = {0};
        final int[] overQuota = {0};
        final int[] pending = {3};

        Timestamp fromTs = new Timestamp(new Date(fromMillis));
        Timestamp toTs   = new Timestamp(new Date(toMillis));

        tasksCol()
                .whereEqualTo("isRecurring", false)
                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                .whereLessThanOrEqualTo("createdAt", toTs)
                .get(Source.SERVER)
                .addOnSuccessListener(snap -> {
                    int cnt = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String st = d.getString("status");
                        if ("PAUSED".equals(st) || "CANCELLED".equals(st)) continue;
                        cnt++;
                    }
                    totalCreated[0] += cnt;
                    if (--pending[0] == 0) {
                        int denom = Math.max(0, totalCreated[0] - overQuota[0]);
                        cb.onSuccess(denom == 0 ? 0.0 : (donePos[0] * 100.0 / denom));
                    }
                })
                .addOnFailureListener(cb::onError);

        tasksCol().whereEqualTo("isRecurring", true).get(Source.SERVER)
                .addOnSuccessListener(recSnap -> {
                    if (recSnap.isEmpty()) {
                        if (--pending[0] == 0) {
                            int denom = Math.max(0, totalCreated[0] - overQuota[0]);
                            cb.onSuccess(denom == 0 ? 0.0 : (donePos[0] * 100.0 / denom));
                        }
                        return;
                    }
                    for (DocumentSnapshot master : recSnap.getDocuments()) {
                        pending[0]++;
                        occCol(master.getId())
                                .whereGreaterThanOrEqualTo("createdAt", fromTs)
                                .whereLessThanOrEqualTo("createdAt", toTs)
                                .get(Source.SERVER)
                                .addOnSuccessListener(occSnap -> {
                                    int cnt = 0;
                                    for (DocumentSnapshot od : occSnap.getDocuments()) {
                                        String st = od.getString("status");
                                        if ("PAUSED".equals(st) || "CANCELLED".equals(st)) continue;
                                        cnt++;
                                    }
                                    totalCreated[0] += cnt;
                                    if (--pending[0] == 0) {
                                        int denom = Math.max(0, totalCreated[0] - overQuota[0]);
                                        cb.onSuccess(denom == 0 ? 0.0 : (donePos[0] * 100.0 / denom));
                                    }
                                })
                                .addOnFailureListener(cb::onError);
                    }
                    if (--pending[0] == 0) {
                        int denom = Math.max(0, totalCreated[0] - overQuota[0]);
                        cb.onSuccess(denom == 0 ? 0.0 : (donePos[0] * 100.0 / denom));
                    }
                })
                .addOnFailureListener(cb::onError);

        logsCol()
                .whereGreaterThanOrEqualTo("completedAt", fromMillis)
                .whereLessThanOrEqualTo("completedAt", toMillis)
                .get(Source.SERVER)
                .addOnSuccessListener(logSnap -> {
                    int pos = 0, over = 0;
                    for (DocumentSnapshot d : logSnap.getDocuments()) {
                        Long xpL = d.getLong("xpAwarded");
                        int xp = (xpL == null) ? 1 : xpL.intValue();
                        if (xp > 0) pos++; else over++;
                    }
                    donePos[0] = pos;
                    overQuota[0] = over;
                    if (--pending[0] == 0) {
                        int denom = Math.max(0, totalCreated[0] - overQuota[0]);
                        cb.onSuccess(denom == 0 ? 0.0 : (donePos[0] * 100.0 / denom));
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public interface StageSuccessCallback { void onSuccess(int successPercent, long totalCounted); void onError(Exception e); }
}

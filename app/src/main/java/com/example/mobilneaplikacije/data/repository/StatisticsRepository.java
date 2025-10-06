package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Statistics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class StatisticsRepository {

    public interface Callback<T> {
        void onSuccess(@Nullable T data);
        void onError(Exception e);
    }

    interface Callback2<T1, T2> {
        void onSuccess(T1 val1, T2 val2);
        void onError(Exception e);
    }

    private final FirebaseFirestore db;
    private final String uid;

    public StatisticsRepository() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("User not logged in");
        this.uid = u.getUid();
    }

    private CollectionReference tasksCol() {
        return db.collection("users").document(uid).collection("tasks");
    }

    private CollectionReference logsCol() {
        return db.collection("users").document(uid).collection("completionLogs");
    }

    public void loadStatistics(final Callback<Statistics> cb) {
        final Statistics stats = new Statistics();
        final int[] pendingCalls = {7};
        final Exception[] firstError = {null};

        final Runnable checkComplete = new Runnable() {
            @Override
            public void run() {
                synchronized (pendingCalls) {
                    pendingCalls[0]--;
                    if (pendingCalls[0] == 0) {
                        if (firstError[0] != null) {
                            cb.onError(firstError[0]);
                        } else {
                            cb.onSuccess(stats);
                        }
                    }
                }
            }
        };

        loadConsecutiveActiveDays(new Callback<Integer>() {
            @Override
            public void onSuccess(@Nullable Integer days) {
                if (days != null) stats.consecutiveActiveDays = days;
                checkComplete.run();
            }
            @Override
            public void onError(Exception e) {
                if (firstError[0] == null) firstError[0] = e;
                checkComplete.run();
            }
        });

        loadTaskCounts(stats, new Runnable() {
            @Override
            public void run() {
                checkComplete.run();
            }
        });

        loadLongestStreak(new Callback<Integer>() {
            @Override
            public void onSuccess(@Nullable Integer streak) {
                if (streak != null) stats.longestCompletionStreak = streak;
                checkComplete.run();
            }
            @Override
            public void onError(Exception e) {
                if (firstError[0] == null) firstError[0] = e;
                checkComplete.run();
            }
        });

        loadTasksByCategory(new Callback<Map<String, Integer>>() {
            @Override
            public void onSuccess(@Nullable Map<String, Integer> map) {
                if (map != null) stats.tasksByCategory = map;
                checkComplete.run();
            }
            @Override
            public void onError(Exception e) {
                if (firstError[0] == null) firstError[0] = e;
                checkComplete.run();
            }
        });

        loadXpLast7Days(new Callback<Map<String, Double>>() {
            @Override
            public void onSuccess(@Nullable Map<String, Double> xpMap) {
                if (xpMap != null) stats.xpByDay = xpMap;
                checkComplete.run();
            }
            @Override
            public void onError(Exception e) {
                if (firstError[0] == null) firstError[0] = e;
                checkComplete.run();
            }
        });

        loadSpecialMissionsCounts(new Callback2<Integer, Integer>() {
            @Override
            public void onSuccess(Integer started, Integer completed) {
                stats.specialMissionsStarted = started != null ? started : 0;
                stats.specialMissionsCompleted = completed != null ? completed : 0;
                checkComplete.run();
            }
            @Override
            public void onError(Exception e) {
                if (firstError[0] == null) firstError[0] = e;
                checkComplete.run();
            }
        });

        loadTasksByDifficulty(new Callback<Map<String, Integer>>() {
            @Override
            public void onSuccess(@Nullable Map<String, Integer> xpByDiff) {
                if (xpByDiff != null) stats.tasksByDifficulty = xpByDiff;
                checkComplete.run();
            }
            @Override
            public void onError(Exception e) {
                if (firstError[0] == null) firstError[0] = e;
                checkComplete.run();
            }
        });
    }

    private void loadConsecutiveActiveDays(final Callback<Integer> cb) {
        logsCol().orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(365)
                .get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        Set<String> activeDays = new HashSet<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Long completedAt = d.getLong("completedAt");
                            if (completedAt != null) {
                                activeDays.add(sdf.format(new Date(completedAt)));
                            }
                        }

                        Calendar cal = Calendar.getInstance();
                        int streak = 0;
                        for (int i = 0; i < 365; i++) {
                            String dayKey = sdf.format(cal.getTime());
                            if (activeDays.contains(dayKey)) {
                                streak++;
                            } else if (streak > 0 || i == 0) {
                                if (i > 0) break;
                            }
                            cal.add(Calendar.DAY_OF_YEAR, -1);
                        }
                        cb.onSuccess(streak);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadTaskCounts(final Statistics stats, final Runnable onComplete) {
        tasksCol().get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        int created = 0, completed = 0, missed = 0, cancelled = 0;

                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String status = d.getString("status");
                            created++;
                            if ("DONE".equals(status)) completed++;
                            else if ("MISSED".equals(status)) missed++;
                            else if ("CANCELLED".equals(status)) cancelled++;
                        }

                        stats.totalCreatedTasks = created;
                        stats.totalCompletedTasks = completed;
                        stats.totalMissedTasks = missed;
                        stats.totalCancelledTasks = cancelled;
                        onComplete.run();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        onComplete.run();
                    }
                });
    }

    private void loadLongestStreak(final Callback<Integer> cb) {
        logsCol().orderBy("completedAt", Query.Direction.ASCENDING)
                .get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
                        Map<String, Boolean> dayMap = new HashMap<>();

                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Long completedAt = d.getLong("completedAt");
                            if (completedAt != null) {
                                dayMap.put(sdf.format(new Date(completedAt)), true);
                            }
                        }

                        List<String> sortedDays = new ArrayList<>(dayMap.keySet());
                        Collections.sort(sortedDays);

                        int maxStreak = 0, currentStreak = 0;
                        String lastDay = null;

                        for (String day : sortedDays) {
                            if (lastDay == null) {
                                currentStreak = 1;
                            } else {
                                Calendar prev = Calendar.getInstance();
                                try {
                                    prev.setTime(sdf.parse(lastDay));
                                    prev.add(Calendar.DAY_OF_YEAR, 1);
                                    String expectedNext = sdf.format(prev.getTime());

                                    if (expectedNext.equals(day)) {
                                        currentStreak++;
                                    } else {
                                        maxStreak = Math.max(maxStreak, currentStreak);
                                        currentStreak = 1;
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            lastDay = day;
                        }
                        maxStreak = Math.max(maxStreak, currentStreak);
                        cb.onSuccess(maxStreak);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadTasksByCategory(final Callback<Map<String, Integer>> cb) {
        logsCol().get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot logSnap) {
                        final Set<String> taskIds = new HashSet<>();
                        for (DocumentSnapshot d : logSnap.getDocuments()) {
                            String taskId = d.getString("taskId");
                            if (taskId != null) taskIds.add(taskId);
                        }

                        if (taskIds.isEmpty()) {
                            cb.onSuccess(new HashMap<String, Integer>());
                            return;
                        }

                        final Map<String, Integer> categoryCount = new HashMap<>();
                        final int[] pending = {taskIds.size()};

                        for (final String taskId : taskIds) {
                            tasksCol().document(taskId).get()
                                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(final DocumentSnapshot taskDoc) {
                                            if (taskDoc.exists()) {
                                                String catId = taskDoc.getString("categoryId");
                                                if (catId != null) {
                                                    db.collection("users").document(uid)
                                                            .collection("categories").document(catId).get()
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onSuccess(DocumentSnapshot catDoc) {
                                                                    String catName = catDoc.exists() ?
                                                                            catDoc.getString("name") : "Ostalo";
                                                                    synchronized (categoryCount) {
                                                                        categoryCount.put(catName,
                                                                                categoryCount.containsKey(catName) ? categoryCount.get(catName) + 1 : 1);
                                                                    }
                                                                    checkDone();
                                                                }

                                                                void checkDone() {
                                                                    synchronized (pending) {
                                                                        pending[0]--;
                                                                        if (pending[0] == 0) {
                                                                            cb.onSuccess(categoryCount);
                                                                        }
                                                                    }
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(Exception e) {
                                                                    synchronized (pending) {
                                                                        pending[0]--;
                                                                        if (pending[0] == 0) {
                                                                            cb.onSuccess(categoryCount);
                                                                        }
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    synchronized (categoryCount) {
                                                        String key = "Bez kategorije";
                                                        categoryCount.put(key, categoryCount.containsKey(key) ? categoryCount.get(key) + 1 : 1);
                                                    }
                                                    synchronized (pending) {
                                                        pending[0]--;
                                                        if (pending[0] == 0) {
                                                            cb.onSuccess(categoryCount);
                                                        }
                                                    }
                                                }
                                            } else {
                                                synchronized (pending) {
                                                    pending[0]--;
                                                    if (pending[0] == 0) {
                                                        cb.onSuccess(categoryCount);
                                                    }
                                                }
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            synchronized (pending) {
                                                pending[0]--;
                                                if (pending[0] == 0) {
                                                    cb.onSuccess(categoryCount);
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadXpLast7Days(final Callback<Map<String, Double>> cb) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        long sevenDaysAgo = cal.getTimeInMillis();

        logsCol().whereGreaterThanOrEqualTo("completedAt", sevenDaysAgo)
                .get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
                        Map<String, Double> xpMap = new HashMap<>();

                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Long completedAt = d.getLong("completedAt");
                            Long xpAwarded = d.getLong("xpAwarded");

                            if (completedAt != null && xpAwarded != null) {
                                String dayKey = sdf.format(new Date(completedAt));
                                double current = xpMap.containsKey(dayKey) ? xpMap.get(dayKey) : 0.0;
                                xpMap.put(dayKey, current + xpAwarded);
                            }
                        }
                        cb.onSuccess(xpMap);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        cb.onError(e);
                    }
                });
    }

    private void loadSpecialMissionsCounts(final Callback2<Integer, Integer> cb) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot userDoc) {
                        Long won = userDoc.getLong("specialMissionsWon");
                        int count = won != null ? won.intValue() : 0;
                        cb.onSuccess(count, count);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        cb.onError(e);
                    }
                });
    }

    public void loadTasksByDifficulty(final Callback<Map<String, Integer>> cb) {
        logsCol().get(Source.SERVER)
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        Map<String, Integer> difficultyXp = new HashMap<>();
                        difficultyXp.put("VEOMA_LAK", 0);
                        difficultyXp.put("LAK", 0);
                        difficultyXp.put("TEZAK", 0);
                        difficultyXp.put("EKSTREMNO_TEZAK", 0);

                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String diff = d.getString("difficulty");
                            Long xp = d.getLong("xpAwarded");
                            if (diff != null && xp != null && xp > 0) {
                                if (difficultyXp.containsKey(diff)) {
                                    // Saberi XP umesto brojanja zadataka
                                    difficultyXp.put(diff, difficultyXp.get(diff) + xp.intValue());
                                }
                            }
                        }
                        cb.onSuccess(difficultyXp);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        cb.onError(e); // ISPRAVLJENO: onError umesto onSuccess
                    }
                });
    }
}

package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.*;

import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.manager.LevelManager;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

import com.example.mobilneaplikacije.ui.boss.BossFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK_ID_HASH = "task_id_hash";
    private static final String ARG_OCCURRENCE_ID = "occurrence_id";
    private String taskIdHash;
    private String occurrenceId;
    private ListenerRegistration taskReg, occReg;
    private String currentStatusLabel = "";
    private ListenerRegistration catReg;
    private String currentCategoryId;
    private boolean lastTaskIsRecurring = false;
    private String lastTaskStatus = null;
    private TextView tvTitle, tvDescription, tvCategory, tvDifficulty, tvImportance, tvXpPoints, tvStatus;
    private Button btnMarkDone, btnPause, btnCancel, btnDelete, btnEdit;

    public TaskDetailFragment() {
    }

    private Task taskFromSnap(DocumentSnapshot d) {
        Task t = new Task();
        t.setIdHash(d.getId());
        t.setTitle(d.getString("title"));
        t.setDescription(d.getString("description"));
        t.setCategoryIdHash(d.getString("categoryId"));
        t.setDifficulty(d.getString("difficulty"));
        t.setImportance(d.getString("importance"));
        Long xp = d.getLong("xpPoints");
        t.setXpPoints(xp == null ? 0 : xp.intValue());
        t.setStatus(d.getString("status"));
        Boolean rec = d.getBoolean("isRecurring");
        t.setRecurring(Boolean.TRUE.equals(rec));
        Long ri = d.getLong("repeatInterval");
        t.setRepeatInterval(ri == null ? 0 : ri.intValue());
        t.setRepeatUnit(d.getString("repeatUnit"));
        Long sd = d.getLong("startDate");
        t.setStartDate(sd == null ? 0 : sd);
        Long ed = d.getLong("endDate");
        t.setEndDate(ed == null ? 0 : ed);
        Long due = d.getLong("dueDateTime");
        t.setDueDateTime(due == null ? 0 : due);
        return t;
    }

    private void loadCategoryName(String categoryIdHash) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("categories").document(categoryIdHash)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    String name = snap != null ? snap.getString("name") : null;
                    tvCategory.setText("Kategorija: " + (name != null ? name : "-"));
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    tvCategory.setText("Kategorija: -");
                });
    }

    private void bindTask(Task t) {
        tvTitle.setText(t.getTitle());
        tvDescription.setText(t.getDescription());
        loadCategoryName(t.getCategoryIdHash());
        tvDifficulty.setText("Težina: " + t.getDifficulty());
        tvImportance.setText("Bitnost: " + t.getImportance());
        tvXpPoints.setText("XP: " + t.getXpPoints());

        if (occurrenceId == null || !t.isRecurring()) {
            tvStatus.setText("Status: " + t.getStatus());
            currentStatusLabel = t.getStatus();
        } else {
            tvStatus.setText("Status (pojava): " + currentStatusLabel);
        }
        lastTaskIsRecurring = t.isRecurring();
        lastTaskStatus = t.getStatus();
    }

    public static TaskDetailFragment newInstance(String taskIdHash, @Nullable String occurrenceId) {
        TaskDetailFragment f = new TaskDetailFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TASK_ID_HASH, taskIdHash);
        if (occurrenceId != null) b.putString(ARG_OCCURRENCE_ID, occurrenceId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_detail, container, false);

        if (getArguments() != null) {
            taskIdHash = getArguments().getString(ARG_TASK_ID_HASH);
            occurrenceId = getArguments().getString(ARG_OCCURRENCE_ID);
        }

        tvTitle = v.findViewById(R.id.tvTitle);
        tvDescription = v.findViewById(R.id.tvDescription);
        tvCategory = v.findViewById(R.id.tvCategory);
        tvDifficulty = v.findViewById(R.id.tvDifficulty);
        tvImportance = v.findViewById(R.id.tvImportance);
        tvXpPoints = v.findViewById(R.id.tvXpPoints);
        tvStatus = v.findViewById(R.id.tvStatus);
        btnMarkDone = v.findViewById(R.id.btnMarkDone);
        btnPause = v.findViewById(R.id.btnPause);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnDelete = v.findViewById(R.id.btnDelete);
        btnEdit = v.findViewById(R.id.btnEdit);

        TaskRepository repo = new TaskRepository();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference taskRef = db.collection("users").document(uid)
                .collection("tasks").document(taskIdHash);

        taskReg = taskRef.addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists()) return;
            Task t = taskFromSnap(snap);
            bindTask(t);
            updateButtons(t.isRecurring(), t.getStatus(), currentStatusLabel);
        });

        if (occurrenceId != null) {
            DocumentReference occRef = taskRef.collection("occurrences").document(occurrenceId);
            occReg = occRef.addSnapshotListener((snap, err) -> {
                if (err != null || snap == null || !snap.exists()) return;
                String st = snap.getString("status");
                if (st == null) st = "";
                currentStatusLabel = st;
                tvStatus.setText("Status (pojava): " + st);
                updateButtons(lastTaskIsRecurring, lastTaskStatus, currentStatusLabel);
            });
        }
        btnEdit.setOnClickListener(x -> {
            AddTaskFragment fragment = AddTaskFragment.newInstanceForEdit(taskIdHash);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        btnMarkDone.setOnClickListener(x -> {
            repo.getTaskById(taskIdHash, new TaskRepository.Callback<Task>() {
                @Override public void onSuccess(Task t) {
                    if (t == null) return;

                    LevelManager lm = new LevelManager();
                    lm.getTaskDifficultyXp(t.getDifficulty(), new LevelManager.XpCallback() {
                        @Override public void onSucces(int diffXp) {
                            lm.getTaskImportanceXp(t.getImportance(), new LevelManager.XpCallback() {
                                @Override public void onSucces(int impXp) {
                                    final int totalXp = diffXp + impXp;

                                    TaskRepository.Callback<Void> afterDoneCb = new TaskRepository.Callback<Void>() {
                                        @Override public void onSuccess(@Nullable Void v) {
                                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                            FirebaseFirestore.getInstance()
                                                    .collection("users").document(uid)
                                                    .collection("completionLogs")
                                                    .whereEqualTo("taskId", taskIdHash)
                                                    .orderBy("completedAt", Query.Direction.DESCENDING)
                                                    .limit(1)
                                                    .get()
                                                    .addOnSuccessListener(qs -> {
                                                        int awarded = 0;
                                                        if (!qs.isEmpty()) {
                                                            Long xpL = qs.getDocuments().get(0).getLong("xpAwarded");
                                                            awarded = (xpL == null) ? totalXp : xpL.intValue();
                                                        }
                                                        if (awarded > 0) {
                                                            lm.addXp(awarded, player -> {
                                                                if (!isAdded()) return;
                                                                requireActivity().getSupportFragmentManager()
                                                                        .beginTransaction()
                                                                        .replace(R.id.fragment_container, new BossFragment())
                                                                        .addToBackStack(null)
                                                                        .commit();
                                                            });
                                                            if (awarded == totalXp) {
                                                                Toast.makeText(getContext(), "Urađeno! +" + awarded + " XP", Toast.LENGTH_LONG).show();
                                                            } else if (awarded == diffXp && impXp > 0) {
                                                                Toast.makeText(getContext(), "Delimično obračunato: +" + awarded + " XP (obračunata težina, bitnost preko kvote)", Toast.LENGTH_LONG).show();
                                                            } else if (awarded == impXp && diffXp > 0) {
                                                                Toast.makeText(getContext(), "Delimično obračunato: +" + awarded + " XP (obračunata bitnost, težina preko kvote)", Toast.LENGTH_LONG).show();
                                                            } else {
                                                                Toast.makeText(getContext(), "Urađeno! +" + awarded + " XP", Toast.LENGTH_LONG).show();
                                                            }
                                                        } else {
                                                            Toast.makeText(getContext(),"Urađeno! (bez XP, pređena kvota)",Toast.LENGTH_LONG).show();
                                                        }
                                                        updateButtons(lastTaskIsRecurring, "DONE", "DONE");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(getContext(),"Urađeno! (nepoznat XP)",Toast.LENGTH_LONG).show();
                                                        updateButtons(lastTaskIsRecurring, "DONE", "DONE");
                                                    });
                                        }
                                        @Override public void onError(Exception e) { handleDoneError(e); }
                                    };

                                    if (occurrenceId != null) {
                                        repo.markOccurrenceDone(taskIdHash, occurrenceId, totalXp, t.getDifficulty(), t.getImportance(), afterDoneCb);
                                    } else if (!t.isRecurring()) {
                                        repo.markSingleDone(taskIdHash, totalXp, t.getDifficulty(), t.getImportance(), afterDoneCb);
                                    } else {
                                        Toast.makeText(getContext(),"Za ponavljajući sa kalendara prosledi occurrence.",Toast.LENGTH_LONG).show();
                                    }
                                }
                                @Override public void onFailure(String msg) {
                                    Toast.makeText(getContext(),"Greška XP importance: " + msg,Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        @Override public void onFailure(String msg) {
                            Toast.makeText(getContext(),"Greška XP difficulty: " + msg,Toast.LENGTH_LONG).show();
                        }
                    });
                }
                @Override public void onError(Exception e) { }
            });
        });


        btnPause.setOnClickListener(x -> {
            repo.getTaskById(taskIdHash, new TaskRepository.Callback<Task>() {
                @Override
                public void onSuccess(Task t) {
                    if (t == null) return;
                    if (!t.isRecurring()) {
                        Toast.makeText(getContext(), "Samo ponavljajući zadaci se pauziraju.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if ("PAUSED".equals(t.getStatus())) {
                        repo.activateRecurringMaster(taskIdHash, toastCb("Zadatak aktiviran"));
                    } else if ("ACTIVE".equals(t.getStatus())) {
                        repo.pauseRecurringMaster(taskIdHash, toastCb("Zadatak pauziran"));
                    } else {
                        Toast.makeText(getContext(), "Akcija nije dozvoljena!", Toast.LENGTH_SHORT).show();
                    }
                    String newMaster = "ACTIVE".equals(lastTaskStatus) ? "PAUSED" : "ACTIVE";
                    lastTaskStatus = newMaster;
                    updateButtons(true, newMaster, null);
                }

                @Override
                public void onError(Exception e) {
                }
            });
        });

        btnCancel.setOnClickListener(x -> {
            if (occurrenceId != null) {
                new TaskRepository().cancelOccurrence(taskIdHash, occurrenceId, toastCb("Pojava otkazana"));
            } else {
                repo.updateTaskStatus(taskIdHash, "CANCELLED", toastCb("Zadatak otkazan"));
            }
            updateButtons(lastTaskIsRecurring, "CANCELLED", occurrenceId != null ? "CANCELLED" : null);
        });

        btnDelete.setOnClickListener(x -> {
            repo.deleteTask(taskIdHash, new TaskRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void v) {
                    Toast.makeText(getContext(), "Zadatak obrisan / skraćen (buduće pojave)", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Brisanje nije dozvoljeno: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (taskReg != null) taskReg.remove();
        if (occReg != null) occReg.remove();
        if (catReg != null) catReg.remove();
    }

    private TaskRepository.Callback<Void> toastCb(String msg) {
        return new TaskRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void v) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }

    private void updateButtons(boolean isRecurring,
                               @Nullable String masterStatus,
                               @Nullable String occurrenceStatus) {
        String s = (occurrenceId != null && isRecurring) ? occurrenceStatus : masterStatus;
        if (s == null) s = "";

        btnMarkDone.setEnabled(false);
        btnCancel.setEnabled(false);
        btnPause.setEnabled(false);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);

        boolean locked =
                "DONE".equals(s) || "MISSED".equals(s) || "CANCELLED".equals(s);
        btnEdit.setEnabled(!locked);
        btnDelete.setEnabled(!locked);

        boolean canAct = "ACTIVE".equals(s);
        btnMarkDone.setEnabled(canAct);
        btnCancel.setEnabled(canAct);

        if (isRecurring && occurrenceId == null) {
            if ("ACTIVE".equals(masterStatus)) {
                btnPause.setEnabled(true);
                btnPause.setText("Pauziraj zadatak");
            } else if ("PAUSED".equals(masterStatus)) {
                btnPause.setEnabled(true);
                btnPause.setText("Aktiviraj zadatak");
            } else {
                btnPause.setEnabled(false);
                btnPause.setText("Pauza");
            }
            btnPause.setVisibility(View.VISIBLE);
        } else {
            btnPause.setVisibility(View.GONE);
        }

        btnMarkDone.setText(canAct ? "Označi kao urađen" : "Urađeno / nije dostupno");
        btnCancel.setText(canAct ? "Otkaži zadatak" : "Otkaži (nije dostupno)");
    }


    private void handleDoneError(Exception e) {
        String msg = "Greška: " + e.getMessage();
        if ("XP_QUOTA_EXCEEDED".equals(e.getMessage())) msg = "Pređena je kvota, XP se ne dodeljuje.";
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
}

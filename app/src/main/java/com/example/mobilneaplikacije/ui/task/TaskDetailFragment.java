package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;

import androidx.fragment.app.Fragment;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

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

    public TaskDetailFragment() {}

    private Task taskFromSnap(DocumentSnapshot d) {
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

    @Nullable @Override
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

        TaskRepository repo = new TaskRepository(requireContext());
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
                    if (occurrenceId != null) {
                        // DONE occurrence
                        repo.markOccurrenceDone(taskIdHash, occurrenceId, t.getXpPoints(), t.getDifficulty(), t.getImportance(),
                                new TaskRepository.Callback<Integer>() {
                                    @Override public void onSuccess(@Nullable Integer award) {
                                        afterDoneXP(award == null ? 0 : award);
                                        updateButtons(lastTaskIsRecurring, "DONE", occurrenceId != null ? "DONE" : null);
                                    }
                                    @Override public void onError(Exception e) { handleDoneError(e); }
                                });

                    } else if (!t.isRecurring()) {
                        // DONE single
                        repo.markSingleDone(taskIdHash, t.getXpPoints(), t.getDifficulty(), t.getImportance(),
                                new TaskRepository.Callback<Integer>() {
                                    @Override public void onSuccess(@Nullable Integer award) {
                                        afterDoneXP(award == null ? 0 : award);
                                        updateButtons(lastTaskIsRecurring, "DONE", occurrenceId != null ? "DONE" : null);
                                    }
                                    @Override public void onError(Exception e) { handleDoneError(e); }
                                });
                    } else {
                        Toast.makeText(getContext(),"Za ponavljajući sa kalendara prosledi occurrence.",Toast.LENGTH_LONG).show();
                    }
                }
                @Override public void onError(Exception e) { }
            });
        });

        btnPause.setOnClickListener(x -> {
            repo.getTaskById(taskIdHash, new TaskRepository.Callback<Task>() {
                @Override public void onSuccess(Task t) {
                    if (t == null) return;
                    if (!t.isRecurring()) {
                        Toast.makeText(getContext(),"Samo ponavljajući zadaci se pauziraju.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if ("PAUSED".equals(t.getStatus())) {
                        repo.activateRecurringMaster(taskIdHash, toastCb("Zadatak aktiviran"));
                    } else if ("ACTIVE".equals(t.getStatus())) {
                        repo.pauseRecurringMaster(taskIdHash, toastCb("Zadatak pauziran"));
                    } else {
                        Toast.makeText(getContext(),"Akcija nije dozvoljena!",Toast.LENGTH_SHORT).show();
                    }
                    String newMaster = "ACTIVE".equals(lastTaskStatus) ? "PAUSED" : "ACTIVE";
                    lastTaskStatus = newMaster;
                    updateButtons(true, newMaster, null);
                }
                @Override public void onError(Exception e) { }
            });
        });

        btnCancel.setOnClickListener(x -> {
            if (occurrenceId != null) {
                new TaskRepository(requireContext()).cancelOccurrence(taskIdHash, occurrenceId, toastCb("Pojava otkazana"));
            } else {
                repo.updateTaskStatus(taskIdHash, "CANCELLED", toastCb("Zadatak otkazan"));
            }
            updateButtons(lastTaskIsRecurring, "CANCELLED", occurrenceId != null ? "CANCELLED" : null);
        });

        btnDelete.setOnClickListener(x -> {
            repo.deleteTask(taskIdHash, new TaskRepository.Callback<Void>() {
                @Override public void onSuccess(Void v) {
                    Toast.makeText(getContext(),"Zadatak obrisan / skraćen (buduće pojave)",Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                }
                @Override public void onError(Exception e) {
                    Toast.makeText(getContext(),"Brisanje nije dozvoljeno: "+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        });

        return v;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (taskReg != null) taskReg.remove();
        if (occReg != null) occReg.remove();
        if (catReg != null) catReg.remove();
    }

    private TaskRepository.Callback<Void> toastCb(String msg) {
        return new TaskRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) { Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show(); }
            @Override public void onError(Exception e) { Toast.makeText(getContext(), "Greška: "+e.getMessage(), Toast.LENGTH_LONG).show(); }
        };
    }

    private void afterDoneXP(int xpAwarded) {
        if (!isAdded()) return;

        String msg = xpAwarded > 0 ? ("Urađeno! +" + xpAwarded + " XP") : "Urađeno! (bez XP, pređena kvota)";
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

        btnMarkDone.setEnabled(false);
        btnCancel.setEnabled(false);

        Bundle res = new Bundle();
        res.putString("task_id_hash", taskIdHash);
        if (occurrenceId != null) res.putString("occurrence_id", occurrenceId);
        getParentFragmentManager().setFragmentResult("task_changed", res);
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
        if ("XP_QUOTA_EXCEEDED".equals(e.getMessage())) msg = "Pređena je dnevna/nev/mes kvota XP.";
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
}

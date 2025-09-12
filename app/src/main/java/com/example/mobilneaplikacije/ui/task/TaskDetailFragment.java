package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK_ID = "task_id";
    private long taskId;

    private TextView tvTitle, tvDescription, tvCategory, tvDifficulty, tvImportance, tvXpPoints, tvStatus;
    private Button btnMarkDone, btnPause, btnCancel, btnDelete, btnEdit;

    public TaskDetailFragment() {}

    public static TaskDetailFragment newInstance(long taskId) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_detail, container, false);

        if (getArguments() != null) {
            taskId = getArguments().getLong(ARG_TASK_ID);
        }

        tvTitle = view.findViewById(R.id.tvTitle);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvCategory = view.findViewById(R.id.tvCategory);
        tvDifficulty = view.findViewById(R.id.tvDifficulty);
        tvImportance = view.findViewById(R.id.tvImportance);
        tvXpPoints = view.findViewById(R.id.tvXpPoints);
        tvStatus = view.findViewById(R.id.tvStatus);

        btnMarkDone = view.findViewById(R.id.btnMarkDone);
        btnPause = view.findViewById(R.id.btnPause);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnEdit = view.findViewById(R.id.btnEdit);

        TaskRepository repo = new TaskRepository(requireContext());

        // ✏️ Edit
        btnEdit.setOnClickListener(v -> {
            Task t = repo.getTaskById(taskId);
            if (t == null) return;

            if ("DONE".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus()) || "MISSED".equals(t.getStatus())) {
                Toast.makeText(getContext(),
                        "Ne možeš menjati završene, propuštene ili otkazane zadatke!",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (t.isRecurring() && t.getDueDateTime() < System.currentTimeMillis()) {
                Toast.makeText(getContext(),
                        "Ne možeš menjati prošle pojave ponavljajućeg zadatka!",
                        Toast.LENGTH_LONG).show();
                return;
            }

            AddTaskFragment fragment = AddTaskFragment.newInstanceForEdit(taskId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // ✅ DONE
        btnMarkDone.setOnClickListener(v -> {
            Task t = repo.getTaskById(taskId);
            if (t == null) return;

            if (!"ACTIVE".equals(t.getStatus())) {
                Toast.makeText(getContext(), "Samo aktivni zadaci mogu biti završeni!", Toast.LENGTH_SHORT).show();
                return;
            }

            repo.updateTaskStatus(taskId, "DONE");

            int countToday = repo.countToday(t.getDifficulty(), t.getImportance());
            int countWeek = repo.countThisWeek(t.getDifficulty(), t.getImportance());
            int countMonth = repo.countThisMonth(t.getDifficulty(), t.getImportance());

            boolean canEarn = com.example.mobilneaplikacije.util.XPUtils
                    .canEarnXP(t.getDifficulty(), t.getImportance(), countToday, countWeek, countMonth);

            int awarded = canEarn ? t.getXpPoints() : 0;

            if (awarded > 0) {
                android.content.SharedPreferences prefs =
                        requireContext().getSharedPreferences("profile", android.content.Context.MODE_PRIVATE);
                int current = prefs.getInt("total_xp", 0);
                prefs.edit().putInt("total_xp", current + awarded).apply();
            }

            repo.logCompletion(taskId, System.currentTimeMillis(), t.getDifficulty(), t.getImportance(), awarded);

            Toast.makeText(getContext(),
                    awarded > 0 ? ("Urađeno! +" + awarded + " XP") : "Urađeno! (bez XP, pređena kvota)",
                    Toast.LENGTH_LONG).show();

            loadTaskDetails();
        });

        // ⏸ PAUSE ↔ ACTIVATE
        btnPause.setOnClickListener(v -> {
            Task t = repo.getTaskById(taskId);
            if (t == null) return;

            if (!t.isRecurring()) {
                Toast.makeText(getContext(), "Samo ponavljajući zadaci mogu biti pauzirani!", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("PAUSED".equals(t.getStatus())) {
                repo.updateTaskStatus(taskId, "ACTIVE");
                Toast.makeText(getContext(), "Zadatak ponovo aktiviran", Toast.LENGTH_SHORT).show();
            } else if ("ACTIVE".equals(t.getStatus())) {
                repo.updateTaskStatus(taskId, "PAUSED");
                Toast.makeText(getContext(), "Zadatak pauziran", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Ova akcija nije dozvoljena!", Toast.LENGTH_SHORT).show();
            }
            loadTaskDetails();
        });

        // ❌ CANCEL
        btnCancel.setOnClickListener(v -> {
            Task t = repo.getTaskById(taskId);
            if (t == null) return;

            if (!"ACTIVE".equals(t.getStatus())) {
                Toast.makeText(getContext(), "Samo aktivni zadaci mogu biti otkazani!", Toast.LENGTH_SHORT).show();
                return;
            }

            repo.updateTaskStatus(taskId, "CANCELLED");
            Toast.makeText(getContext(), "Zadatak otkazan", Toast.LENGTH_SHORT).show();
            loadTaskDetails();
        });

        // 🗑 DELETE
        btnDelete.setOnClickListener(v -> {
            Task t = repo.getTaskById(taskId);
            if (t == null) return;

            if ("DONE".equals(t.getStatus()) || "MISSED".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus())) {
                Toast.makeText(getContext(),
                        "Ne možeš obrisati završene, propuštene ili otkazane zadatke!",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (t.isRecurring()) {
                t.setEndDate(System.currentTimeMillis());
                repo.updateTask(t);
                Toast.makeText(getContext(), "Ponavljajući zadatak skraćen (obrisana buduća ponavljanja)", Toast.LENGTH_SHORT).show();
            } else {
                repo.deleteTask(taskId);
                Toast.makeText(getContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
            }

            requireActivity().onBackPressed();
        });

        loadTaskDetails();
        return view;
    }

    private void loadTaskDetails() {
        TaskRepository repo = new TaskRepository(requireContext());
        Task t = repo.getTaskById(taskId);
        if (t != null) {
            tvTitle.setText(t.getTitle());
            tvDescription.setText(t.getDescription());
            tvCategory.setText("Kategorija: " + t.getCategoryId());
            tvDifficulty.setText("Težina: " + t.getDifficulty());
            tvImportance.setText("Bitnost: " + t.getImportance());
            tvXpPoints.setText("XP: " + t.getXpPoints());
            tvStatus.setText("Status: " + t.getStatus());
        }
    }
}

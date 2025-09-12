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

    // Factory metoda za kreiranje novog fragmenta sa prosleđenim ID-jem zadatka
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

        btnEdit.setOnClickListener(v -> {
            TaskRepository repo = new TaskRepository(requireContext());
            for (Task t : repo.getAllTasks()) {
                if (t.getId() == taskId) {
                    if (t.isRecurring() && t.getDueDateTime() < System.currentTimeMillis()) {
                        Toast.makeText(getContext(),
                                "Ne možeš menjati prošle pojave ponavljajućeg zadatka!",
                                Toast.LENGTH_LONG).show();
                        return; // prekini edit
                    }
                }
            }

            AddTaskFragment fragment = AddTaskFragment.newInstanceForEdit(taskId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        loadTaskDetails();

        TaskRepository repo = new TaskRepository(requireContext());

        btnMarkDone.setOnClickListener(v -> {
            Task t = repo.getTaskById(taskId);
            if (t == null) return;

            // 1) Prebaci u DONE
            repo.updateTaskStatus(taskId, "DONE");

            // 2) Provera kvote
            int countToday = repo.countToday(t.getDifficulty(), t.getImportance());
            int countWeek = repo.countThisWeek(t.getDifficulty(), t.getImportance());
            int countMonth = repo.countThisMonth(t.getDifficulty(), t.getImportance());

            boolean canEarn = com.example.mobilneaplikacije.util.XPUtils
                    .canEarnXP(t.getDifficulty(), t.getImportance(), countToday, countWeek, countMonth);

            int awarded = canEarn ? t.getXpPoints() : 0;

            // 3) Saberi XP u SharedPreferences (profil)
            if (awarded > 0) {
                android.content.SharedPreferences prefs =
                        requireContext().getSharedPreferences("profile", android.content.Context.MODE_PRIVATE);
                int current = prefs.getInt("total_xp", 0);
                prefs.edit().putInt("total_xp", current + awarded).apply();
            }

            // 4) Loguj završetak (za buduća brojanja)
            repo.logCompletion(taskId, System.currentTimeMillis(), t.getDifficulty(), t.getImportance(), awarded);

            Toast.makeText(getContext(),
                    awarded > 0 ? ("Urađeno! +" + awarded + " XP") : "Urađeno! (bez XP, pređena kvota)",
                    Toast.LENGTH_LONG).show();

            loadTaskDetails();
        });


        btnPause.setOnClickListener(v -> {
            repo.updateTaskStatus(taskId, "PAUSED");
            Toast.makeText(getContext(), "Zadatak pauziran", Toast.LENGTH_SHORT).show();
            loadTaskDetails();
        });

        btnCancel.setOnClickListener(v -> {
            repo.updateTaskStatus(taskId, "CANCELLED");
            Toast.makeText(getContext(), "Zadatak otkazan", Toast.LENGTH_SHORT).show();
            loadTaskDetails();
        });

        btnDelete.setOnClickListener(v -> {
            repo.deleteTask(taskId);
            Toast.makeText(getContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        });

        return view;
    }

    private void loadTaskDetails() {
        TaskRepository repo = new TaskRepository(requireContext());
        for (Task t : repo.getAllTasks()) {
            if (t.getId() == taskId) {
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
}

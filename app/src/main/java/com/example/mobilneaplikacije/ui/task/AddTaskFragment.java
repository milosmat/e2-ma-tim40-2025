package com.example.mobilneaplikacije.ui.task;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.example.mobilneaplikacije.util.XPUtils;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AddTaskFragment extends Fragment {

    private static final String ARG_TASK_ID = "task_id";
    private long editTaskId = -1; // ako je -1 → novi zadatak

    private EditText etTitle, etDescription, etRepeatInterval;
    private Spinner spCategory, spDifficulty, spImportance, spRepeatUnit;
    private SwitchMaterial switchRecurring;
    private Button btnPickDate, btnPickTime, btnSave, btnPickEndDate;
    private long pickedDateTime = 0; // millis
    private long pickedEndDate = 0;

    public AddTaskFragment() { }

    // Factory metoda za otvaranje u "edit" modu
    public static AddTaskFragment newInstanceForEdit(long taskId) {
        AddTaskFragment fragment = new AddTaskFragment();
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
        View view = inflater.inflate(R.layout.fragment_add_task, container, false);

        // UI
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        spCategory = view.findViewById(R.id.spCategory);
        spDifficulty = view.findViewById(R.id.spDifficulty);
        spImportance = view.findViewById(R.id.spImportance);
        switchRecurring = view.findViewById(R.id.switchRecurring);
        etRepeatInterval = view.findViewById(R.id.etRepeatInterval);
        spRepeatUnit = view.findViewById(R.id.spRepeatUnit);
        btnPickDate = view.findViewById(R.id.btnPickDate);
        btnPickEndDate = view.findViewById(R.id.btnPickEndDate);
        btnPickTime = view.findViewById(R.id.btnPickTime);
        btnSave = view.findViewById(R.id.btnSave);

        // Spinner podaci
        List<String> categories = Arrays.asList("Zdravlje", "Učenje", "Zabava", "Sređivanje");
        List<String> difficulties = Arrays.asList("VEOMA_LAK", "LAK", "TEZAK", "EKSTREMNO_TEZAK");
        List<String> importances = Arrays.asList("NORMALAN", "VAŽAN", "EKSTREMNO_VAŽAN", "SPECIJALAN");
        List<String> repeatUnits = Arrays.asList("DAY", "WEEK");

        spCategory.setAdapter(makeAdapter(categories));
        spDifficulty.setAdapter(makeAdapter(difficulties));
        spImportance.setAdapter(makeAdapter(importances));
        spRepeatUnit.setAdapter(makeAdapter(repeatUnits));

        // Ako je došao task_id → edit mod
        if (getArguments() != null && getArguments().containsKey(ARG_TASK_ID)) {
            editTaskId = getArguments().getLong(ARG_TASK_ID);
            loadTaskForEdit(editTaskId);
        }

        LinearLayout layoutRecurring = view.findViewById(R.id.layoutRecurring);
        switchRecurring.setOnCheckedChangeListener((btn, checked) ->
                layoutRecurring.setVisibility(checked ? View.VISIBLE : View.GONE));

        // Date picker
        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                    (view1, year, month, day) -> {
                        calendar.set(year, month, day);
                        pickedDateTime = calendar.getTimeInMillis();
                        Toast.makeText(getContext(), "Datum: " + day + "." + (month + 1) + "." + year, Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnPickEndDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                    (view1, year, month, day) -> {
                        calendar.set(year, month, day);
                        pickedEndDate = calendar.getTimeInMillis();
                        Toast.makeText(getContext(),
                                "Završetak: " + day + "." + (month + 1) + "." + year,
                                Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Time picker
        btnPickTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                    (view12, hour, minute) -> {
                        calendar.setTimeInMillis(pickedDateTime);
                        calendar.set(Calendar.HOUR_OF_DAY, hour);
                        calendar.set(Calendar.MINUTE, minute);
                        pickedDateTime = calendar.getTimeInMillis();
                        Toast.makeText(getContext(), "Vreme: " + hour + ":" + minute, Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });

        // Save
        btnSave.setOnClickListener(v -> saveTask());

        return view;
    }

    private ArrayAdapter<String> makeAdapter(List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void loadTaskForEdit(long taskId) {
        TaskRepository repo = new TaskRepository(requireContext());
        for (Task t : repo.getAllTasks()) {
            if (t.getId() == taskId) {
                etTitle.setText(t.getTitle());
                etDescription.setText(t.getDescription());
                setSpinnerSelection(spDifficulty, t.getDifficulty());
                setSpinnerSelection(spImportance, t.getImportance());
                setSpinnerSelection(spCategory, String.valueOf(t.getCategoryId()));
                if (t.isRecurring()) {
                    switchRecurring.setChecked(true);
                    etRepeatInterval.setText(String.valueOf(t.getRepeatInterval()));
                    setSpinnerSelection(spRepeatUnit, t.getRepeatUnit());
                }
                pickedDateTime = t.getDueDateTime();
            }
        }
    }

    private void saveTask() {
        if (!validateInputs()) return;
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String difficulty = spDifficulty.getSelectedItem().toString();
        String importance = spImportance.getSelectedItem().toString();

        boolean isRecurring = switchRecurring.isChecked();
        int repeatInterval = 0;
        String repeatUnit = null;
        long startDate = 0;
        long endDate = 0;

        if (isRecurring) {
            String intervalText = etRepeatInterval.getText().toString().trim();
            if (intervalText.isEmpty()) {
                Toast.makeText(getContext(), "Unesi interval ponavljanja!", Toast.LENGTH_SHORT).show();
                return; // prekini save dok user ne unese
            }
            repeatInterval = Integer.parseInt(intervalText);

            repeatUnit = spRepeatUnit.getSelectedItem().toString();
            if (pickedDateTime == 0 || pickedEndDate == 0) {
                Toast.makeText(getContext(), "Izaberi datum početka i završetka!", Toast.LENGTH_SHORT).show();
                return;
            }
            startDate = pickedDateTime;
            endDate = pickedEndDate;
        }


        int xp = XPUtils.calculateXP(difficulty, importance);

        TaskRepository repo = new TaskRepository(requireContext());

        if (editTaskId == -1) {
            // Novi zadatak
            Task task = new Task();
            task.setTitle(title);
            task.setDescription(description);
            task.setDifficulty(difficulty);
            task.setImportance(importance);
            task.setXpPoints(xp);
            task.setDueDateTime(pickedDateTime);
            task.setStatus("ACTIVE");
            task.setRecurring(isRecurring);
            task.setRepeatInterval(repeatInterval);
            task.setRepeatUnit(repeatUnit);
            task.setStartDate(startDate);
            task.setEndDate(endDate);

            long id = repo.insertTask(task);
            if (id > 0) {
                Toast.makeText(getContext(), "Zadatak sačuvan!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update postojećeg
            for (Task t : repo.getAllTasks()) {
                if (t.getId() == editTaskId) {
                    t.setTitle(title);
                    t.setDescription(description);
                    t.setDifficulty(difficulty);
                    t.setImportance(importance);
                    t.setXpPoints(xp);
                    t.setDueDateTime(pickedDateTime);
                    t.setRecurring(isRecurring);
                    t.setRepeatInterval(repeatInterval);
                    t.setRepeatUnit(repeatUnit);
                    t.setStartDate(startDate);
                    t.setEndDate(endDate);

                    repo.updateTask(t);
                    Toast.makeText(getContext(), "Zadatak izmenjen!", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }

        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(value)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private boolean validateInputs() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Unesi naziv zadatka.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (pickedDateTime == 0) {
            Toast.makeText(getContext(), "Izaberi datum početka / due datum.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (switchRecurring.isChecked()) {
            String intervalText = etRepeatInterval.getText().toString().trim();
            if (intervalText.isEmpty()) {
                Toast.makeText(getContext(), "Unesi interval ponavljanja.", Toast.LENGTH_SHORT).show();
                return false;
            }
            int interval = 0;
            try {
                interval = Integer.parseInt(intervalText);
            } catch (NumberFormatException ignored) {}
            if (interval <= 0) {
                Toast.makeText(getContext(), "Interval mora biti pozitivan broj.", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (pickedEndDate == 0) {
                Toast.makeText(getContext(), "Izaberi datum završetka ponavljanja.", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (pickedEndDate < pickedDateTime) {
                Toast.makeText(getContext(), "Završetak ne može biti pre početka.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }
}

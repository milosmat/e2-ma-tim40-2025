package com.example.mobilneaplikacije.ui.task;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.CategoryRepository;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.example.mobilneaplikacije.util.XPUtils;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.*;

public class AddTaskFragment extends Fragment {

    private static final String ARG_TASK_ID_HASH = "task_id_hash";
    private String editTaskIdHash = null;

    private EditText etTitle, etDescription, etRepeatInterval;
    private Spinner spCategory, spDifficulty, spImportance, spRepeatUnit;
    private SwitchMaterial switchRecurring;
    private Button btnPickDate, btnPickTime, btnSave, btnPickEndDate;
    private long pickedDateTime = 0;
    private long pickedEndDate = 0;
    private List<Category> categories = new ArrayList<>();

    public AddTaskFragment() { }

    public static AddTaskFragment newInstanceForEdit(String taskIdHash) {
        AddTaskFragment fragment = new AddTaskFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_ID_HASH, taskIdHash);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_task, container, false);

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

        spDifficulty.setAdapter(makeAdapter(Arrays.asList("VEOMA_LAK","LAK","TEZAK","EKSTREMNO_TEZAK")));
        spImportance.setAdapter(makeAdapter(Arrays.asList("NORMALAN","VAZAN","EKSTREMNO_VAZAN","SPECIJALAN")));
        spRepeatUnit.setAdapter(makeAdapter(Arrays.asList("DAN","NEDELJA")));

        CategoryRepository catRepo = new CategoryRepository(requireContext());
        catRepo.getAllCategories(new CategoryRepository.Callback<List<Category>>() {
            @Override public void onSuccess(List<Category> data) {
                categories = data != null ? data : new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (Category c: categories) names.add(c.getName());
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, names);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCategory.setAdapter(catAdapter);

                if (getArguments() != null && getArguments().containsKey(ARG_TASK_ID_HASH)) {
                    editTaskIdHash = getArguments().getString(ARG_TASK_ID_HASH);
                    loadTaskForEdit(editTaskIdHash);
                }
            }
            @Override public void onError(Exception e) {
                Toast.makeText(getContext(),"Greška pri učitavanju kategorija",Toast.LENGTH_LONG).show();
            }
        });

        LinearLayout layoutRecurring = view.findViewById(R.id.layoutRecurring);
        switchRecurring.setOnCheckedChangeListener((btn, checked) ->
                layoutRecurring.setVisibility(checked ? View.VISIBLE : View.GONE));

        btnPickDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(requireContext(),
                    (view1, y, m, d) -> {
                        calendar.set(y, m, d);
                        pickedDateTime = calendar.getTimeInMillis();
                        Toast.makeText(getContext(), "Datum: " + d + "." + (m+1) + "." + y, Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnPickEndDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(requireContext(),
                    (view1, y, m, d) -> {
                        calendar.set(y, m, d);
                        pickedEndDate = calendar.getTimeInMillis();
                        Toast.makeText(getContext(), "Završetak: " + d + "." + (m+1) + "." + y, Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        btnPickTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog tp = new TimePickerDialog(requireContext(),
                    (view12, hour, minute) -> {
                        if (pickedDateTime == 0) pickedDateTime = System.currentTimeMillis();
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(pickedDateTime);
                        c.set(Calendar.HOUR_OF_DAY, hour);
                        c.set(Calendar.MINUTE, minute);
                        pickedDateTime = c.getTimeInMillis();
                        Toast.makeText(getContext(), "Vreme: " + hour + ":" + minute, Toast.LENGTH_SHORT).show();
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            tp.show();
        });

        btnSave.setOnClickListener(v -> saveTask());

        return view;
    }

    private ArrayAdapter<String> makeAdapter(List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void loadTaskForEdit(String taskIdHash) {
        TaskRepository repo = new TaskRepository(requireContext());
        repo.getTaskById(taskIdHash, new TaskRepository.Callback<Task>() {
            @Override public void onSuccess(Task t) {
                if (t == null) { Toast.makeText(getContext(),"Task ne postoji",Toast.LENGTH_SHORT).show(); return; }
                etTitle.setText(t.getTitle());
                etDescription.setText(t.getDescription());
                setSpinnerSelection(spDifficulty, t.getDifficulty());
                setSpinnerSelection(spImportance, t.getImportance());

                for (int i=0;i<categories.size();i++) {
                    if (categories.get(i).getIdHash().equals(t.getCategoryIdHash())) {
                        spCategory.setSelection(i); break;
                    }
                }
                if (t.isRecurring()) {
                    switchRecurring.setChecked(true);
                    etRepeatInterval.setText(String.valueOf(t.getRepeatInterval()));
                    setSpinnerSelection(spRepeatUnit, t.getRepeatUnit());
                    pickedDateTime = t.getStartDate();
                    pickedEndDate = t.getEndDate();
                } else {
                    pickedDateTime = t.getDueDateTime();
                }
            }
            @Override public void onError(Exception e) {
                Toast.makeText(getContext(),"Greška pri učitavanju zadatka",Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveTask() {
        if (!validateInputs()) return;

        int catIndex = spCategory.getSelectedItemPosition();
        if (catIndex < 0 || catIndex >= categories.size()) {
            Toast.makeText(getContext(), "Izaberi kategoriju", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String difficulty = spDifficulty.getSelectedItem().toString();
        String importance = spImportance.getSelectedItem().toString();

        boolean isRecurring = switchRecurring.isChecked();
        int repeatInterval = 0; String repeatUnit = null; long startDate = 0; long endDate = 0;

        if (isRecurring) {
            String intervalText = etRepeatInterval.getText().toString().trim();
            if (TextUtils.isEmpty(intervalText)) {
                Toast.makeText(getContext(), "Unesi interval ponavljanja!", Toast.LENGTH_SHORT).show();
                return;
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
        if (editTaskIdHash == null) {
            Task t = new Task();
            t.setTitle(title);
            t.setDescription(description);
            t.setDifficulty(difficulty);
            t.setImportance(importance);
            t.setXpPoints(xp);
            t.setStatus("ACTIVE");
            t.setRecurring(isRecurring);
            t.setRepeatInterval(repeatInterval);
            t.setRepeatUnit(repeatUnit);
            t.setStartDate(startDate);
            t.setEndDate(endDate);
            t.setDueDateTime(isRecurring ? 0 : pickedDateTime);
            t.setCategoryIdHash(categories.get(catIndex).getIdHash());

            repo.insertTask(t, new TaskRepository.Callback<String>() {
                @Override public void onSuccess(String idHash) {
                    Toast.makeText(getContext(),"Zadatak sačuvan!",Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
                @Override public void onError(Exception e) {
                    Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Task t = new Task();
            t.setIdHash(editTaskIdHash);
            t.setTitle(title);
            t.setDescription(description);
            t.setDifficulty(difficulty);
            t.setImportance(importance);
            t.setXpPoints(xp);
            t.setRecurring(isRecurring);
            t.setRepeatInterval(repeatInterval);
            t.setRepeatUnit(repeatUnit);
            t.setStartDate(startDate);
            t.setEndDate(endDate);
            t.setDueDateTime(isRecurring ? 0 : pickedDateTime);
            t.setCategoryIdHash(categories.get(catIndex).getIdHash());

            repo.updateTask(editTaskIdHash, t, new TaskRepository.Callback<Void>() {
                @Override public void onSuccess(Void v) {
                    Toast.makeText(getContext(),"Zadatak izmenjen!",Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
                @Override public void onError(Exception e) {
                    Toast.makeText(getContext(),"Greška: "+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) { spinner.setSelection(i); break; }
        }
    }

    private boolean validateInputs() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Unesi naziv zadatka.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!switchRecurring.isChecked() && pickedDateTime == 0) {
            Toast.makeText(getContext(), "Izaberi due datum/vreme.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (switchRecurring.isChecked()) {
            String intervalText = etRepeatInterval.getText().toString().trim();
            if (intervalText.isEmpty()) {
                Toast.makeText(getContext(), "Unesi interval ponavljanja.", Toast.LENGTH_SHORT).show();
                return false;
            }
            int interval;
            try { interval = Integer.parseInt(intervalText); } catch (NumberFormatException e) { interval = 0; }
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

package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.*;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.model.TaskOccurrence;
import com.example.mobilneaplikacije.data.repository.CategoryRepository;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.example.mobilneaplikacije.ui.dto.DayRowDTO;

import java.util.*;

public class TaskCalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView recyclerDayTasks;
    private DayTaskAdapter dayAdapter;

    private final Map<String, Category> categoryMap = new HashMap<>();
    private final List<Task> allTasks = new ArrayList<>();
    private TaskRepository taskRepo;

    public TaskCalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_calendar, container, false);
        calendarView = v.findViewById(R.id.calendarView);
        recyclerDayTasks = v.findViewById(R.id.recyclerDayTasks);
        recyclerDayTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        dayAdapter = new DayTaskAdapter(new ArrayList<>(), categoryMap, (task, occurrenceId) -> {
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(task.getIdHash(), occurrenceId);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerDayTasks.setAdapter(dayAdapter);

        taskRepo = new TaskRepository(requireContext());

        new CategoryRepository(requireContext()).getAllCategories(new CategoryRepository.Callback<List<Category>>() {
            @Override public void onSuccess(List<Category> data) {
                categoryMap.clear();
                if (data != null) for (Category c: data) categoryMap.put(c.getIdHash(), c);
                loadAllTasks();
            }
            @Override public void onError(Exception e) { loadAllTasks(); }
        });

        calendarView.setOnDateChangeListener((cv, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d, 0, 0, 0); c.set(Calendar.MILLISECOND,0);
            long startOfDay = c.getTimeInMillis();
            long endOfDay = startOfDay + 24L*60*60*1000 - 1;
            showForDay(startOfDay, endOfDay);
        });

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        showForDay(c.getTimeInMillis(), c.getTimeInMillis()+24L*60*60*1000-1);

        return v;
    }

    private void loadAllTasks() {
        taskRepo.getAllTasks(new TaskRepository.Callback<List<Task>>() {
            @Override public void onSuccess(List<Task> data) {
                allTasks.clear();
                if (data != null) allTasks.addAll(data);
                long day = calendarView.getDate();
                Calendar c = Calendar.getInstance(); c.setTimeInMillis(day);
                c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
                showForDay(c.getTimeInMillis(), c.getTimeInMillis()+24L*60*60*1000-1);
            }
            @Override public void onError(Exception e) { }
        });
    }

    private void showForDay(long startOfDay, long endOfDay) {
        List<DayRowDTO> rows = new ArrayList<>();

        for (Task t : allTasks) {
            if (!t.isRecurring()
                    && t.getDueDateTime() >= startOfDay
                    && t.getDueDateTime() <= endOfDay) {
                rows.add(new DayRowDTO(t, null));
            }
        }

        final int[] pending = {0};
        boolean anyRecurring = false;

        for (Task t: allTasks) {
            if (!t.isRecurring()) continue;
            anyRecurring = true;
            pending[0]++;

            taskRepo.getOccurrencesForRange(t.getIdHash(), startOfDay, endOfDay,
                    new TaskRepository.Callback<List<TaskOccurrence>>() {
                        @Override public void onSuccess(List<TaskOccurrence> occs) {
                            if (occs != null) {
                                for (TaskOccurrence o : occs) {
                                    rows.add(new DayRowDTO(t, o));
                                }
                            }
                            if (--pending[0] == 0) dayAdapter.updateData(rows);
                        }
                        @Override public void onError(Exception e) {
                            if (--pending[0] == 0) dayAdapter.updateData(rows);
                        }
                    });
        }

        if (!anyRecurring) {
            dayAdapter.updateData(rows);
        }
    }
}

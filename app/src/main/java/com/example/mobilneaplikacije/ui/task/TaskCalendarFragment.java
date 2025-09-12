package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.example.mobilneaplikacije.util.TaskUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskCalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView recyclerDayTasks;
    private DayTaskAdapter dayAdapter;
    private List<Task> allTasks;

    public TaskCalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        recyclerDayTasks = view.findViewById(R.id.recyclerDayTasks);
        recyclerDayTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        dayAdapter = new DayTaskAdapter(new ArrayList<>(), task -> {
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(task.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerDayTasks.setAdapter(dayAdapter);

        TaskRepository repo = new TaskRepository(requireContext());
        allTasks = repo.getAllTasks();

        // Kada korisnik promeni datum u kalendaru
        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            long selectedMillis = new Date(year - 1900, month, dayOfMonth).getTime();
            long startOfDay = selectedMillis;
            long endOfDay = selectedMillis + 24L * 60 * 60 * 1000 - 1;

            List<Task> tasksForDay = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (Task t : allTasks) {
                if (t.isRecurring()) {
                    List<Long> occ = TaskUtils.generateOccurrencesBetween(t, startOfDay, endOfDay, 100);
                    if (!occ.isEmpty()) {
                        tasksForDay.add(t);
                    }
                } else {
                    if (isSameDay(t.getDueDateTime(), selectedMillis)) {
                        tasksForDay.add(t);
                    }
                }
            }

            dayAdapter.updateData(tasksForDay);
        });

        return view;
    }

    private boolean isSameDay(long millis1, long millis2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date(millis1)).equals(sdf.format(new Date(millis2)));
    }
}

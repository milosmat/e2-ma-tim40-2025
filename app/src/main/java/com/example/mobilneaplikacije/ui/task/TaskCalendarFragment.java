package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.example.mobilneaplikacije.util.TaskUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskCalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedTasks;

    public TaskCalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedTasks = view.findViewById(R.id.tvSelectedTasks);

        TaskRepository repo = new TaskRepository(requireContext());
        List<Task> tasks = repo.getAllTasks();

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            long selectedMillis = new Date(year - 1900, month, dayOfMonth).getTime();
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (Task t : tasks) {
                if (t.isRecurring()) {
                    long startOfDay = selectedMillis;
                    long endOfDay = selectedMillis + 24L * 60 * 60 * 1000 - 1;

                    List<Long> occ = TaskUtils.generateOccurrencesBetween(t, startOfDay, endOfDay, 100);
                    for (Long occurrence : occ) {
                        sb.append("• ").append(t.getTitle())
                                .append(" (").append(sdf.format(new Date(occurrence))).append(")\n");
                    }
                } else {
                    if (isSameDay(t.getDueDateTime(), selectedMillis)) {
                        sb.append("• ").append(t.getTitle())
                                .append(" (").append(sdf.format(new Date(t.getDueDateTime()))).append(")\n");
                    }
                }
            }

            if (sb.length() == 0) {
                sb.append("Nema zadataka za ovaj datum.");
            }

            tvSelectedTasks.setText(sb.toString());
        });

        return view;
    }

    private boolean isSameDay(long millis1, long millis2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date(millis1)).equals(sdf.format(new Date(millis2)));
    }
}

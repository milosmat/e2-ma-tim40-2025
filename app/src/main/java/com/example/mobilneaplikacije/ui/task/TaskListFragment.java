package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

import java.util.List;

import com.google.android.material.tabs.TabLayout;
import java.util.stream.Collectors;

public class TaskListFragment extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter adapter;
    private List<Task> allTasks;

    public TaskListFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        TaskRepository repo = new TaskRepository(requireContext());
        allTasks = repo.getAllTasks();
        long now = System.currentTimeMillis();
        allTasks = repo.getAllTasks()
                .stream()
                .filter(t -> hasFutureOccurrence(t, now))
                .collect(Collectors.toList());

        adapter = new TaskAdapter(allTasks, task -> {
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(task.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerTasks.setAdapter(adapter);

        // Dodaj tabove
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Svi"));
        tabLayout.addTab(tabLayout.newTab().setText("Jednokratni"));
        tabLayout.addTab(tabLayout.newTab().setText("Ponavljajući"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterTasks(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private boolean hasFutureOccurrence(Task t, long now) {
        if (!t.isRecurring()) return t.getDueDateTime() >= now;

        // ima budućih pojavljivanja ako endDate >= now
        if (t.getEndDate() < now) return false;

        // opcionalno: preciznije – tražimo prvu pojavu u [now, now + 1 god]
        long year = 365L*24*60*60*1000;
        return !com.example.mobilneaplikacije.util.TaskUtils
                .generateOccurrencesBetween(t, now, now + year, 1).isEmpty();
    }
    private void filterTasks(int tabIndex) {
        List<Task> filtered;
        if (tabIndex == 1) {
            filtered = allTasks.stream()
                    .filter(t -> !t.isRecurring())
                    .collect(Collectors.toList());
        } else if (tabIndex == 2) {
            filtered = allTasks.stream()
                    .filter(Task::isRecurring)
                    .collect(Collectors.toList());
        } else {
            filtered = allTasks;
        }
        adapter.updateData(filtered);
    }
}

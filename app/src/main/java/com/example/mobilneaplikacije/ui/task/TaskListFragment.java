package com.example.mobilneaplikacije.ui.task;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.repository.CategoryRepository;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.google.android.material.tabs.TabLayout;

import java.util.*;
import java.util.stream.Collectors;

public class TaskListFragment extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter adapter;
    private final List<Task> allTasks = new ArrayList<>();
    private final Map<String,Category> categoryMap = new HashMap<>();

    public TaskListFragment() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_task_list, container, false);

        recyclerTasks = v.findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TaskAdapter(new ArrayList<>(), categoryMap, task -> {
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(task.getIdHash(), null);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerTasks.setAdapter(adapter);

        TabLayout tabLayout = v.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Svi"));
        tabLayout.addTab(tabLayout.newTab().setText("Jednokratni"));
        tabLayout.addTab(tabLayout.newTab().setText("Ponavljajući"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterTasks(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Učitaj kategorije pa taskove
        new CategoryRepository(requireContext()).getAllCategories(new CategoryRepository.Callback<List<Category>>() {
            @Override public void onSuccess(List<Category> data) {
                categoryMap.clear();
                if (data != null) for (Category c: data) categoryMap.put(c.getIdHash(), c);
                loadTasks();
            }
            @Override public void onError(Exception e) { loadTasks(); }
        });

        return v;
    }

    private void loadTasks() {
        new TaskRepository(requireContext()).getAllTasks(new TaskRepository.Callback<List<Task>>() {
            @Override public void onSuccess(List<Task> data) {
                allTasks.clear();
                if (data != null) {
                    long now = System.currentTimeMillis();
                    allTasks.addAll(
                            data.stream().filter(t -> hasFutureOccurrence(t, now)).collect(Collectors.toList())
                    );
                }
                filterTasks(0);
            }
            @Override public void onError(Exception e) {
                Toast.makeText(getContext(),"Greška pri učitavanju: "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean hasFutureOccurrence(Task t, long now) {
        if (!t.isRecurring()) return t.getDueDateTime() >= now;
        return t.getEndDate() >= now;
    }

    private void filterTasks(int tabIndex) {
        List<Task> filtered;
        if (tabIndex == 1) {
            filtered = allTasks.stream().filter(t -> !t.isRecurring()).collect(Collectors.toList());
        } else if (tabIndex == 2) {
            filtered = allTasks.stream().filter(Task::isRecurring).collect(Collectors.toList());
        } else {
            filtered = allTasks;
        }
        adapter.updateData(filtered);
    }
}

package com.example.mobilneaplikacije.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.model.Task;

import java.text.SimpleDateFormat;
import java.util.*;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener { void onTaskClick(Task task); }

    private List<Task> tasks;
    private final OnTaskClickListener listener;
    private final Map<String, Category> categoryMap;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public TaskAdapter(List<Task> tasks, Map<String,Category> categoryMap, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
        this.categoryMap = categoryMap != null ? categoryMap : new HashMap<>();
    }

    public void updateData(List<Task> newTasks) {
        this.tasks = newTasks != null ? newTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(tasks.get(position), categoryMap, listener, dateFmt);
    }
    @Override public int getItemCount() { return tasks.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvStatus, tvXP;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvXP = itemView.findViewById(R.id.tvXP);
        }

        void bind(Task task, Map<String,Category> catMap, OnTaskClickListener listener, SimpleDateFormat df) {
            tvTitle.setText(task.getTitle());

            Category cat = catMap.get(task.getCategoryIdHash());
            if (cat != null) {
                tvCategory.setText("Kategorija: " + cat.getName());
                try {
                    int color = android.graphics.Color.parseColor(cat.getColor());
                    tvCategory.setTextColor(color);
                } catch (Exception ignored) { tvCategory.setTextColor(android.graphics.Color.BLACK); }
            } else {
                tvCategory.setText("Kategorija: -");
                tvCategory.setTextColor(android.graphics.Color.BLACK);
            }

            tvStatus.setText("Status: " + task.getStatus());
            tvXP.setText("XP: " + task.getXpPoints());

            if (task.isRecurring()) {
                tvStatus.append("\nPonavlja se svaka " + task.getRepeatInterval() + " " + task.getRepeatUnit()
                        + " do " + new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        .format(new java.util.Date(task.getEndDate())));
            } else {
                tvStatus.append("\nRok: " + df.format(new java.util.Date(task.getDueDateTime())));
            }

            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}

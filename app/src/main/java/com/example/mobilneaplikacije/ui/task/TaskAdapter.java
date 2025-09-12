package com.example.mobilneaplikacije.ui.task;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    // 🔹 DODAJ OVO
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged(); // obavesti RecyclerView da ponovo nacrta listu
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvStatus, tvXP;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvXP = itemView.findViewById(R.id.tvXP);
        }

        public void bind(Task task, OnTaskClickListener listener) {
            tvTitle.setText(task.getTitle());
            tvCategory.setText("Kategorija: " + task.getCategoryId());
            tvStatus.setText("Status: " + task.getStatus());
            tvXP.setText("XP: " + task.getXpPoints());

            if (task.isRecurring()) {
                tvStatus.append("\nPonavlja se svaka " + task.getRepeatInterval() + " "
                        + task.getRepeatUnit() + " do "
                        + new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                        .format(new java.util.Date(task.getEndDate())));
            } else {
                tvStatus.append("\nRok: " +
                        new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                                .format(new java.util.Date(task.getDueDateTime())));
            }

            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}

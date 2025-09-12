package com.example.mobilneaplikacije.ui.task;

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
            String extra = "";
            tvTitle.setText(task.getTitle());
            tvCategory.setText("Kategorija: " + task.getCategoryId());
            tvStatus.setText("Status: " + task.getStatus());
            tvXP.setText("XP: " + task.getXpPoints());
            if (task.isRecurring()) {
                String unit = "DAY".equals(task.getRepeatUnit()) ? "dan" : "nedelja";
                extra = " • Ponavlja se na " + task.getRepeatInterval() + " " + unit +
                        " do " + new java.text.SimpleDateFormat("dd.MM.yyyy")
                        .format(new java.util.Date(task.getEndDate()));
            } else if (task.getDueDateTime() > 0) {
                extra = " • " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                        .format(new java.util.Date(task.getDueDateTime()));
            }
            tvTitle.setText(task.getTitle() + extra);
            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}

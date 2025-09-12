package com.example.mobilneaplikacije.ui.task;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Task;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayTaskAdapter extends RecyclerView.Adapter<DayTaskAdapter.DayTaskViewHolder> {

    private List<Task> tasks;
    private final TaskAdapter.OnTaskClickListener listener;

    public DayTaskAdapter(List<Task> tasks, TaskAdapter.OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_task, parent, false);
        return new DayTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayTaskViewHolder holder, int position) {
        holder.bind(tasks.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateData(List<Task> newTasks) {
        tasks = newTasks;
        notifyDataSetChanged();
    }

    static class DayTaskViewHolder extends RecyclerView.ViewHolder {
        View viewColor;
        TextView tvTitle, tvTime;
        final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        DayTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColor = itemView.findViewById(R.id.viewColor);
            tvTitle   = itemView.findViewById(R.id.tvTitle);
            tvTime    = itemView.findViewById(R.id.tvTime);
        }

        void bind(Task task, TaskAdapter.OnTaskClickListener listener) {
            tvTitle.setText(task.getTitle());

            // vreme: za ponavljajući prikazujemo startTime tog dana (TaskCalendarFragment već filtrira po danu)
            long when = task.isRecurring() ? task.getStartDate() : task.getDueDateTime();
            tvTime.setText(timeFmt.format(new Date(when)));

            // boja kategorije (pretpostavljamo da je u Task.categoryColor ili preko helpera)
            //String colorHex = task.getCategoryColor(); // ako nemaš polje, vidi helper dole
            //try { viewColor.setBackgroundColor(Color.parseColor(colorHex)); }
            //catch (Exception ignored) { viewColor.setBackgroundColor(0xFFBBBBBB); }

            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}

package com.example.mobilneaplikacije.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.model.TaskOccurrence;
import com.example.mobilneaplikacije.ui.dto.DayRowDTO;

import java.text.SimpleDateFormat;
import java.util.*;

public class DayTaskAdapter extends RecyclerView.Adapter<DayTaskAdapter.DayTaskViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(Task task, @Nullable String occurrenceId);
    }

    private List<DayRowDTO> rows;
    private final OnDayClickListener listener;
    private final Map<String, Category> categoryMap;

    public DayTaskAdapter(List<DayRowDTO> rows, Map<String,Category> categoryMap, OnDayClickListener listener) {
        this.rows = rows != null ? rows : new ArrayList<>();
        this.listener = listener;
        this.categoryMap = categoryMap != null ? categoryMap : new HashMap<>();
    }

    @NonNull @Override
    public DayTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_task, parent, false);
        return new DayTaskViewHolder(v);
    }

    @Override public void onBindViewHolder(@NonNull DayTaskViewHolder h, int pos) {
        h.bind(rows.get(pos), categoryMap, listener);
    }

    @Override public int getItemCount() { return rows.size(); }

    public void updateData(List<DayRowDTO> newRows) {
        rows = newRows != null ? newRows : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class DayTaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

        DayTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime  = itemView.findViewById(R.id.tvTime);
        }

        void bind(DayRowDTO row,
                  Map<String,Category> catMap,
                  OnDayClickListener listener) {

            Task task = row.task;
            TaskOccurrence occ = row.occ;

            tvTitle.setText(task.getTitle());

            long when = (occ != null && occ.dueDateTime > 0)
                    ? occ.dueDateTime
                    : (task.isRecurring() ? task.getStartDate() : task.getDueDateTime());
            tvTime.setText(timeFmt.format(new Date(when)));

            Category cat = catMap.get(task.getCategoryIdHash());
            if (cat != null) {
                try { tvTitle.setTextColor(android.graphics.Color.parseColor(cat.getColor())); }
                catch (Exception e) { tvTitle.setTextColor(android.graphics.Color.BLACK); }
            } else {
                tvTitle.setTextColor(android.graphics.Color.BLACK);
            }

            itemView.setOnClickListener(v ->
                    listener.onDayClick(task, occ == null ? null : occ.id));
        }
    }
}

package com.example.mobilneaplikacije.ui.equipment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Equipment;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

    public interface OnActivateClickListener {
        void onActivate(Equipment equipment);
    }

    private List<Equipment> equipmentList;
    private OnActivateClickListener listener;

    public EquipmentAdapter(List<Equipment> equipmentList, OnActivateClickListener listener) {
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment item = equipmentList.get(position);

        holder.tvName.setText(item.getName());

        if (item.isActive()) {
            holder.btnActivate.setEnabled(false);
            holder.btnActivate.setText("Aktiviran");
            holder.itemView.setAlpha(0.5f); // zasivljen card
        } else {
            holder.btnActivate.setEnabled(true);
            holder.btnActivate.setText("Aktiviraj");
            holder.itemView.setAlpha(1f);
        }

        holder.btnActivate.setOnClickListener(v -> {
            if (!item.isActive()) {
                listener.onActivate(item);
                item.setActive(true);
                notifyItemChanged(position); // refresh kartice
            }
        });
    }


    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvEffect, tvDuration;
        private Button btnActivate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEffect = itemView.findViewById(R.id.tvEffect);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnActivate = itemView.findViewById(R.id.btnActivate);
        }

        public void bind(final Equipment equipment, final OnActivateClickListener listener) {
            tvName.setText(equipment.getName());
            tvEffect.setText("Efekat: " + equipment.getEffect().toString());
            tvDuration.setText(equipment.getDuration() == -1 ? "Trajno" : ("Traje " + equipment.getDuration() + " borbi"));

            btnActivate.setOnClickListener(v -> {
                listener.onActivate(equipment);
            });
        }
    }
}

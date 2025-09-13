package com.example.mobilneaplikacije.ui.category;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_CATEGORY = 1;

    private List<Category> categories;
    private OnCategoryClickListener listener;
    private Runnable onAddClick; // callback za dodavanje

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener, Runnable onAddClick) {
        this.categories = categories;
        this.listener = listener;
        this.onAddClick = onAddClick;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ADD : TYPE_CATEGORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_category, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            ((AddViewHolder) holder).bind(onAddClick);
        } else {
            Category category = categories.get(position - 1); // -1 jer je prvi "Add"
            ((CategoryViewHolder) holder).bind(category, listener);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size() + 1; // +1 za "Dodaj novu kategoriju"
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        public void bind(Runnable onAddClick) {
            itemView.setOnClickListener(v -> onAddClick.run());
        }
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvColor;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvColor = itemView.findViewById(R.id.tvCategoryColor);
        }

        public void bind(Category category, OnCategoryClickListener listener) {
            tvName.setText(category.getName());
            tvColor.setText(category.getColor());
            try {
                tvColor.setBackgroundColor(Color.parseColor(category.getColor()));
            } catch (Exception ignored) {}

            itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }
    }

    public void updateData(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }
}


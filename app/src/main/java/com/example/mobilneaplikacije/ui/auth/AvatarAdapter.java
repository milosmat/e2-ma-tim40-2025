package com.example.mobilneaplikacije.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final int[] avatarIds = {
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5
    };

    private int selectedPosition = RecyclerView.NO_POSITION;
    private final OnAvatarClickListener listener;

    public interface OnAvatarClickListener {
        void onAvatarSelected(String avatarName);
    }

    public AvatarAdapter(OnAvatarClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        holder.img.setImageResource(avatarIds[position]);
        holder.itemView.setSelected(selectedPosition == position);

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            String avatarName = "avatar_" + (position + 1);
            listener.onAvatarSelected(avatarName);
        });
    }

    @Override
    public int getItemCount() {
        return avatarIds.length;
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView img;
        AvatarViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.imgAvatar);
        }
    }
}

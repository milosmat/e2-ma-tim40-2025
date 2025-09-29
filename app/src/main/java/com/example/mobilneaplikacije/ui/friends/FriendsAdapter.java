package com.example.mobilneaplikacije.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.UserPublic;
import com.example.mobilneaplikacije.ui.profile.PublicProfileFragment;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_USER = 1;

    public interface ActionHandler {
        void onAccept(UserPublic u);
        void onAdd(UserPublic u);
    }

    private final List<Object> items = new ArrayList<>();
    private final ActionHandler handler;

    public FriendsAdapter(ActionHandler handler) {
        this.handler = handler;
    }

    public void setData(List<UserPublic> incoming, List<UserPublic> friends, List<UserPublic> results) {
        items.clear();
        if (!incoming.isEmpty()) {
            items.add("Zahtevi za prijateljstvo");
            items.addAll(incoming);
        }
        if (!friends.isEmpty()) {
            items.add("Prijatelji");
            items.addAll(friends);
        }
        if (!results.isEmpty()) {
            items.add("Rezultati pretrage");
            items.addAll(results);
        }
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_USER;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inf.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.row_user, parent, false);
            return new UserVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind((String) items.get(position));
        } else if (holder instanceof UserVH) {
            ((UserVH) holder).bind((UserPublic) items.get(position), handler);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tv;
        HeaderVH(@NonNull View itemView) { super(itemView); tv = itemView.findViewById(android.R.id.text1); }
        void bind(String title) { tv.setText(title); }
    }

    static class UserVH extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tvName, tvInfo;
        Button btn;
        UserVH(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.ivUserRow);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            btn = itemView.findViewById(R.id.btnAddFriend);
        }
        void bind(UserPublic u, ActionHandler handler) {
            tvName.setText(u.username);
            StringBuilder subtitle = new StringBuilder();
            if (u.level != null) subtitle.append("Nivo: ").append(u.level).append("  ");
            if (u.title != null) subtitle.append("Titula: ").append(u.title).append("  ");
            if (u.pp != null) subtitle.append("PP: ").append(u.pp).append("  ");
            if (u.coins != null) subtitle.append("Novcici: ").append(u.coins);
            tvInfo.setText(subtitle.toString().trim());
            int avatarRes = itemView.getResources().getIdentifier(u.avatar == null ? "ic_person" : u.avatar, "drawable", itemView.getContext().getPackageName());
            if (avatarRes != 0) iv.setImageResource(avatarRes);

            itemView.setOnClickListener(v -> {
                androidx.fragment.app.FragmentActivity act = (androidx.fragment.app.FragmentActivity) v.getContext();
                try {
                    PublicProfileFragment f = PublicProfileFragment.newInstance(u.uid);
                    act.getSupportFragmentManager().beginTransaction()
                            .replace(com.example.mobilneaplikacije.R.id.fragment_container, f)
                            .addToBackStack(null)
                            .commit();
                } catch (Exception ignored) {}
            });

            if ("friend".equals(u.status)) {
                btn.setVisibility(View.GONE);
            } else if ("incoming".equals(u.status)) {
                btn.setVisibility(View.VISIBLE);
                btn.setText(R.string.accept);
                btn.setEnabled(true);
                btn.setOnClickListener(v -> handler.onAccept(u));
            } else if ("outgoing".equals(u.status)) {
                btn.setVisibility(View.VISIBLE);
                btn.setText(R.string.request_sent);
                btn.setEnabled(false);
                btn.setOnClickListener(null);
            } else {
                btn.setVisibility(View.VISIBLE);
                btn.setText(R.string.add_friend);
                btn.setEnabled(true);
                btn.setOnClickListener(v -> handler.onAdd(u));
            }
        }
    }
}

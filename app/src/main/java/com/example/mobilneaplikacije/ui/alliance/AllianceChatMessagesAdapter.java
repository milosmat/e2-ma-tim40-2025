package com.example.mobilneaplikacije.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.AllianceMessage;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class AllianceChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;
    private final List<AllianceMessage> items;
    private String myUid;

    public AllianceChatMessagesAdapter(List<AllianceMessage> init) {
    this.items = init != null ? init : new ArrayList<>(); }
    public void setMyUid(String uid) { this.myUid = uid; notifyDataSetChanged(); }
    public void setItems(List<AllianceMessage> data) { items.clear(); if (data != null) items.addAll(data);
        notifyDataSetChanged(); }

    @Override public int getItemCount() { return items.size(); }

    @Override public int getItemViewType(int position) {
        AllianceMessage m = items.get(position);
        return (myUid != null && myUid.equals(m.senderUid)) ? TYPE_ME : TYPE_OTHER;
    }
    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            View v = inf.inflate(R.layout.row_message_right, parent, false);
            return new VH(v);
        } else {
            View v = inf.inflate(R.layout.row_message_left, parent, false);
            return new VH(v);
        }
    }
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VH vh = (VH) holder;
        AllianceMessage m = items.get(position);
        vh.tvUser.setText(m.senderUsername != null ? m.senderUsername : m.senderUid);
        vh.tvText.setText(m.text);
        String ts = m.createdAt != null ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(m.createdAt) : "";
        vh.tvTime.setText(ts);
    }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvUser, tvText, tvTime;
        VH(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvText = itemView.findViewById(R.id.tvText);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}

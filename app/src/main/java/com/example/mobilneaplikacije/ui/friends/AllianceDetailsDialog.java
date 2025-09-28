package com.example.mobilneaplikacije.ui.friends;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Alliance;
import com.example.mobilneaplikacije.data.model.UserPublic;
import com.example.mobilneaplikacije.data.repository.AllianceRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AllianceDetailsDialog extends DialogFragment {
    private final String allianceId;
    private AllianceRepository allianceRepo;

    public AllianceDetailsDialog(String allianceId) { this.allianceId = allianceId; }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_alliance_details, null, false);
        TextView tvName = v.findViewById(R.id.tvAllianceName);
        TextView tvLeader = v.findViewById(R.id.tvAllianceLeader);
        ListView lvMembers = v.findViewById(R.id.lvMembers);
        TextView tvEmpty = v.findViewById(R.id.tvEmpty);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        lvMembers.setAdapter(adapter);
        lvMembers.setEmptyView(tvEmpty);

        allianceRepo = new AllianceRepository();
        allianceRepo.getAlliance(allianceId, new AllianceRepository.Callback<Alliance>() {
            @Override public void onSuccess(Alliance al) {
                if (al == null) return;
                tvName.setText(al.name != null ? al.name : "Savez");
                tvLeader.setTag(al.leaderUid);
            }
            @Override public void onError(Exception e) {
                tvName.setText("Greska");
                tvLeader.setText("");
            }
        });

        allianceRepo.getMembers(allianceId, new AllianceRepository.Callback<List<UserPublic>>() {
            @Override public void onSuccess(List<UserPublic> members) {
                List<String> names = new ArrayList<>();
                String leaderUid = (String) tvLeader.getTag();
                String leaderName = null;
                if (members != null) {
                    for (UserPublic u : members) {
                        if (u == null) continue;
                        if (leaderUid != null && leaderUid.equals(u.uid)) leaderName = u.username;
                        names.add(u.username != null ? u.username : u.uid);
                    }
                }
                if (leaderName != null) tvLeader.setText("Vlasnik: " + leaderName);
                else if (leaderUid != null) tvLeader.setText("Vlasnik: " + leaderUid);
                adapter.clear();
                adapter.addAll(names);
                adapter.notifyDataSetChanged();
            }
            @Override public void onError(Exception e) {  }
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(v)
                .setPositiveButton("Zatvori", (d,w) -> dismiss())
                .create();
    }
}
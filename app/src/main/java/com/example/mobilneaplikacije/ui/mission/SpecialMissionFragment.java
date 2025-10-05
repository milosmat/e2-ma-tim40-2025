package com.example.mobilneaplikacije.ui.mission;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.repository.AllianceRepository;
import com.example.mobilneaplikacije.data.repository.SpecialMissionRepository;

public class SpecialMissionFragment extends Fragment {

    private TextView tvStatus, tvHp, tvTime;
    private android.widget.ImageView ivBoss;
    private ProgressBar pb;
    private Button btnStart;
    private RecyclerView rvProgress;
    private String allianceId;
    private boolean isLeader;
    private final SpecialMissionRepository repo = new SpecialMissionRepository();
    private final AllianceRepository allianceRepo = new AllianceRepository();
    private ProgressAdapter adapter;
    private java.util.Map<String, String> nameByUid = new java.util.HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_special_mission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    tvStatus = view.findViewById(R.id.tvMissionStatus);
        tvHp = view.findViewById(R.id.tvMissionHp);
        tvTime = view.findViewById(R.id.tvMissionTime);
        pb = view.findViewById(R.id.pbMission);
        btnStart = view.findViewById(R.id.btnStartMission);
    ivBoss = view.findViewById(R.id.ivSpecialBoss);
        rvProgress = view.findViewById(R.id.rvMissionProgress);
        rvProgress.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProgressAdapter();
        rvProgress.setAdapter(adapter);

        allianceRepo.getMyAllianceInfo(new AllianceRepository.Callback<AllianceRepository.AllianceInfo>() {
            @Override public void onSuccess(AllianceRepository.AllianceInfo info) {
                allianceId = info != null ? info.allianceId : null;
                isLeader = info != null && info.isLeader;
                btnStart.setVisibility(isLeader ? View.VISIBLE : View.GONE);
                if (allianceId != null) {
                    allianceRepo.getMembers(allianceId, new AllianceRepository.Callback<java.util.List<com.example.mobilneaplikacije.data.model.UserPublic>>() {
                        @Override public void onSuccess(java.util.List<com.example.mobilneaplikacije.data.model.UserPublic> members) {
                            java.util.HashMap<String, String> map = new java.util.HashMap<>();
                            for (com.example.mobilneaplikacije.data.model.UserPublic u : members) {
                                if (u != null && u.uid != null) map.put(u.uid, u.username != null ? u.username : "(nepoznat)");
                            }
                            nameByUid = map;
                            if (adapter != null) adapter.setNameMap(map);
                            refresh();
                        }
                        @Override public void onError(Exception e) {
                            refresh();
                        }
                    });
                } else {
                    refresh();
                }
            }
            @Override public void onError(Exception e) { toast(e.getMessage()); }
        });

        btnStart.setOnClickListener(v -> {
            if (allianceId == null) return;
            repo.startMission(allianceId, new SpecialMissionRepository.Callback<Void>() {
                @Override public void onSuccess(Void data) { toast("Specijalna misija pokrenuta"); refresh(); }
                @Override public void onError(Exception e) { toast(e.getMessage()); }
            });
        });
    }

    private void refresh() {
        if (allianceId == null) {
            tvStatus.setText("Niste u savezu");
            if (btnStart != null) btnStart.setVisibility(View.GONE);
            return;
        }
        repo.getCurrent(allianceId, new SpecialMissionRepository.Callback<SpecialMissionRepository.MissionState>() {
            @Override public void onSuccess(SpecialMissionRepository.MissionState s) {
                if (s == null || !s.active) {
                    tvStatus.setText("Nema aktivne misije");
                    tvHp.setText("");
                    tvTime.setText("");
                    pb.setProgress(0); pb.setMax(1);
                    if (ivBoss != null) ivBoss.setImageResource(com.example.mobilneaplikacije.R.drawable.boss_idle);
                    if (btnStart != null) btnStart.setVisibility(isLeader ? View.VISIBLE : View.GONE);
                    return;
                }
                tvStatus.setText("Aktivna misija");
                tvHp.setText(s.bossHp + " / " + s.bossMaxHp + " HP");
                pb.setMax((int) Math.max(1, s.bossMaxHp));
                pb.setProgress((int) Math.max(0, s.bossHp));
                if (ivBoss != null) ivBoss.setImageResource(com.example.mobilneaplikacije.R.drawable.boss_idle);
                long rem = s.endsAt - System.currentTimeMillis();
                long days = Math.max(0, rem / (24L*60*60*1000));
                tvTime.setText("Preostalo: " + days + " d");
                if (btnStart != null) btnStart.setVisibility(View.GONE);

                repo.listProgress(allianceId, new SpecialMissionRepository.Callback<java.util.List<SpecialMissionRepository.UserProgress>>() {
                    @Override public void onSuccess(java.util.List<SpecialMissionRepository.UserProgress> data) {
                        if (adapter != null) adapter.setItems(data);
                    }
                    @Override public void onError(Exception e) { }
                });
            }
            @Override public void onError(Exception e) { toast(e.getMessage()); }
        });
    }

    private void toast(String m) { if (isAdded()) Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}

class ProgressAdapter extends RecyclerView.Adapter<ProgressVH> {
    private final java.util.List<SpecialMissionRepository.UserProgress> items = new java.util.ArrayList<>();
    private java.util.Map<String, String> nameByUid = new java.util.HashMap<>();

    public void setItems(java.util.List<SpecialMissionRepository.UserProgress> list) {
        items.clear();
        if (list != null) items.addAll(list);

        java.util.Collections.sort(items, (a, b) -> {
            int cmp = Long.compare(b.dealtHp, a.dealtHp);
            if (cmp != 0) return cmp;
            String an = nameByUid.getOrDefault(a.uid, "");
            String bn = nameByUid.getOrDefault(b.uid, "");
            return an.compareToIgnoreCase(bn);
        });
        notifyDataSetChanged();
    }
    public void setNameMap(java.util.Map<String, String> map) {
        this.nameByUid = (map != null) ? map : new java.util.HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull @Override public ProgressVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.view.LayoutInflater inf = android.view.LayoutInflater.from(parent.getContext());
        android.view.View v = inf.inflate(com.example.mobilneaplikacije.R.layout.row_mission_progress, parent, false);
        return new ProgressVH(v);
    }
    @Override public void onBindViewHolder(@NonNull ProgressVH h, int pos) {
        SpecialMissionRepository.UserProgress up = items.get(pos);
        String name = nameByUid.get(up.uid);
        if (name == null || name.trim().isEmpty()) name = "(nepoznat)";
        h.tvUsername.setText(name);
        int days = (up.chatDays == null) ? 0 : up.chatDays.size();
        String stats = "Kupovine: " + up.purchases
                + " • Pogoci: " + up.hits
                + " • Zadaci A: " + up.groupA
                + " • Zadaci B: " + up.groupB
                + " • Dani poruka: " + days;
        h.tvStats.setText(stats);
        h.tvDealt.setText("Umanjeni HP: " + up.dealtHp);

        long dmgPurchases = up.purchases * 2L;
        long dmgHits = up.hits * 2L;
        long dmgGroupA = up.groupA * 1L;
        long dmgGroupB = up.groupB * 4L;
        long dmgChatDays = days * 4L;
        long dmgBonus = up.noUnresolvedAwarded ? 10L : 0L;
        String breakdown = "• Kupovine: -" + dmgPurchases + " HP (" + up.purchases + "×2)\n"
                + "• Pogoci u regularnoj borbi: -" + dmgHits + " HP (" + up.hits + "×2)\n"
                + "• Rešeni laki/normalni/važni zadaci: -" + dmgGroupA + " HP (" + up.groupA + "×1)\n"
                + "• Rešeni ostali zadaci: -" + dmgGroupB + " HP (" + up.groupB + "×4)\n"
                + "• Dani sa porukom u savezu: -" + dmgChatDays + " HP (" + days + "×4)\n"
                + (up.noUnresolvedAwarded ? "• Bez nerešenih zadataka tokom misije: -10 HP" : "• Bez nerešenih zadataka tokom misije: nije ostvareno");
        h.tvBreakdown.setText(breakdown);
    }
    @Override public int getItemCount() { return items.size(); }
}
class ProgressVH extends RecyclerView.ViewHolder {
    final android.widget.TextView tvUsername;
    final android.widget.TextView tvStats;
    final android.widget.TextView tvDealt;
    final android.widget.TextView tvBreakdown;
    public ProgressVH(@NonNull View itemView) {
        super(itemView);
        tvUsername = itemView.findViewById(com.example.mobilneaplikacije.R.id.tvUsername);
        tvStats = itemView.findViewById(com.example.mobilneaplikacije.R.id.tvStats);
        tvDealt = itemView.findViewById(com.example.mobilneaplikacije.R.id.tvDealt);
        tvBreakdown = itemView.findViewById(com.example.mobilneaplikacije.R.id.tvBreakdown);
    }
}

package com.example.mobilneaplikacije.ui.equipment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.manager.SessionManager;
import com.example.mobilneaplikacije.data.model.Boss;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.ui.boss.BossFragment;

import java.util.List;

public class EquipmentSelectionFragment extends Fragment {

    private RecyclerView rvInventory;
    private Button btnStartBattle;
    private EquipmentAdapter adapter;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment_selection, container, false);

        session = new SessionManager(requireContext());
        rvInventory = view.findViewById(R.id.rvInventory);
        btnStartBattle = view.findViewById(R.id.btnStartBattle);

        List<Equipment> inventory = session.getInventory();

        adapter = new EquipmentAdapter(inventory, item -> {
            List<Equipment> active = session.getActiveEquipment();

            boolean alreadyActive = false;
            for (Equipment e : active) {
                if (e.getName().equals(item.getName())) {
                    alreadyActive = true;
                    break;
                }
            }

            if (!alreadyActive || item.isConsumable()) {
                active.add(item);
                session.saveActiveEquipment(active);
                item.setActive(true); // markiraj ga
                adapter.notifyDataSetChanged();

                Toast.makeText(requireContext(),
                        item.getName() + " aktiviran!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        item.getName() + " je već aktiviran!", Toast.LENGTH_SHORT).show();
            }
        });



        rvInventory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvInventory.setAdapter(adapter);

        btnStartBattle.setOnClickListener(v -> {
            // Učitaj igrača
            Player player = session.getPlayer();

            // Proveri da li već postoji boss
            Boss existingBoss = session.getBossState(player);

            if (existingBoss != null && !existingBoss.isDefeated()) {
                // Ako postoji boss koji NIJE poražen → vrati njega
                session.saveBossState(existingBoss);
            } else {
                // Ako nema ili je poražen → napravi novog za trenutni level
                session.clearBossState();
                Boss newBoss;
                if (player.getLevel() == 1) {
                    newBoss = new Boss(1, 0, 0); // prvi boss ima fix HP/coins
                } else {
                    Boss prev = new Boss(player.getLevel() - 1, 0, 0);
                    newBoss = new Boss(player.getLevel(), prev.getMaxHp(), prev.getCoinsReward());
                }
                session.saveBossState(newBoss);
            }

            // Pokreni BossFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BossFragment())
                    .addToBackStack(null)
                    .commit();
        });


        return view;
    }
}

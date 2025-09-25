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
import com.example.mobilneaplikacije.data.model.Boss;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.ui.boss.BossFragment;

import java.util.List;

public class EquipmentSelectionFragment extends Fragment {

    private RecyclerView rvInventory;
    private Button btnStartBattle;
    private EquipmentAdapter adapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment_selection, container, false);

        rvInventory = view.findViewById(R.id.rvInventory);
        btnStartBattle = view.findViewById(R.id.btnStartBattle);


        List<Equipment> inventory = null;
        adapter = new EquipmentAdapter(inventory, item -> {
            List<Equipment> active = null;

            boolean alreadyActive = false;
            for (Equipment e : active) {
                if (e.getName().equals(item.getName())) {
                    alreadyActive = true;
                    break;
                }
            }

            if (!alreadyActive || item.isConsumable()) {
                active.add(item);
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

            // Proveri da li već postoji boss


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

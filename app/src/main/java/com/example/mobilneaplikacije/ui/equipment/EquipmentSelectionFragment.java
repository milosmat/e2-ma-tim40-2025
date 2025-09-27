package com.example.mobilneaplikacije.ui.equipment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.ActiveItem;
import com.example.mobilneaplikacije.data.model.InventoryItem;
import com.example.mobilneaplikacije.data.model.Item;
import com.example.mobilneaplikacije.data.repository.CatalogRepository;
import com.example.mobilneaplikacije.data.repository.InventoryRepository;

import java.util.*;

public class EquipmentSelectionFragment extends Fragment {

    private RecyclerView rvInventory, rvActive, rvWeapons;
    private long anchorCoins = 0L;

    private final InventoryRepository invRepo = new InventoryRepository();
    private final CatalogRepository catalogRepo = new CatalogRepository();

    private final List<Item> catalog = new ArrayList<>();
    private final Map<String, Item> byId = new HashMap<>();
    private final List<InventoryItem> inventory = new ArrayList<>();
    private final List<InventoryItem> clothesAndPotions = new ArrayList<>();
    private final List<InventoryItem> weapons = new ArrayList<>();
    private ActiveAdapter activeAdapter;
    private InventoryAdapter inventoryAdapter;
    private WeaponsAdapter weaponsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment_selection, container, false);

    rvInventory = view.findViewById(R.id.rvInventory);
    rvActive = view.findViewById(R.id.rvActive);
    rvWeapons = view.findViewById(R.id.rvWeapons);

        rvInventory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWeapons.setLayoutManager(new LinearLayoutManager(getContext()));
        activeAdapter = new ActiveAdapter();
        rvActive.setAdapter(activeAdapter);
        weaponsAdapter = new WeaponsAdapter(new WeaponsAdapter.Listener() {
            @Override public void onUpgrade(InventoryItem inv) {
                invRepo.upgradeWeapon(inv.itemId, new InventoryRepository.Callback<Void>() {
                    @Override public void onSuccess(Void data) {
                        Toast.makeText(requireContext(), "Oruzje unapređeno", Toast.LENGTH_SHORT).show();
                        loadAll();
                    }
                    @Override public void onError(Exception e) {
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        rvWeapons.setAdapter(weaponsAdapter);

        inventoryAdapter = new InventoryAdapter(new InventoryAdapter.Listener() {
            @Override public void onActivate(InventoryItem inv) {
                Item ci = byId.get(inv.itemId);
                if (ci == null) return;
                if (ci.type == Item.Type.WEAPON) {
                    Toast.makeText(requireContext(), "Oruzje je pasivno i ne aktivira se.", Toast.LENGTH_SHORT).show();
                    return;
                }
                invRepo.activateItem(inv, new InventoryRepository.Callback<Void>() {
                    @Override public void onSuccess(Void data) { Toast.makeText(requireContext(), "Aktivirano", Toast.LENGTH_SHORT).show(); loadAll(); }
                    @Override public void onError(Exception e) { Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show(); }
                });
            }
        });
        rvInventory.setAdapter(inventoryAdapter);

        loadAll();
        return view;
    }

    private void loadAll() {
        invRepo.getPriceAnchorCoins(new InventoryRepository.Callback<Long>() {
            @Override public void onSuccess(Long data) {
                anchorCoins = (data == null) ? 0L : data;
                if (weaponsAdapter != null) weaponsAdapter.setAnchorCoins(anchorCoins);
            }
            @Override public void onError(Exception e) { }
        });
        catalogRepo.getAll(new CatalogRepository.Callback<List<Item>>() {
            @Override public void onSuccess(List<Item> data) {
                catalog.clear(); byId.clear();
                if (data != null) {
                    catalog.addAll(data);
                    for (Item i : data) byId.put(i.id, i);
                }
                invRepo.listInventory(new InventoryRepository.Callback<List<InventoryItem>>() {
                    @Override public void onSuccess(List<InventoryItem> inv) {
                        inventory.clear(); clothesAndPotions.clear();
                        weapons.clear();
                        if (inv != null) inventory.addAll(inv);
                        for (InventoryItem ii : inventory) {
                            Item i = byId.get(ii.itemId);
                            if (i == null) continue;
                            if (i.type == Item.Type.WEAPON) weapons.add(ii);
                            else if (ii.quantity > 0) clothesAndPotions.add(ii);
                        }
                        inventoryAdapter.submit(clothesAndPotions, byId);
                        weaponsAdapter.submit(weapons, byId);
                        invRepo.listActive(new InventoryRepository.Callback<List<ActiveItem>>() {
                            @Override public void onSuccess(List<ActiveItem> act) {
                                activeAdapter.submit(act, byId);
                            }
                            @Override public void onError(Exception e) { }
                        });
                    }
                    @Override public void onError(Exception e) { }
                });
            }
            @Override public void onError(Exception e) { }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAll();
    }

    static class ActiveAdapter extends RecyclerView.Adapter<ActiveVH> {
        private final List<ActiveItem> data = new ArrayList<>();
        private Map<String, Item> byId = new HashMap<>();
        void submit(List<ActiveItem> d, Map<String, Item> byId) {
            this.data.clear(); if (d != null) this.data.addAll(d); this.byId = byId; notifyDataSetChanged();
        }
        @NonNull @Override public ActiveVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_equipment, p, false);
            return new ActiveVH(view);
        }
        @Override public void onBindViewHolder(@NonNull ActiveVH h, int pos) {
            var a = data.get(pos);
            Item ci = byId.get(a.itemId);
            String name = ci != null ? ci.name : a.itemId;
            h.bind(name, a.remainingBattles);
        }
        @Override public int getItemCount() { return data.size(); }
    }

    static class ActiveVH extends RecyclerView.ViewHolder {
        TextView tvName, tvEffect, tvDuration; Button btnActivate;
        ActiveVH(@NonNull View v) { super(v); tvName = v.findViewById(R.id.tvName);
            tvEffect = v.findViewById(R.id.tvEffect); tvDuration = v.findViewById(R.id.tvDuration);
            btnActivate = v.findViewById(R.id.btnActivate);
            btnActivate.setVisibility(View.GONE); }
        void bind(String name, int remaining) {
            tvName.setText(name);
            tvEffect.setText("");
            tvDuration.setText(remaining <= 0 ? "Trajno" : ("Preostalo borbi: " + remaining));
        }
    }

    static class InventoryAdapter extends RecyclerView.Adapter<InventoryVH> {
        interface Listener { void onActivate(InventoryItem inv); }
        private final List<InventoryItem> data = new ArrayList<>();
        private Map<String, Item> byId = new HashMap<>();
        private final Listener listener;
        InventoryAdapter(Listener l) { this.listener = l; }
        void submit(List<InventoryItem> items, Map<String, Item> byId) { this.data.clear();
            if (items != null) this.data.addAll(items);
            this.byId = byId; notifyDataSetChanged(); }
        @NonNull @Override public InventoryVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_equipment, p, false);
            return new InventoryVH(view);
        }
        @Override public void onBindViewHolder(@NonNull InventoryVH h, int pos) {
            InventoryItem inv = data.get(pos);
            Item i = byId.get(inv.itemId);
            String name = i != null ? i.name : inv.itemId;
            String effect = i != null ? i.effect.name() : "";
            int dur = i != null ? (i.type == Item.Type.CLOTHES ? 2 : i.durationBattles) : 0;
            h.bind(name, effect, dur, inv, listener);
        }
        @Override public int getItemCount() { return data.size(); }
    }

    static class InventoryVH extends RecyclerView.ViewHolder {
        TextView tvName, tvEffect, tvDuration; Button btnActivate;
        InventoryVH(@NonNull View v) { super(v);
            tvName = v.findViewById(R.id.tvName);
            tvEffect = v.findViewById(R.id.tvEffect);
            tvDuration = v.findViewById(R.id.tvDuration);
            btnActivate = v.findViewById(R.id.btnActivate); }
        void bind(String name, String effect, int duration, InventoryItem inv, InventoryAdapter.Listener l) {
            tvName.setText(name);
            tvEffect.setText("Efekat: " + effect);
            tvDuration.setText(duration <= 0 ? "Jednokratno" : ("Traje " + duration + " borbi"));
            btnActivate.setText("Aktiviraj");
            btnActivate.setOnClickListener(v -> l.onActivate(inv));
        }
    }
    static class WeaponsAdapter extends RecyclerView.Adapter<WeaponsVH> {
        interface Listener { void onUpgrade(InventoryItem inv); }
        private final List<InventoryItem> data = new ArrayList<>();
        private Map<String, Item> byId = new HashMap<>();
        private final Listener listener;
        private long anchorCoins = 0L;
        WeaponsAdapter(Listener l) { this.listener = l; }
        void submit(List<InventoryItem> items, Map<String, Item> byId) { this.data.clear();
            if (items != null) this.data.addAll(items);
            this.byId = byId;
            notifyDataSetChanged(); }
        void setAnchorCoins(long anchor) {
            this.anchorCoins = anchor;
            notifyDataSetChanged();
        }
        @NonNull @Override public WeaponsVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_equipment, p, false);
            return new WeaponsVH(view);
        }
        @Override public void onBindViewHolder(@NonNull WeaponsVH h, int pos) {
            InventoryItem inv = data.get(pos);
            Item i = byId.get(inv.itemId);
            String name = i != null ? i.name : inv.itemId;
            String effect = i != null ? i.effect.name() : "";
            long price = (long) Math.round((double) anchorCoins * 0.60);
            h.bind(name, effect, inv, price, listener);
        }
        @Override public int getItemCount() { return data.size(); }
    }

    static class WeaponsVH extends RecyclerView.ViewHolder {
        TextView tvName, tvEffect, tvDuration; Button btnActivate;
        WeaponsVH(@NonNull View v) { super(v); tvName = v.findViewById(R.id.tvName);
            tvEffect = v.findViewById(R.id.tvEffect);
            tvDuration = v.findViewById(R.id.tvDuration);
            btnActivate = v.findViewById(R.id.btnActivate); }
        void bind(String name, String effect, InventoryItem inv, long price, WeaponsAdapter.Listener l) {
            tvName.setText(name + " (Lvl " + inv.upgradeLevel + ")");
            tvEffect.setText("Efekat: " + effect);
            tvDuration.setText("Trajan bonus");
            btnActivate.setText("Unapredi (" + price + ")");
            btnActivate.setOnClickListener(v -> l.onUpgrade(inv));
        }
    }
}

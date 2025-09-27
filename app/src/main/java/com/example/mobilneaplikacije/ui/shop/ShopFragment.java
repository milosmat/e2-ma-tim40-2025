package com.example.mobilneaplikacije.ui.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.InventoryItem;
import com.example.mobilneaplikacije.data.model.Item;
import com.example.mobilneaplikacije.data.repository.CatalogRepository;
import com.example.mobilneaplikacije.data.repository.InventoryRepository;
import com.example.mobilneaplikacije.data.repository.PlayerRepository;

import java.util.*;

public class ShopFragment extends Fragment implements ShopItemAdapter.Listener {

    private RecyclerView rvCatalog;
    private ProgressBar pb;
    private TextView tvAnchor;
    private ShopItemAdapter adapter;
    private final CatalogRepository catalogRepo = new CatalogRepository();
    private final InventoryRepository invRepo = new InventoryRepository();
    private final PlayerRepository playerRepo = new PlayerRepository();
    private long anchorCoins = 0L;
    private final List<Item> items = new ArrayList<>();
    private final Map<String, Integer> ownedQty = new HashMap<>();
    private int playerLevel = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvCatalog = view.findViewById(R.id.rvCatalog);
        pb = view.findViewById(R.id.pbLoading);
        tvAnchor = view.findViewById(R.id.tvAnchor);

        rvCatalog.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ShopItemAdapter(items, ownedQty,
                () -> anchorCoins,
                new ShopItemAdapter.PurchaseGate() {
                    @Override public boolean canBuy() { return playerLevel > 1; }
                    @Override public String reason() { return "Nivo 2+"; }
                },
                this);
        rvCatalog.setAdapter(adapter);

        loadAll();
    }

    private void loadAll() {
        pb.setVisibility(View.VISIBLE);
        playerRepo.loadPlayer(new PlayerRepository.PlayerCallback() {
            @Override public void onSuccess(com.example.mobilneaplikacije.data.model.Player player) {
                playerLevel = player.getLevel();
                loadShopData();
            }
            @Override public void onFailure(Exception e) {
                playerLevel = 1;
                loadShopData();
            }
        });
    }

    private void loadShopData() {
        invRepo.getPriceAnchorCoins(new InventoryRepository.Callback<Long>() {
            @Override public void onSuccess(Long v) {
                anchorCoins = v == null ? 0L : v;
                tvAnchor.setText(getString(R.string.shop_anchor, anchorCoins));
                invRepo.listInventory(new InventoryRepository.Callback<List<InventoryItem>>() {
                    @Override public void onSuccess(List<InventoryItem> inv) {
                        ownedQty.clear();
                        if (inv != null) for (InventoryItem ii : inv) ownedQty.put(ii.itemId, ii.quantity);
                        catalogRepo.getAll(new CatalogRepository.Callback<List<Item>>() {
                            @Override public void onSuccess(List<Item> data) {
                                items.clear();
                                if (data != null) items.addAll(data);
                                adapter.notifyDataSetChanged();
                                pb.setVisibility(View.GONE);
                            }
                            @Override public void onError(Exception e) { pb.setVisibility(View.GONE); toast(e.getMessage()); }
                        });
                    }
                    @Override public void onError(Exception e) { pb.setVisibility(View.GONE); toast(e.getMessage()); }
                });
            }
            @Override public void onError(Exception e) { pb.setVisibility(View.GONE); toast(e.getMessage()); }
        });
    }

    @Override
    public void onBuy(Item item) {
        if (playerLevel <= 1) {
            toast("Kupovina je dostupna od nivoa 2");
            return;
        }
        if (item.type == Item.Type.WEAPON) {
            toast(getString(R.string.shop_weapons_boss_only));
            return;
        }
        invRepo.purchaseItem(item, new InventoryRepository.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                toast(getString(R.string.shop_bought, item.name));
                loadAll();
            }
            @Override public void onError(Exception e) { toast(e.getMessage()); }
        });
    }

    private void toast(String m) {
        Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show();
    }
}

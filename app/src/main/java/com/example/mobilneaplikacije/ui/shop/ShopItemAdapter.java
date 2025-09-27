package com.example.mobilneaplikacije.ui.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Item;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.VH> {

    interface PriceProvider { long anchorCoins(); }
    interface PurchaseGate { boolean canBuy(); String reason(); }
    interface Listener { void onBuy(Item item); }

    private final List<Item> data;
    private final Map<String, Integer> ownedQty;
    private final PriceProvider priceProvider;
    private final PurchaseGate gate;
    private final Listener listener;

    public ShopItemAdapter(List<Item> data, Map<String, Integer> ownedQty, PriceProvider priceProvider, PurchaseGate gate, Listener listener) {
        this.data = data; this.ownedQty = ownedQty; this.priceProvider = priceProvider; this.gate = gate; this.listener = listener;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Item it = data.get(pos);
        h.tvName.setText(it.name);
        h.tvType.setText(it.type.name());
        String eff;
        switch (it.effect) {
            case INCREASE_PP: eff = "+" + (int) Math.round(it.valuePct * 100) + "% PP"; break;
            case INCREASE_SUCCESS: eff = "+" + (int) Math.round(it.valuePct * 100) + "% šanse"; break;
            case EXTRA_ATTACK: eff = "+" + (int) Math.round(it.valuePct * 100) + "% dodatni napad"; break;
            case EXTRA_COINS: eff = "+" + (int) Math.round(it.valuePct * 100) + "% novčića"; break;
            default: eff = ""; break;
        }
        h.tvEffect.setText(eff);

        int duration = it.durationBattles;
        h.tvDuration.setText(duration <= 0 ? (it.type == Item.Type.WEAPON ? "Trajno" : "Jednokratno") : ("Traje " + duration + " borbi"));

        Integer q = ownedQty.get(it.id);
        h.tvOwned.setText(q == null ? "0 kom" : (q + " kom"));

        long anchor = priceProvider.anchorCoins();
        long price = Math.max(0, Math.round(anchor * it.pricePctOfPrevBossReward));
        h.btnBuy.setText(h.itemView.getContext().getString(R.string.shop_buy_price, price));
    boolean weaponBlocked = it.type == Item.Type.WEAPON;
    boolean levelBlocked = !gate.canBuy();
    h.btnBuy.setEnabled(!weaponBlocked && !levelBlocked);
    if (levelBlocked) h.btnBuy.setText(gate.reason());

    int iconRes = resolveIconRes(h, it);
        h.ivIcon.setImageResource(iconRes);

    h.btnBuy.setOnClickListener(v -> listener.onBuy(it));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon; TextView tvName; TextView tvType; TextView tvEffect; TextView tvDuration; TextView tvOwned; Button btnBuy;
        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvName = v.findViewById(R.id.tvName);
            tvType = v.findViewById(R.id.tvType);
            tvEffect = v.findViewById(R.id.tvEffect);
            tvDuration = v.findViewById(R.id.tvDuration);
            tvOwned = v.findViewById(R.id.tvOwned);
            btnBuy = v.findViewById(R.id.btnBuy);
        }
    }

    private int resolveIconRes(@NonNull VH h, @NonNull Item it) {
        String idName = it.id == null ? "" : it.id.toLowerCase(Locale.ROOT);
        switch (it.type) {
            case WEAPON:
                if (idName.contains("sword")) return R.drawable.sword;
                if (idName.contains("bow")) return R.drawable.bow;
                if (idName.contains("shield")) return R.drawable.shield;
                return R.drawable.sword;
            case CLOTHES:
                if (idName.contains("gloves")) return R.drawable.gloves;
                if (idName.contains("boots")) return R.drawable.boots;
                return R.drawable.gloves;
            case POTION:
                if (idName.startsWith("potion_pp_")) {
                    try {
                        String numStr = idName.substring("potion_pp_".length());
                        int usIdx = numStr.indexOf('_');
                        if (usIdx >= 0) numStr = numStr.substring(0, usIdx);
                        int val = Integer.parseInt(numStr);
                        if (val == 40) return R.drawable.potion_40;
                        if (val == 20) return R.drawable.potion_20;
                        if (val == 10) return R.drawable.potion_10_perm;
                        if (val == 5) return R.drawable.potion_5_perm;
                    } catch (Exception ignored) {}
                }
                break;
        }
        return R.drawable.ic_shop_placeholder;
    }
}

package com.example.mobilneaplikacije.data.repository;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Item;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class CatalogRepository {
    public interface Callback<T> { void onSuccess(@Nullable T data); void onError(Exception e); }

    private final FirebaseFirestore db;
    public CatalogRepository() { this.db = FirebaseFirestore.getInstance(); }

    private CollectionReference col() { return db.collection("catalog_items"); }

    public void getAll(Callback<List<Item>> cb) {
        col().get().addOnSuccessListener(snap -> {
            List<Item> items = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Item it = new Item();
                it.id = d.getId();
                String type = d.getString("type");
                it.type = Item.Type.valueOf(type);
                String effect = d.getString("effect");
                it.effect = Item.Effect.valueOf(effect);
                it.name = d.getString("name");
                it.valuePct = d.getDouble("valuePct");
                Long dur = d.getLong("durationBattles");
                it.durationBattles = dur.intValue();
                it.consumable = d.getBoolean("consumable");
                it.pricePctOfPrevBossReward =  d.getDouble("pricePctOfPrevBossReward");
                it.availability = d.getString("availability");
                it.stackable = d.getBoolean("stackable");
                it.imageResName = d.getString("imageResName");
                items.add(it);
            }
            cb.onSuccess(items);
        }).addOnFailureListener(cb::onError);
    }
}

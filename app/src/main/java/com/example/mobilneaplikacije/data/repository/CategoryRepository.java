package com.example.mobilneaplikacije.data.repository;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepository {

    public interface Callback<T> {
        void onSuccess(@Nullable T data);
        void onError(Exception e);
    }

    private final Context context;
    private final FirebaseFirestore db;
    private final String uid;

    public CategoryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("User not logged in");
        this.uid = u.getUid();
    }

    private CollectionReference col() {
        return db.collection("users").document(uid).collection("categories");
    }

    public void insertCategory(Category category, Callback<String> cb) {
        isColorTaken(category.getColor(), null, new Callback<Boolean>() {
            @Override public void onSuccess(Boolean taken) {
                if (Boolean.TRUE.equals(taken)) {
                    Toast.makeText(context, "Boja je već zauzeta!", Toast.LENGTH_LONG).show();
                    cb.onError(new IllegalStateException("COLOR_TAKEN"));
                    return;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("name", category.getName());
                map.put("color", category.getColor());
                map.put("createdAt", FieldValue.serverTimestamp());

                col().add(map)
                        .addOnSuccessListener(ref -> cb.onSuccess(ref.getId()))
                        .addOnFailureListener(cb::onError);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    public void updateCategory(Category category, Callback<Void> cb) {
        String idHash = category.getIdHash();
        if (idHash == null || idHash.isEmpty()) {
            cb.onError(new IllegalArgumentException("Missing category idHash"));
            return;
        }
        isColorTaken(category.getColor(), idHash, new Callback<Boolean>() {
            @Override public void onSuccess(Boolean taken) {
                if (Boolean.TRUE.equals(taken)) {
                    Toast.makeText(context, "Boja je već zauzeta!", Toast.LENGTH_LONG).show();
                    cb.onError(new IllegalStateException("COLOR_TAKEN"));
                    return;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("name", category.getName());
                map.put("color", category.getColor());

                col().document(idHash)
                        .set(map, SetOptions.merge())
                        .addOnSuccessListener(v -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
            }
            @Override public void onError(Exception e) { cb.onError(e); }
        });
    }

    public void getAllCategories(Callback<List<Category>> cb) {
        col().orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Category> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Category c = new Category();
                        c.setIdHash(d.getId());
                        c.setName(d.getString("name"));
                        c.setColor(d.getString("color"));
                        out.add(c);
                    }
                    cb.onSuccess(out);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getCategoryById(String idHash, Callback<Category> cb) {
        if (idHash == null || idHash.isEmpty()) {
            cb.onError(new IllegalArgumentException("Missing category idHash"));
            return;
        }
        col().document(idHash).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { cb.onSuccess(null); return; }
                    Category c = new Category();
                    c.setIdHash(d.getId());
                    c.setName(d.getString("name"));
                    c.setColor(d.getString("color"));
                    cb.onSuccess(c);
                })
                .addOnFailureListener(cb::onError);
    }

    private void isColorTaken(String color, @Nullable String ignoreIdHash, Callback<Boolean> cb) {
        col().whereEqualTo("color", color).get()
                .addOnSuccessListener(snap -> {
                    boolean taken = false;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (ignoreIdHash != null && ignoreIdHash.equals(d.getId())) continue;
                        taken = true; break;
                    }
                    cb.onSuccess(taken);
                })
                .addOnFailureListener(cb::onError);
    }
}

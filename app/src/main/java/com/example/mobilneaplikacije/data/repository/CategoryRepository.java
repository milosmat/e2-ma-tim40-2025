package com.example.mobilneaplikacije.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.example.mobilneaplikacije.data.db.AppDatabase;
import com.example.mobilneaplikacije.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {
    private AppDatabase dbHelper;
    private Context context;

    public CategoryRepository(Context context) {
        this.context = context;
        dbHelper = new AppDatabase(context);
    }

    // ➕ Insert
    public long insertCategory(Category category) {
        if (isColorTaken(category.getColor(), -1)) {
            Toast.makeText(context, "Boja je već zauzeta!", Toast.LENGTH_LONG).show();
            return -1;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        values.put("color", category.getColor());
        long id = db.insert("Category", null, values);
        db.close();
        return id;
    }

    // ✏️ Update
    public void updateCategory(Category category) {
        if (isColorTaken(category.getColor(), category.getId())) {
            Toast.makeText(context, "Boja je već zauzeta!", Toast.LENGTH_LONG).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        values.put("color", category.getColor());

        db.update("Category", values, "id=?", new String[]{String.valueOf(category.getId())});
        db.close();
    }

    // ❌ Delete
    public void deleteCategory(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("Category", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 📥 Get All
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Category", null);

        if (cursor.moveToFirst()) {
            do {
                Category c = new Category();
                c.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                c.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                c.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
                categories.add(c);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // 📥 Get by ID
    public Category getCategoryById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Category WHERE id=?",
                new String[]{String.valueOf(id)});
        Category c = null;
        if (cursor.moveToFirst()) {
            c = new Category();
            c.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            c.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            c.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
        }
        cursor.close();
        db.close();
        return c;
    }

    // ✅ Provera da li je boja već zauzeta
    private boolean isColorTaken(String color, long ignoreId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM Category WHERE color=? AND id<>?",
                new String[]{color, String.valueOf(ignoreId)}
        );
        boolean taken = false;
        if (cursor.moveToFirst()) {
            taken = cursor.getInt(0) > 0;
        }
        cursor.close();
        db.close();
        return taken;
    }
}

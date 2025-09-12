package com.example.mobilneaplikacije.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mobilneaplikacije.data.db.AppDatabase;
import com.example.mobilneaplikacije.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {
    private AppDatabase dbHelper;

    public CategoryRepository(Context context) {
        dbHelper = new AppDatabase(context);
    }

    // Insert
    public long insertCategory(Category category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", category.getName());
        values.put("color", category.getColor());
        long id = db.insert("Category", null, values);
        db.close();
        return id;
    }

    // Get all
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Category", null);

        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                category.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    // Delete
    public void deleteCategory(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("Category", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }
}


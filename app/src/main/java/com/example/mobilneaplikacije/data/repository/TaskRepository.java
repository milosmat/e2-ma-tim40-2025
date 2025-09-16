package com.example.mobilneaplikacije.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mobilneaplikacije.data.db.AppDatabase;
import com.example.mobilneaplikacije.data.model.Category;
import com.example.mobilneaplikacije.data.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskRepository {
    private AppDatabase dbHelper;

    public TaskRepository(Context context) {
        dbHelper = AppDatabase.getInstance(context);
    }

    // Insert
    public long insertTask(Task task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("categoryId", task.getCategoryId());
        values.put("isRecurring", task.isRecurring() ? 1 : 0);
        values.put("repeatInterval", task.getRepeatInterval());
        values.put("repeatUnit", task.getRepeatUnit());
        values.put("startDate", task.getStartDate());
        values.put("endDate", task.getEndDate());
        values.put("difficulty", task.getDifficulty());
        values.put("importance", task.getImportance());
        values.put("xpPoints", task.getXpPoints());
        values.put("status", task.getStatus());
        values.put("dueDateTime", task.getDueDateTime());
        long id = db.insert("Task", null, values);
        db.close();
        return id;
    }

    // Get all + auto označavanje "MISSED"
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Task", null);

        long now = System.currentTimeMillis();
        long threeDaysAgo = now - 3L * 24 * 60 * 60 * 1000;

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                task.setCategoryId(cursor.getLong(cursor.getColumnIndexOrThrow("categoryId")));
                task.setRecurring(cursor.getInt(cursor.getColumnIndexOrThrow("isRecurring")) == 1);
                task.setRepeatInterval(cursor.getInt(cursor.getColumnIndexOrThrow("repeatInterval")));
                task.setRepeatUnit(cursor.getString(cursor.getColumnIndexOrThrow("repeatUnit")));
                task.setStartDate(cursor.getLong(cursor.getColumnIndexOrThrow("startDate")));
                task.setEndDate(cursor.getLong(cursor.getColumnIndexOrThrow("endDate")));
                task.setDifficulty(cursor.getString(cursor.getColumnIndexOrThrow("difficulty")));
                task.setImportance(cursor.getString(cursor.getColumnIndexOrThrow("importance")));
                task.setXpPoints(cursor.getInt(cursor.getColumnIndexOrThrow("xpPoints")));
                task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
                task.setDueDateTime(cursor.getLong(cursor.getColumnIndexOrThrow("dueDateTime")));

                // 🚨 Automatski označi kao MISSED ako je ACTIVE i prošlo više od 3 dana
                if ("ACTIVE".equals(task.getStatus()) && task.getDueDateTime() < threeDaysAgo) {
                    task.setStatus("MISSED");
                    updateTaskStatus(task.getId(), "MISSED");
                }

                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public void updateTask(Task task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("categoryId", task.getCategoryId());
        values.put("isRecurring", task.isRecurring() ? 1 : 0);
        values.put("repeatInterval", task.getRepeatInterval());
        values.put("repeatUnit", task.getRepeatUnit());
        values.put("startDate", task.getStartDate());
        values.put("endDate", task.getEndDate());
        values.put("difficulty", task.getDifficulty());
        values.put("importance", task.getImportance());
        values.put("xpPoints", task.getXpPoints());
        values.put("status", task.getStatus());
        values.put("dueDateTime", task.getDueDateTime());

        db.update("Task", values, "id=?", new String[]{String.valueOf(task.getId())});
        db.close();
    }

    public void updateTaskStatus(long id, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        db.update("Task", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteTask(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("Task", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Task getTaskById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM Task WHERE id=?", new String[]{String.valueOf(id)});
        Task t = null;
        if (c.moveToFirst()) {
            t = new Task();
            t.setId(c.getLong(c.getColumnIndexOrThrow("id")));
            t.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
            t.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
            t.setCategoryId(c.getLong(c.getColumnIndexOrThrow("categoryId")));
            t.setRecurring(c.getInt(c.getColumnIndexOrThrow("isRecurring")) == 1);
            t.setRepeatInterval(c.getInt(c.getColumnIndexOrThrow("repeatInterval")));
            t.setRepeatUnit(c.getString(c.getColumnIndexOrThrow("repeatUnit")));
            t.setStartDate(c.getLong(c.getColumnIndexOrThrow("startDate")));
            t.setEndDate(c.getLong(c.getColumnIndexOrThrow("endDate")));
            t.setDifficulty(c.getString(c.getColumnIndexOrThrow("difficulty")));
            t.setImportance(c.getString(c.getColumnIndexOrThrow("importance")));
            t.setXpPoints(c.getInt(c.getColumnIndexOrThrow("xpPoints")));
            t.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
            t.setDueDateTime(c.getLong(c.getColumnIndexOrThrow("dueDateTime")));

            // 🚨 Provera i za pojedinačan upit
            long threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000;
            if ("ACTIVE".equals(t.getStatus()) && t.getDueDateTime() < threeDaysAgo) {
                t.setStatus("MISSED");
                updateTaskStatus(t.getId(), "MISSED");
            }
        }
        c.close();
        db.close();
        return t;
    }

    public void logCompletion(long taskId, long completedAt, String difficulty, String importance, int xpAwarded) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("taskId", taskId);
        v.put("completedAt", completedAt);
        v.put("difficulty", difficulty);
        v.put("importance", importance);
        v.put("xpAwarded", xpAwarded);
        db.insert("CompletionLog", null, v);
        db.close();
    }

    private int countCompletionsBetween(String difficulty, String importance, long startMillis, long endMillis) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM CompletionLog WHERE difficulty=? AND importance=? AND completedAt BETWEEN ? AND ?",
                new String[]{difficulty, importance, String.valueOf(startMillis), String.valueOf(endMillis)}
        );
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    public int countToday(String difficulty, String importance) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = start + 24L * 60 * 60 * 1000 - 1;
        return countCompletionsBetween(difficulty, importance, start, end);
    }

    public int countThisWeek(String difficulty, String importance) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = start + 7L * 24 * 60 * 60 * 1000 - 1;
        return countCompletionsBetween(difficulty, importance, start, end);
    }

    public int countThisMonth(String difficulty, String importance) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long end = cal.getTimeInMillis();
        return countCompletionsBetween(difficulty, importance, start, end);
    }

    public Category getCategoryById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM Category WHERE id=?", new String[]{String.valueOf(id)});
        Category cat = null;
        if (c.moveToFirst()) {
            cat = new Category();
            cat.setId(c.getLong(c.getColumnIndexOrThrow("id")));
            cat.setName(c.getString(c.getColumnIndexOrThrow("name")));
            cat.setColor(c.getString(c.getColumnIndexOrThrow("color")));
        }
        c.close();
        db.close();
        return cat;
    }

    public double calculateSuccessRate() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Broj završenih (DONE)
        Cursor doneCursor = db.rawQuery("SELECT COUNT(*) FROM Task WHERE status='DONE'", null);
        int done = 0;
        if (doneCursor.moveToFirst()) done = doneCursor.getInt(0);
        doneCursor.close();

        // Broj ukupnih zadataka koji se računaju (ACTIVE + DONE + MISSED)
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM Task WHERE status!='PAUSED' AND status!='CANCELLED'", null);
        int total = 0;
        if (totalCursor.moveToFirst()) total = totalCursor.getInt(0);
        totalCursor.close();

        db.close();

        if (total == 0) return 0.0;
        return (done * 100.0) / total;
    }

}

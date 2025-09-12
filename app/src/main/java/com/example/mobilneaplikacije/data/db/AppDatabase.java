package com.example.mobilneaplikacije.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 2;

    public AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Category (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, color TEXT)");
        db.execSQL("CREATE TABLE Task (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT," +
                "description TEXT," +
                "categoryId INTEGER," +
                "isRecurring INTEGER," +
                "repeatInterval INTEGER," +
                "repeatUnit TEXT," +
                "startDate INTEGER," +
                "endDate INTEGER," +
                "difficulty TEXT," +
                "importance TEXT," +
                "xpPoints INTEGER," +
                "status TEXT," +
                "dueDateTime INTEGER," +
                "FOREIGN KEY(categoryId) REFERENCES Category(id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS CompletionLog (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "taskId INTEGER," +
                "completedAt INTEGER," +     // millis
                "difficulty TEXT," +
                "importance TEXT," +
                "xpAwarded INTEGER," +
                "FOREIGN KEY(taskId) REFERENCES Task(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS CompletionLog (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "taskId INTEGER," +
                    "completedAt INTEGER," +
                    "difficulty TEXT," +
                    "importance TEXT," +
                    "xpAwarded INTEGER," +
                    "FOREIGN KEY(taskId) REFERENCES Task(id))");
        }
    }
}

package com.example.mobilneaplikacije;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobilneaplikacije.ui.category.CategoryListFragment;
import com.example.mobilneaplikacije.ui.task.AddTaskFragment;
import com.example.mobilneaplikacije.ui.task.TaskCalendarFragment;
import com.example.mobilneaplikacije.ui.task.TaskListFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_list) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskListFragment())
                        .commit();
                return true;
            } else if (id == R.id.nav_add) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AddTaskFragment())
                        .commit();
                return true;
            }else if (id == R.id.nav_calendar) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskCalendarFragment())
                        .commit();
                return true;
            }else if (id == R.id.nav_categories) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CategoryListFragment())
                        .commit();
                return true;
            }
            return false;
        });

        // Default fragment kada se app otvori (lista zadataka)
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }
    }
}

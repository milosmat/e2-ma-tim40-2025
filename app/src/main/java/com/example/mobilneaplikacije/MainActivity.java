package com.example.mobilneaplikacije;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobilneaplikacije.ui.auth.LoginFragment;
import com.example.mobilneaplikacije.ui.category.CategoryListFragment;
import com.example.mobilneaplikacije.ui.profile.ProfileFragment;
import com.example.mobilneaplikacije.ui.task.AddTaskFragment;
import com.example.mobilneaplikacije.ui.task.TaskCalendarFragment;
import com.example.mobilneaplikacije.ui.task.TaskListFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.mobilneaplikacije.ui.boss.BossFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        // treba da se skloni
        //FirebaseAuth.getInstance().signOut();

        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNav = findViewById(R.id.bottomNav);
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
            } else if (id == R.id.nav_calendar) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskCalendarFragment())
                        .commit();
                return true;
            } else if (id == R.id.nav_categories) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CategoryListFragment())
                        .commit();
                return true;
            } else if (id == R.id.nav_boss) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new BossFragment())
                        .commit();
                return true;
            }
            return false;
        });

        // Provera Firebase usera na startu
        if (savedInstanceState == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser u = auth.getCurrentUser();

            if (u == null || !u.isEmailVerified()) {
                if (u != null) auth.signOut();
                // Nije ulogovan ili nije verifikovan → otvaramo login
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
                bottomNav.setVisibility(View.GONE);
                toolbar.setVisibility(View.GONE);
            } else {
                // Već ulogovan i verifikovan → otvaramo glavni deo
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskListFragment())
                        .commit();
                bottomNav.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_nav_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper metoda za login/logout fragmente da prikažu ili sakriju bottom nav
    public void setBottomNavVisible(boolean visible) {
        if (bottomNav != null) {
            bottomNav.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setToolbarVisible(boolean visible) {
        if (toolbar != null) {
            toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}

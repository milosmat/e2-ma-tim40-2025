package com.example.mobilneaplikacije;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobilneaplikacije.data.model.AllianceInvite;
import com.example.mobilneaplikacije.data.repository.AllianceRepository;
import com.example.mobilneaplikacije.notifications.AllianceInviteActionReceiver;
import com.example.mobilneaplikacije.ui.auth.LoginFragment;
import com.example.mobilneaplikacije.ui.category.CategoryListFragment;
import com.example.mobilneaplikacije.ui.equipment.EquipmentSelectionFragment;
import com.example.mobilneaplikacije.ui.friends.FriendsFragment;
import com.example.mobilneaplikacije.ui.profile.ProfileFragment;
import com.example.mobilneaplikacije.ui.shop.ShopFragment;
import com.example.mobilneaplikacije.ui.statistics.StatisticsFragment;
import com.example.mobilneaplikacije.ui.task.AddTaskFragment;
import com.example.mobilneaplikacije.ui.task.TaskCalendarFragment;
import com.example.mobilneaplikacije.ui.task.TaskListFragment;
import com.example.mobilneaplikacije.ui.mission.SpecialMissionFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;
    private static final String CHANNEL_ID = "alliance_invites";
    private ListenerRegistration invitesRegistration;
    private FirebaseAuth.AuthStateListener authStateListener;
    private AllianceRepository allianceRepo;

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
            } else if (id == R.id.nav_shop) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ShopFragment())
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
                //Vec ulogoban i verifikovan
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new TaskListFragment())
                        .commit();
                bottomNav.setVisibility(View.VISIBLE);
                toolbar.setVisibility(View.VISIBLE);
            }
        }

        // Notifications channel and auth listener
        createNotificationChannel();
        setupAuthStateListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
    FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
        checkAndRequestNotificationPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        stopAllianceInvitesListener();
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
        } else if (item.getItemId() == R.id.action_statistics) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StatisticsFragment())
                    .commit();
            return true;
        } else if (item.getItemId() == R.id.action_equipment) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new EquipmentSelectionFragment())
                    .commit();
            return true;
        } else if (item.getItemId() == R.id.action_friends) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new FriendsFragment())
                    .commit();
            return true;
        } else if (item.getItemId() == R.id.action_special_mission) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SpecialMissionFragment())
                    .commit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setBottomNavVisible(boolean visible) {
        if (bottomNav != null) {
            bottomNav.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alliance Invites";
            String description = "Obavestenja o pozivima u savez";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startAllianceInvitesListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (allianceRepo == null) allianceRepo = new AllianceRepository();
        invitesRegistration = allianceRepo.listenIncomingInvites(new AllianceRepository.InviteAddedCallback() {
            @Override
            public void onInviteAdded(AllianceInvite invite, String allianceName, String inviterUsername) {
                showAllianceInviteNotification(allianceName, inviterUsername, invite.allianceId);
            }
            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void stopAllianceInvitesListener() {
        if (invitesRegistration != null) {
            invitesRegistration.remove();
            invitesRegistration = null;
        }
    }

    private void showAllianceInviteNotification(String allianceName, String inviterName, String allianceIdFallback) {
        String title = "Novi poziv u savez";
        String text = "Pozvani ste u " + (allianceName != null ? allianceName : (allianceIdFallback != null ? allianceIdFallback : "savez"))
            + (inviterName != null ? (" od " + inviterName) : "");

        int notificationId = (allianceIdFallback != null) ? (0x7FFFFFFF & allianceIdFallback.hashCode()) : (int) (System.currentTimeMillis() & 0xFFFFFF);

        Intent acceptIntent = new Intent(this, AllianceInviteActionReceiver.class);
        acceptIntent.putExtra(AllianceInviteActionReceiver.EXTRA_ALLIANCE_ID, allianceIdFallback);
        acceptIntent.putExtra(AllianceInviteActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        acceptIntent.putExtra(AllianceInviteActionReceiver.EXTRA_ACCEPT, true);
        PendingIntent acceptPi = PendingIntent.getBroadcast(
                this, notificationId + 1, acceptIntent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent declineIntent = new Intent(this, AllianceInviteActionReceiver.class);
        declineIntent.putExtra(AllianceInviteActionReceiver.EXTRA_ALLIANCE_ID, allianceIdFallback);
        declineIntent.putExtra(AllianceInviteActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
        declineIntent.putExtra(AllianceInviteActionReceiver.EXTRA_ACCEPT, false);
        PendingIntent declinePi = PendingIntent.getBroadcast(
                this, notificationId + 2, declineIntent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        .addAction(new NotificationCompat.Action(0, "Prihvati", acceptPi))
        .addAction(new NotificationCompat.Action(0, "Odbij", declinePi));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private void setupAuthStateListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser u = firebaseAuth.getCurrentUser();
            if (u != null && u.isEmailVerified()) {
                stopAllianceInvitesListener();
                startAllianceInvitesListener();
            } else {
                stopAllianceInvitesListener();
            }
        };
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    public void setToolbarVisible(boolean visible) {
        if (toolbar != null) {
            toolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllianceInvitesListener();
    }
}

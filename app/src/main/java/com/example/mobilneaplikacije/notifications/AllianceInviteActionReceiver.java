package com.example.mobilneaplikacije.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;

import com.example.mobilneaplikacije.data.repository.AllianceRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AllianceInviteActionReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALLIANCE_ID = "extra_alliance_id";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_ACCEPT = "extra_accept";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean accept = intent.getBooleanExtra(EXTRA_ACCEPT, false);
        String allianceId = intent.getStringExtra(EXTRA_ALLIANCE_ID);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || allianceId == null || allianceId.isEmpty()) {
            return;
        }

        try {
            AllianceRepository repo = new AllianceRepository();
            repo.respondToInvite(allianceId, accept, new AllianceRepository.Callback<Void>() {
                @Override public void onSuccess(Void data) {
                    NotificationManagerCompat.from(context).cancel(notificationId);
                }
                @Override public void onError(Exception e) {
                }
            });
        } catch (Exception ignored) { }
    }
}
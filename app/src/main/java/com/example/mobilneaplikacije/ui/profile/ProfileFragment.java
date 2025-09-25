package com.example.mobilneaplikacije.ui.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.MainActivity;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.data.repository.AuthRepository;
import com.example.mobilneaplikacije.ui.auth.LoginFragment;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvUsername, tvLevel, tvTitle, tvPP, tvXP, tvCoins, tvMissions;
    private Button btnChangePassword;

    private AuthRepository authRepo;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvPP = view.findViewById(R.id.tvPP);
        tvXP = view.findViewById(R.id.tvXP);
        tvCoins = view.findViewById(R.id.tvCoins);

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        authRepo = new AuthRepository();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        btnLogout.setOnClickListener(v -> logOut());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Player player = new Player(
                                doc.getString("username"),
                                doc.getString("avatar"),
                                doc.getLong("level").intValue(),
                                doc.getString("title"),
                                doc.getLong("pp").intValue(),
                                doc.getLong("xp").intValue(),
                                doc.getLong("coins").intValue(),
                                doc.getDouble("successRate")
                        );

                        tvUsername.setText(player.getUsername());
                        tvLevel.setText("Nivo: " + player.getLevel());
                        tvTitle.setText("Titula: " + player.getTitle());
                        tvPP.setText("PP: " + player.getPp());
                        tvXP.setText("XP: " + player.getXp());
                        tvCoins.setText("Novcici: " + player.getCoins());

                        int avatarRes = getResources().getIdentifier(
                                player.getAvatar(), "drawable", requireContext().getPackageName());
                        if (avatarRes != 0) ivAvatar.setImageResource(avatarRes);
                    }
                });

        loadCompletedMissionsCount();
    }

    private void loadCompletedMissionsCount() {
        if (user == null) return;

        CollectionReference logsRef = db.collection("users")
                .document(user.getUid())
                .collection("completionLogs");

        Query qWithXp = logsRef.whereGreaterThan("xpAwarded", 0);

        try {
            logsRef.count().get(AggregateSource.SERVER)
                    .addOnSuccessListener((AggregateQuerySnapshot snapAll) -> {
                        final long total = snapAll.getCount();

                        qWithXp.count().get(AggregateSource.SERVER)
                                .addOnSuccessListener((AggregateQuerySnapshot snapWithXp) -> {
                                    long withXp = snapWithXp.getCount();
                                    long withoutXp = Math.max(0, total - withXp);

                                    tvMissions.setText("Završene misije: " + total + " (sa XP: " + withXp + " misija, bez XP: " + withoutXp + " misija)");
                                })
                                .addOnFailureListener(e -> fallbackCountLogs(logsRef));
                    })
                    .addOnFailureListener(e -> fallbackCountLogs(logsRef));

        } catch (Throwable t) {
            fallbackCountLogs(logsRef);
        }
    }

    private void fallbackCountLogs(CollectionReference logsRef) {
        logsRef.get()
                .addOnSuccessListener(qs -> {
                    int total = qs.size();
                    int withXp = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot d : qs) {
                        Long xpL = d.getLong("xpAwarded");
                        int xp = xpL == null ? 0 : xpL.intValue();
                        if (xp > 0) withXp++;
                    }
                    int withoutXp = Math.max(0, total - withXp);

                    tvMissions.setText(
                            "Završene misije: " + total + " (sa XP: " + withXp + " misija, bez XP: " + withoutXp + " misija)"
                    );
                })
                .addOnFailureListener(e ->
                        tvMissions.setText("Završene misije: -")
                );
    }
    private void logOut() {
        authRepo.logOut();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();

        ((MainActivity) requireActivity()).setBottomNavVisible(false);
        ((MainActivity) requireActivity()).setToolbarVisible(false);
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_change_password, null);

        EditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPass = dialogView.findViewById(R.id.etNewPassword);
        EditText etNewPass2 = dialogView.findViewById(R.id.etNewPassword2);

        new AlertDialog.Builder(getContext())
                .setTitle("Promena lozinke")
                .setView(dialogView)
                .setPositiveButton("Promeni", (d, w) -> {
                    String current = etCurrent.getText().toString();
                    String newPas = etNewPass.getText().toString();
                    String newPas2 = etNewPass2.getText().toString();

                    if (TextUtils.isEmpty(current) || TextUtils.isEmpty(newPas) || TextUtils.isEmpty(newPas2)) {
                        Toast.makeText(getContext(), "Popuni sva polja", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPas.equals(newPas2)) {
                        Toast.makeText(getContext(), "Nove lozinke se ne poklapaju", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    authRepo.changePassword(current, newPas, new AuthRepository.AuthCallback() {
                        @Override
                        public void onSucces(String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String message) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Otkazi", null)
                .show();
    }
}

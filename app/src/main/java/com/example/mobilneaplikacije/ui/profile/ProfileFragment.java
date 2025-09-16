package com.example.mobilneaplikacije.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.manager.SessionManager;
import com.example.mobilneaplikacije.data.model.Player;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvUsername, tvLevel, tvTitle, tvPP, tvXP, tvCoins, tvMissions;

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
        tvMissions = view.findViewById(R.id.tvMissions);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData(); // 🔄 svaki put kad se vratiš na ekran, učita najnovije
    }

    private void loadProfileData() {
        SessionManager session = new SessionManager(requireContext());
        Player player = session.getPlayer();

        tvUsername.setText(player.getUsername());
        tvLevel.setText("Nivo: " + player.getLevel());
        tvTitle.setText("Titula: " + player.getTitle());
        tvPP.setText("PP: " + player.getPp());
        tvXP.setText("XP: " + player.getXp());
        tvCoins.setText("Novčići: " + player.getCoins());

        int avatarRes = getResources().getIdentifier(player.getAvatar(), "drawable", requireContext().getPackageName());
        if (avatarRes != 0) {
            ivAvatar.setImageResource(avatarRes);
        }
    }
}

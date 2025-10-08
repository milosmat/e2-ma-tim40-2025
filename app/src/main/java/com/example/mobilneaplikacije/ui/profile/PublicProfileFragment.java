package com.example.mobilneaplikacije.ui.profile;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Item;
import com.example.mobilneaplikacije.data.model.PublicProfile;
import com.example.mobilneaplikacije.data.repository.PlayerRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class PublicProfileFragment extends Fragment {
    private static final String ARG_UID = "uid";
    public static PublicProfileFragment newInstance(String uid) {
        Bundle b = new Bundle(); b.putString(ARG_UID, uid);
        PublicProfileFragment f = new PublicProfileFragment(); f.setArguments(b); return f;
    }

    private String uid;
    private ImageView ivAvatar, ivQr;
    private TextView tvUsername, tvLevel, tvTitle, tvXp;
    private LinearLayout badgesContainer, equipmentContainer;
    private final PlayerRepository repo = new PlayerRepository();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_public_profile, container, false);
        uid = getArguments() != null ? getArguments().getString(ARG_UID) : null;
        ivAvatar = v.findViewById(R.id.ivAvatar);
        ivQr = v.findViewById(R.id.ivQr);
        tvUsername = v.findViewById(R.id.tvUsername);
        tvLevel = v.findViewById(R.id.tvLevel);
        tvTitle = v.findViewById(R.id.tvTitle);
        tvXp = v.findViewById(R.id.tvXp);
        badgesContainer = v.findViewById(R.id.containerBadges);
        equipmentContainer = v.findViewById(R.id.containerEquipment);
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        if (TextUtils.isEmpty(uid)) return;
        repo.loadPublicProfile(uid, new PlayerRepository.PublicProfileCallback() {
            @Override public void onSuccess(PublicProfile p) {
                bind(p);
            }
            @Override public void onFailure(Exception e) { }
        });
    }

    private void bind(PublicProfile p) {
        tvUsername.setText(p.username);
        tvLevel.setText("Nivo: " + p.level);
        tvTitle.setText("Titula: " + (p.title == null ? "" : p.title));
        tvXp.setText("XP: " + p.xp);
        int avatarRes = getResources().getIdentifier(p.avatar == null ? "ic_person" : p.avatar, "drawable", requireContext().getPackageName());
        if (avatarRes != 0) ivAvatar.setImageResource(avatarRes);

        badgesContainer.removeAllViews();
        loadBadgeForUser(p.uid);

        equipmentContainer.removeAllViews();
        for (Item it : p.activeItems) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.row_equipment_compact, equipmentContainer, false);
            TextView name = row.findViewById(R.id.tvItemName);
            ImageView icon = row.findViewById(R.id.ivItemIcon);
            name.setText(it.name);
            int res = getResources().getIdentifier(it.imageResName == null ? "sword" : it.imageResName, "drawable", requireContext().getPackageName());
            if (res != 0) icon.setImageResource(res);
            equipmentContainer.addView(row);
        }
        try { ivQr.setImageBitmap(generateQr(p.uid, 512)); } catch (Exception ignored) {}
    }

    private void loadBadgeForUser(String userId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long completed = doc.getLong("specialMissionsWon");
                        int count = (completed != null) ? completed.intValue() : 0;
                        TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.view_badge, badgesContainer, false);
                        tv.setText("Bedž: " + count);
                        badgesContainer.addView(tv);
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private Bitmap generateQr(String text, int size) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bm = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
        int w = bm.getWidth(), h = bm.getHeight();
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                bmp.setPixel(x, y, bm.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
        return bmp;
    }
}

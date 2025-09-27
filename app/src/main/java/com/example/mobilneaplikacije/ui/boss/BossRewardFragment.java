package com.example.mobilneaplikacije.ui.boss;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.ui.task.TaskListFragment;

import java.util.List;
import java.util.Random;
import java.util.Locale;

public class BossRewardFragment extends Fragment implements SensorEventListener {

    private static final String ARG_COINS = "coins";
    private static final String ARG_HAS_EQUIP = "hasEquipment";
    private static final String ARG_EQ_NAME = "eqName";
    private static final String ARG_EQ_TYPE = "eqType";

    private SensorManager sensorManager;
    private float accelCurrent;
    private float accelLast;
    private float shakeThreshold = 12f;
    private boolean chestOpened = false;

    private LottieAnimationView lavChest;
    private TextView tvCoins;
    private Button btnContinue;
    private LinearLayout llRewards;
    public static BossRewardFragment newInstance(int coins, boolean hasEquipment,
                                                 @Nullable String eqName, @Nullable String eqType) {
        BossRewardFragment fragment = new BossRewardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COINS, coins);
        args.putBoolean(ARG_HAS_EQUIP, hasEquipment);
        if (eqName != null) args.putString(ARG_EQ_NAME, eqName);
        if (eqType != null) args.putString(ARG_EQ_TYPE, eqType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boss_reward, container, false);

        lavChest = view.findViewById(R.id.lavChest);
        btnContinue = view.findViewById(R.id.btnContinue);
        llRewards = view.findViewById(R.id.llRewards);

        btnContinue.setVisibility(View.GONE);
        llRewards.setVisibility(View.GONE); // nema nagrada dok se ne otvori

        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);

        return view;
    }

    private void openChest() {
        chestOpened = true;
        lavChest.playAnimation();

        llRewards.removeAllViews();
        llRewards.setVisibility(View.VISIBLE);

        if (getArguments() != null) {
            int coins = getArguments().getInt(ARG_COINS, 0);
            boolean hasEquipment = getArguments().getBoolean(ARG_HAS_EQUIP, false);
            String eqName = getArguments().getString(ARG_EQ_NAME, null);
            String eqType = getArguments().getString(ARG_EQ_TYPE, null); // "WEAPON" / "CLOTHES"

            // 🔹 Prikaži coinse
            TextView coinView = new TextView(requireContext());
            coinView.setText("Osvojeno: x" + coins + " novčića");
            coinView.setTextColor(getResources().getColor(android.R.color.white));
            coinView.setTextSize(20f);
            llRewards.addView(coinView);

            // 🔹 Ako je pala oprema iz back-end transakcije, prikaži je (bez lokalne lutrije)
            if (hasEquipment && eqName != null && eqType != null) {
                int iconRes = resolveRewardIcon(eqType, eqName);

                LinearLayout itemRow = new LinearLayout(requireContext());
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setGravity(Gravity.CENTER);

                ImageView iv = new ImageView(requireContext());
                iv.setImageResource(iconRes);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(100, 100);
                lp.setMargins(0, 16, 16, 0);
                iv.setLayoutParams(lp);

                TextView tv = new TextView(requireContext());
                tv.setText("Nova oprema: " + eqName);
                tv.setTextColor(getResources().getColor(android.R.color.white));
                tv.setTextSize(18f);

                itemRow.addView(iv);
                itemRow.addView(tv);
                llRewards.addView(itemRow);
            }
        }

        new Handler().postDelayed(() -> btnContinue.setVisibility(View.VISIBLE), 3000);

        btnContinue.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        });
    }
    private int resolveRewardIcon(@Nullable String type, @Nullable String name) {
        String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if ("WEAPON".equals(t)) {
            if (n.contains("sword")) return R.drawable.sword;
            if (n.contains("bow")) return R.drawable.bow;
            if (n.contains("shield")) return R.drawable.shield;
            return R.drawable.sword;
        }
        if ("CLOTHES".equals(t)) {
            if (n.contains("gloves")) return R.drawable.gloves;
            if (n.contains("boots")) return R.drawable.boots;
            return R.drawable.gloves;
        }
        if ("POTION".equals(t)) {
            if (n.startsWith("potion_pp_")) {
                try {
                    String numStr = n.substring("potion_pp_".length());
                    int usIdx = numStr.indexOf('_');
                    if (usIdx >= 0) numStr = numStr.substring(0, usIdx);
                    int val = Integer.parseInt(numStr);
                    if (val == 40) return R.drawable.potion_40;
                    if (val == 20) return R.drawable.potion_20;
                    if (val == 10) return R.drawable.potion_10_perm;
                    if (val == 5) return R.drawable.potion_5_perm;
                } catch (Exception ignored) {}
            }
        }
        return R.drawable.ic_shop_placeholder;
    }

    @Override
    public void onResume() {
        super.onResume();
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !chestOpened) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accelLast = accelCurrent;
            accelCurrent = (float) Math.sqrt((x * x + y * y + z * z));
            float delta = accelCurrent - accelLast;

            if (delta > shakeThreshold) {
                openChest();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

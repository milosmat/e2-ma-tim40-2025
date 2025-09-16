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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.manager.SessionManager;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.ui.task.TaskListFragment;

import java.util.List;
import java.util.Random;

public class BossRewardFragment extends Fragment implements SensorEventListener {

    private static final String ARG_COINS = "coins";
    private static final String ARG_EQUIPMENT = "equipment";

    private SensorManager sensorManager;
    private float accelCurrent;
    private float accelLast;
    private float shakeThreshold = 12f;
    private boolean chestOpened = false;

    private LottieAnimationView lavChest;
    private TextView tvCoins;
    private Button btnContinue;
    private LinearLayout llRewards;
    public static BossRewardFragment newInstance(int coins, boolean hasEquipment) {
        BossRewardFragment fragment = new BossRewardFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COINS, coins);
        args.putBoolean(ARG_EQUIPMENT, hasEquipment);
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
            boolean hasEquipment = getArguments().getBoolean(ARG_EQUIPMENT, false);

            SessionManager session = new SessionManager(requireContext());
            Player player = session.getPlayer();
            player.setCoins(player.getCoins() + coins);
            //player.setXp(player.getXp() + 100); // test XP ne treba za finis

            // 🔹 Prikaži coinse
            TextView coinView = new TextView(requireContext());
            coinView.setText("Osvojeno: x" + coins + " novčića");
            coinView.setTextColor(getResources().getColor(android.R.color.white));
            coinView.setTextSize(20f);
            llRewards.addView(coinView);

            // 🔹 Ako je pala oprema
            if (hasEquipment) {
                List<Equipment> inventory = session.getInventory();
                Random rand = new Random();
                Equipment drop;
                int iconRes;
                if (rand.nextInt(100) < 5) {
                    drop = new Equipment("Mač (+5% PP)", Equipment.Type.WEAPON,
                            Equipment.Effect.INCREASE_PP, 0.05, -1, false);
                    iconRes = R.drawable.ic_sword;
                } else {
                    drop = new Equipment("Rukavice (+10% PP)", Equipment.Type.CLOTHES,
                            Equipment.Effect.INCREASE_PP, 0.1, 2, false);
                    iconRes = R.drawable.ic_gloves;
                }
                inventory.add(drop);
                session.saveInventory(inventory);

                // UI prikaz opreme
                LinearLayout itemRow = new LinearLayout(requireContext());
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setGravity(Gravity.CENTER);

                ImageView iv = new ImageView(requireContext());
                iv.setImageResource(iconRes);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(100, 100);
                lp.setMargins(0, 16, 16, 0);
                iv.setLayoutParams(lp);

                TextView tv = new TextView(requireContext());
                tv.setText("Nova oprema: " + drop.getName());
                tv.setTextColor(getResources().getColor(android.R.color.white));
                tv.setTextSize(18f);

                itemRow.addView(iv);
                itemRow.addView(tv);

                llRewards.addView(itemRow);
            }

            session.savePlayer(player);
        }

        // Prikaži Continue posle 5 sekundi
        new Handler().postDelayed(() -> btnContinue.setVisibility(View.VISIBLE), 5000);

        btnContinue.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        });
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

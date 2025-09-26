package com.example.mobilneaplikacije.ui.boss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.manager.BattleManager;
import com.example.mobilneaplikacije.data.manager.LevelManager;
import com.example.mobilneaplikacije.data.model.Boss;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;
import com.example.mobilneaplikacije.data.repository.PlayerRepository;
import com.example.mobilneaplikacije.data.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

public class BossFragment extends Fragment implements SensorEventListener {
    private SensorManager sensorManager;
    private float accelCurrent;
    private float accelLast;
    private float shakeThreshold = 12f; // koliko jako treba protresti
    private ImageView ivBoss;
    private ProgressBar pbBossHp, pbPlayerPp;
    private TextView tvSuccessRate, tvAttempts, tvBattleLog, tvCoins;
    private Button btnAttack;
    private LottieAnimationView lavChest;
    private View llRewards;

    private BattleManager battleManager;
    private Player player;
    private Boss boss;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivBoss = view.findViewById(R.id.ivBoss);
        pbBossHp = view.findViewById(R.id.pbBossHp);
        pbPlayerPp = view.findViewById(R.id.pbPlayerPp);
        tvSuccessRate = view.findViewById(R.id.tvSuccessRate);
        tvBattleLog = view.findViewById(R.id.tvBattleLog);
        tvCoins = view.findViewById(R.id.tvCoins);
        btnAttack = view.findViewById(R.id.btnAttack);
        lavChest = view.findViewById(R.id.lavChest);
        llRewards = view.findViewById(R.id.llRewards);

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;

        PlayerRepository playerRepo = new PlayerRepository();
        TaskRepository taskRepo = new TaskRepository(requireContext());

        playerRepo.refreshSuccessRate(taskRepo, new PlayerRepository.PlayerCallback() {
            @Override
            public void onSuccess(Player p) {
                player = p;
                boss = new Boss(player.getLevel(), 200, 200);
                battleManager = new BattleManager(player, boss, new ArrayList<>());
                initUi();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initUi() {
        pbBossHp.setMax(boss.getMaxHp());
        pbBossHp.setProgress(boss.getHp());

        pbPlayerPp.setMax(player.getPp());
        pbPlayerPp.setProgress(player.getPp());

        tvSuccessRate.setText("Šansa za pogodak: " + player.getSuccessRate() + "%");
        tvAttempts.setText("Pokušaji: " + battleManager.getAttemptsLeft() + "/" + battleManager.getMaxAttempts());
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accelLast = accelCurrent;
            accelCurrent = (float) Math.sqrt((x * x + y * y + z * z));
            float delta = accelCurrent - accelLast;

            if (delta > shakeThreshold) {
                // Pokreni napad
                handleAttack();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    private void handleAttack() {
        if (battleManager == null || boss == null || player == null) return;
        String result = battleManager.attack();

        if (result.contains("Uspešan napad")) {
            showBossHit();
        } else if (result.contains("Promašaj")) {
            showBossAttack();
        } else {
            showBossIdle();
        }

        // Update UI
        pbBossHp.setProgress(boss.getHp());
        tvAttempts.setText("Pokušaji: " + battleManager.getAttemptsLeft() + "/" + battleManager.getMaxAttempts());
        tvBattleLog.setText(result);

        if (battleManager.isFinished()) {
            sensorManager.unregisterListener(this);
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container,
                            BossRewardFragment.newInstance(battleManager.getFinalCoins(), battleManager.hasEquipment()))
                    .commit();
        }
    }

    private void showBossIdle() {
        ivBoss.setImageResource(R.drawable.boss_idle);
    }

    private void showBossHit() {
        ivBoss.setImageResource(R.drawable.boss_hit);
        ivBoss.postDelayed(() -> ivBoss.setImageResource(R.drawable.boss_idle), 500);
    }

    private void showBossAttack() {
        ivBoss.setImageResource(R.drawable.boss_attack);
        ivBoss.postDelayed(() -> ivBoss.setImageResource(R.drawable.boss_idle), 500);
    }
}

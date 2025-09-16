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
import com.example.mobilneaplikacije.data.manager.SessionManager;
import com.example.mobilneaplikacije.data.model.Boss;
import com.example.mobilneaplikacije.data.model.Equipment;
import com.example.mobilneaplikacije.data.model.Player;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boss, container, false);

        ivBoss = view.findViewById(R.id.ivBoss);
        pbBossHp = view.findViewById(R.id.pbBossHp);
        pbPlayerPp = view.findViewById(R.id.pbPlayerPp);
        tvSuccessRate = view.findViewById(R.id.tvSuccessRate);
        tvAttempts = view.findViewById(R.id.tvAttempts);
        tvBattleLog = view.findViewById(R.id.tvBattleLog);
        btnAttack = view.findViewById(R.id.btnAttack);
        lavChest = view.findViewById(R.id.lavChest);
        llRewards = view.findViewById(R.id.llRewards);
        tvCoins = view.findViewById(R.id.tvCoins);
        LinearLayout llActiveEquipment = view.findViewById(R.id.llActiveEquipment);

        // Dummy podaci (kasnije povezujemo sa bazom / logikom nivoa)
        SessionManager session = new SessionManager(requireContext());
        List<Equipment> active = session.getActiveEquipment();
        for (Equipment e : active) {
            ImageView icon = new ImageView(requireContext());
            icon.setLayoutParams(new LinearLayout.LayoutParams(80, 80));

            switch (e.getEffect()) {
                case INCREASE_PP: icon.setImageResource(R.drawable.ic_gloves); break;
                case INCREASE_SUCCESS: icon.setImageResource(R.drawable.ic_shield); break;
                case EXTRA_ATTACK: icon.setImageResource(R.drawable.ic_boots); break;
                case EXTRA_COINS: icon.setImageResource(R.drawable.ic_bow); break;
            }
            llActiveEquipment.addView(icon);
        }
        player = session.getPlayer();
        boss = session.getBossState(player); // povuci stanje bossa
        battleManager = new BattleManager(player, boss, active, session);

        // Init UI
        pbBossHp.setMax(boss.getMaxHp());
        pbBossHp.setProgress(boss.getHp());

        pbPlayerPp.setMax(100);
        pbPlayerPp.setProgress(player.getPp());

        tvSuccessRate.setText("Šansa za pogodak: " + player.getSuccessRate() + "%");
        tvAttempts.setText("Pokušaji: 5/5");

        // Napad klikom
        btnAttack.setOnClickListener(v -> handleAttack());

        // Klik na kovčeg (otvara nagrade)
        lavChest.setOnClickListener(v -> {
            lavChest.playAnimation();
            showRewards();
        });

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        accelCurrent = SensorManager.GRAVITY_EARTH;
        accelLast = SensorManager.GRAVITY_EARTH;

        return view;
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

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
    private void handleAttack() {
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
        tvAttempts.setText("Pokušaji: " + battleManager.getAttemptsLeft() + "/5");
        tvBattleLog.setText(result);

        if (battleManager.isFinished()) {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container,
                            BossRewardFragment.newInstance(battleManager.getFinalCoins(),
                                    battleManager.hasEquipment()))
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

    private void showRewards() {
        llRewards.setVisibility(View.VISIBLE);
        int coins = battleManager.getFinalCoins();
        tvCoins.setText("x" + coins);
        SessionManager session = new SessionManager(requireContext());
        // Ažuriraj coins
        player.setCoins(player.getCoins() + coins);

        //  Ako je boss poražen, možeš dodeliti i XP za prelazak nivoa
//  Ako je boss poražen, dodeli XP
        if (boss.isDefeated()) {
            //player.setXp(player.getXp() + 225); // test xp ne treba za finis
            session.clearBossState();
        }

// 🔹 Proveri level-up
        if (LevelManager.checkLevelUp(player)) {
            Toast.makeText(getContext(),
                    "LEVEL UP! Sada si nivo " + player.getLevel() +
                            " (" + player.getTitle() + ")",
                    Toast.LENGTH_LONG).show();
        }

// Sačuvaj igrača
        session.savePlayer(player);


        // Ako boss nije poražen, sačuvaj i njegovo trenutno stanje
        session.saveBossState(boss);
    }
}

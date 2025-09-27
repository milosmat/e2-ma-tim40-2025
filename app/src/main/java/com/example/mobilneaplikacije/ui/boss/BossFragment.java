package com.example.mobilneaplikacije.ui.boss;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.manager.BattleManager;
import com.example.mobilneaplikacije.data.model.ActiveItem;
import com.example.mobilneaplikacije.data.model.dto.AttackResult;
import com.example.mobilneaplikacije.data.model.dto.BattleState;
import com.example.mobilneaplikacije.data.model.dto.VictoryResult;
import com.example.mobilneaplikacije.data.repository.PlayerRepository;
import com.example.mobilneaplikacije.data.repository.TaskRepository;
import com.example.mobilneaplikacije.data.repository.InventoryRepository;
import com.example.mobilneaplikacije.ui.equipment.EquipmentSelectionFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class BossFragment extends Fragment implements SensorEventListener {

    private ImageView ivBoss;
    private ProgressBar pbBossHp, pbPlayerPp;
    private TextView tvBossHpValue, tvPlayerPpValue;
    private TextView tvSuccessRate, tvAttempts, tvBattleLog, tvCoins;
    private Button btnAttack;
    private Button btnChooseEquipment;
    private LottieAnimationView lavChest;
    private View llRewards;
    private ListenerRegistration battleStateReg;
    private BattleManager battleManager;
    private PlayerRepository playerRepo;
    private TaskRepository taskRepo;
    private InventoryRepository inventoryRepo;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12f;
    private long lastShakeMs = 0L;

    private long playerPP = 0L;
    private int hitChance = 0;
    private int bossMaxHpUi = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boss, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivBoss = view.findViewById(R.id.ivBoss);
        pbBossHp = view.findViewById(R.id.pbBossHp);
        pbPlayerPp = view.findViewById(R.id.pbPlayerPp);
        tvBossHpValue = view.findViewById(R.id.tvBossHpValue);
        tvPlayerPpValue = view.findViewById(R.id.tvPlayerPpValue);
        tvSuccessRate = view.findViewById(R.id.tvSuccessRate);
        tvBattleLog = view.findViewById(R.id.tvBattleLog);
        tvCoins = view.findViewById(R.id.tvCoins);
        btnAttack = view.findViewById(R.id.btnAttack);
        btnChooseEquipment = view.findViewById(R.id.btnChooseEquipment);
        lavChest = view.findViewById(R.id.lavChest);
        llRewards = view.findViewById(R.id.llRewards);
        tvAttempts = view.findViewById(R.id.tvAttempts);

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        playerRepo = new PlayerRepository();
        taskRepo = new TaskRepository();
        battleManager = new BattleManager();
        inventoryRepo = new InventoryRepository();

        battleManager.loadOrInit(0L, new BattleManager.Callback<BattleState>() {
            @Override public void onSuccess(@Nullable BattleState state) {
                if (state == null) return;

                bindStateToUi(state);

                playerRepo.getCurrentPP(new PlayerRepository.LongCallback() {
                    @Override public void onResult(long v) {
                        inventoryRepo.getCombatBonuses(new InventoryRepository.Callback<InventoryRepository.CombatBonuses>() {
                            @Override public void onSuccess(InventoryRepository.CombatBonuses bonuses) {
                                playerPP = Math.round(v * Math.max(1.0, bonuses.ppMultiplier));
                                int adjHit = Math.max(0, Math.min(100, hitChance + bonuses.successAddPct));
                                hitChance = adjHit;
                                pbPlayerPp.setMax((int) Math.max(1, playerPP));
                                pbPlayerPp.setProgress((int) playerPP);
                                tvPlayerPpValue.setText(playerPP + " PP");
                                tvSuccessRate.setText("Sansa za pogodak: " + hitChance + "%");
                            }
                            @Override public void onError(Exception e) {
                                playerPP = v;
                                pbPlayerPp.setMax((int) Math.max(1, v));
                                pbPlayerPp.setProgress((int) v);
                                tvPlayerPpValue.setText(v + " PP");
                            }
                        });
                    }
                    @Override public void onError(Exception e) {
                        playerPP = 0L;
                        pbPlayerPp.setMax(1);
                        pbPlayerPp.setProgress(0);
                    }
                });

                if (battleStateReg != null) battleStateReg.remove();
                battleStateReg = battleManager.addStateListener(new BattleManager.Callback<BattleState>() {
                    @Override public void onSuccess(@Nullable BattleState s) { bindStateToUi(s); }
                    @Override public void onError(Exception e) {  }
                });

                btnAttack.setOnClickListener(v -> doAttack());
                btnChooseEquipment.setOnClickListener(v -> {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new EquipmentSelectionFragment())
                            .addToBackStack(null)
                            .commit();
                });

                inventoryRepo.listActive(new InventoryRepository.Callback<List<ActiveItem>>() {
                    @Override public void onSuccess(List<ActiveItem> activeItemList) {
                        View container = requireView().findViewById(R.id.llActiveEquipment);
                        if (!(container instanceof ViewGroup)) return;
                        ViewGroup vg = (ViewGroup) container;
                        vg.removeAllViews();
                        if (activeItemList == null || activeItemList.isEmpty()) {
                            TextView tv = new TextView(requireContext());
                            tv.setText("Nema aktivnih itema");
                            vg.addView(tv);
                            return;
                        }
                        for (var a : activeItemList) {
                            TextView tv = new TextView(requireContext());
                            tv.setText(a.itemId + " (" + (a.remainingBattles <= 0 ? "trajni" : (a.remainingBattles + " borbe")) + ")");
                            tv.setBackgroundResource(R.drawable.bg_item_equipment);
                            tv.setPadding(12, 8, 12, 8);
                            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(8, 8, 8, 8);
                            vg.addView(tv, lp);
                        }
                    }
                    @Override public void onError(Exception e) { }
                });
            }
            @Override public void onError(Exception e) {
                Toast.makeText(requireContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (battleStateReg != null) {
            battleStateReg.remove();
            battleStateReg = null;
        }
    }
    private void bindStateToUi(BattleState state) {
        if (state == null || getView() == null) return;
        bossMaxHpUi = (int) state.bossMaxHp;

        pbBossHp.setMax(bossMaxHpUi);
        pbBossHp.setProgress((int) state.bossHp);
        tvBossHpValue.setText(state.bossHp + " / " + state.bossMaxHp + " HP");

        tvAttempts.setText("Pokušaji: " + state.attemptsLeft + "/5");

        hitChance = state.hitChance;
        tvSuccessRate.setText("Šansa za pogodak: " + hitChance + "%");

        long baseCoins = BattleManager.coinsForIndex(state.currentBossIndex);
        long previewCoins = state.halvedReward ? Math.round(baseCoins / 2.0) : baseCoins;
        int gearPct = state.halvedReward ? 10 : 20;
        tvCoins.setText("Potencijalno: " + previewCoins + " novčića • Oprema: " + gearPct + "%");
    }
    private void doAttack() {
        btnAttack.setEnabled(false);
        battleManager.performAttack(hitChance, playerPP, new BattleManager.Callback<AttackResult>() {
            @Override
            public void onSuccess(@Nullable AttackResult ar) {
                btnAttack.setEnabled(true);
                if (ar == null) return;

                if (ar.hit) {
                    showBossHit();
                    tvBattleLog.setText(getString(R.string.boss_hit, ar.damageApplied));
                } else {
                    showBossAttack();
                    tvBattleLog.setText(getString(R.string.boss_miss));
                }

                pbBossHp.setProgress((int) ar.bossHpAfter);
                tvBossHpValue.setText(ar.bossHpAfter + " / " + bossMaxHpUi + " HP");
                tvAttempts.setText("Pokušaji: " + ar.attemptsLeftAfter + "/5");

                if (ar.bossDefeated) {
                    battleManager.resolveVictoryAndAdvance(new BattleManager.Callback<VictoryResult>() {
                        @Override public void onSuccess(@Nullable VictoryResult vr) {
                            inventoryRepo.updateDurationOfActiveItems(new InventoryRepository.Callback<Void>() {
                                @Override public void onSuccess(Void data) { }
                                @Override public void onError(Exception e) { }
                            });
                            // ako dropuje item oruzija promeni sanse
                            if (vr != null && vr.equipmentDropped && "WEAPON".equals(vr.equipmentType)) {
                                String droppedId = vr.equipmentItemId;
                                if (droppedId != null) {
                                    inventoryRepo.onBossWeaponDrop(droppedId, new InventoryRepository.Callback<Void>() {
                                        @Override public void onSuccess(Void data) { }
                                        @Override public void onError(Exception e) { }
                                    });
                                }
                            }
                            unregisterShake();
                            long coins = (vr == null) ? 0 : vr.coinsAwarded;

                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container,
                                            BossRewardFragment.newInstance(
                                                    (int) coins,
                                                    vr != null && vr.equipmentDropped,
                                                    vr != null ? vr.equipmentName : null,
                                                    vr != null ? vr.equipmentType : null
                                            )
                                    )
                                    .commit();
                        }
                        @Override public void onError(Exception e) {
                            Toast.makeText(requireContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (ar.fightEnded) {
                    inventoryRepo.updateDurationOfActiveItems(new InventoryRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) { }
                        @Override public void onError(Exception e) { }
                    });
                    Toast.makeText(requireContext(),
                            "Okršaj završen. Nastavljaš nakon sledećeg pređenog nivoa.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {
                btnAttack.setEnabled(true);
                Toast.makeText(requireContext(), "Greška: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showBossIdle() {
        ivBoss.setImageResource(R.drawable.boss_idle);
    }

    private void showBossHit() {
        ivBoss.setImageResource(R.drawable.boss_hit);
        ivBoss.postDelayed(this::showBossIdle, 500);
    }

    private void showBossAttack() {
        ivBoss.setImageResource(R.drawable.boss_attack);
        ivBoss.postDelayed(this::showBossIdle, 500);
    }


    @Override
    public void onResume() {
        super.onResume();
        registerShake();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterShake();
    }

    private void registerShake() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterShake() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double magnitude = Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;
        long now = System.currentTimeMillis();
        if (magnitude > SHAKE_THRESHOLD && (now - lastShakeMs) > 600) {
            lastShakeMs = now;
            doAttack();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

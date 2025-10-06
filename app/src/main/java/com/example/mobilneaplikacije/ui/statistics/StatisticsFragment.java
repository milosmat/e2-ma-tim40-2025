
package com.example.mobilneaplikacije.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.mobilneaplikacije.R;
import com.example.mobilneaplikacije.data.model.Statistics;
import com.example.mobilneaplikacije.data.repository.StatisticsRepository;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {
    private TextView tvConsecutiveDays, tvLongestStreak, tvMissionsStarted, tvMissionsCompleted;
    private PieChart chartTaskStatus;
    private BarChart chartTasksByCategory;
    private LineChart chartDifficulty;
    private LineChart chartXpLast7Days;
    private ProgressBar progressBar;
    private StatisticsRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_statistics, container, false);
        tvConsecutiveDays = v.findViewById(R.id.tvConsecutiveDays);
        tvLongestStreak = v.findViewById(R.id.tvLongestStreak);
        tvMissionsStarted = v.findViewById(R.id.tvMissionsStarted);
        tvMissionsCompleted = v.findViewById(R.id.tvMissionsCompleted);
        chartTaskStatus = v.findViewById(R.id.chartTaskStatus);
        chartTasksByCategory = v.findViewById(R.id.chartTasksByCategory);
        chartDifficulty = v.findViewById(R.id.chartDifficulty);
        chartXpLast7Days = v.findViewById(R.id.chartXpLast7Days);
        progressBar = v.findViewById(R.id.progressBar);
        repo = new StatisticsRepository();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void loadStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        repo.loadStatistics(new StatisticsRepository.Callback<Statistics>() {
            @Override
            public void onSuccess(@Nullable Statistics stats) {
                if (!isAdded() || stats == null) return;
                progressBar.setVisibility(View.GONE);
                displayStatistics(stats);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Greska pri ucitavanju statistike", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayStatistics(Statistics stats) {
        tvConsecutiveDays.setText(String.valueOf(stats.consecutiveActiveDays));
        tvLongestStreak.setText(stats.longestCompletionStreak + " dana");
        tvMissionsStarted.setText(String.valueOf(stats.specialMissionsStarted));
        tvMissionsCompleted.setText(String.valueOf(stats.specialMissionsCompleted));
        setupTaskStatusChart(stats);
        setupCategoryChart(stats.tasksByCategory);
        setupDifficultyXpChart(stats.tasksByDifficulty);
        setupXpChart(stats.xpByDay);
    }

    private void setupTaskStatusChart(Statistics stats) {
        List<PieEntry> entries = new ArrayList<>();
        if (stats.totalCompletedTasks > 0) entries.add(new PieEntry(stats.totalCompletedTasks, "Uradjeni"));
        if (stats.totalMissedTasks > 0) entries.add(new PieEntry(stats.totalMissedTasks, "Neuradjeni"));
        if (stats.totalCancelledTasks > 0) entries.add(new PieEntry(stats.totalCancelledTasks, "Otkazani"));
        int active = stats.totalCreatedTasks - stats.totalCompletedTasks - stats.totalMissedTasks - stats.totalCancelledTasks;
        if (active > 0) entries.add(new PieEntry(active, "Aktivni"));
        if (entries.isEmpty()) {
            chartTaskStatus.setNoDataText("Nema podataka");
            chartTaskStatus.invalidate();
            return;
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.rgb(76, 175, 80), Color.rgb(244, 67, 54),
                Color.rgb(158, 158, 158), Color.rgb(33, 150, 243)});
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        chartTaskStatus.setData(data);
        chartTaskStatus.getDescription().setEnabled(false);
        chartTaskStatus.setDrawHoleEnabled(true);
        chartTaskStatus.setHoleRadius(40f);
        chartTaskStatus.setTransparentCircleRadius(45f);
        chartTaskStatus.setEntryLabelTextSize(11f);
        chartTaskStatus.setEntryLabelColor(Color.BLACK);
        Legend legend = chartTaskStatus.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        chartTaskStatus.animateY(1000);
        chartTaskStatus.invalidate();
    }

    private void setupCategoryChart(Map<String, Integer> categoryMap) {
        if (categoryMap == null || categoryMap.isEmpty()) {
            chartTasksByCategory.setNoDataText("Nema podataka");
            chartTasksByCategory.invalidate();
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(categoryMap.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, Integer> entry : sorted) {
            entries.add(new BarEntry(index++, entry.getValue()));
            labels.add(entry.getKey());
        }
        BarDataSet dataSet = new BarDataSet(entries, "Broj zavrsenih zadataka");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(10f);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);
        chartTasksByCategory.setData(data);
        chartTasksByCategory.getDescription().setEnabled(false);
        chartTasksByCategory.setFitBars(true);
        XAxis xAxis = chartTasksByCategory.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45);
        chartTasksByCategory.getAxisLeft().setAxisMinimum(0f);
        chartTasksByCategory.getAxisRight().setEnabled(false);
        chartTasksByCategory.animateY(1000);
        chartTasksByCategory.invalidate();
    }

    private void setupDifficultyXpChart(Map<String, Integer> xpByDifficulty) {
        if (xpByDifficulty == null || xpByDifficulty.isEmpty()) {
            chartDifficulty.setNoDataText("Nema podataka");
            chartDifficulty.invalidate();
            return;
        }
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String[] difficulties = {"VEOMA_LAK", "LAK", "TEZAK", "EKSTREMNO_TEZAK"};
        String[] diffLabels = {"Veoma lak", "Lak", "Težak", "Ekstremno težak"};
        for (int i = 0; i < difficulties.length; i++) {
            int xp = xpByDifficulty.containsKey(difficulties[i]) ? xpByDifficulty.get(difficulties[i]) : 0;
            entries.add(new Entry(i, xp));
            labels.add(diffLabels[i]);
        }
        LineDataSet dataSet = new LineDataSet(entries, "Osvojeni XP");
        dataSet.setColor(Color.rgb(255, 152, 0));
        dataSet.setCircleColor(Color.rgb(255, 152, 0));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(11f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(255, 152, 0));
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineData data = new LineData(dataSet);
        chartDifficulty.setData(data);
        chartDifficulty.getDescription().setEnabled(false);
        XAxis xAxis = chartDifficulty.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-20);
        chartDifficulty.getAxisLeft().setAxisMinimum(0f);
        chartDifficulty.getAxisRight().setEnabled(false);
        Legend legend = chartDifficulty.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        chartDifficulty.animateX(1200, Easing.EaseInOutQuad);
        chartDifficulty.invalidate();
    }

    private void setupXpChart(Map<String, Double> xpMap) {
        if (xpMap == null || xpMap.isEmpty()) {
            chartXpLast7Days.setNoDataText("Nema podataka");
            chartXpLast7Days.invalidate();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM", Locale.US);
        Calendar cal = Calendar.getInstance();
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);
            String dayKey = sdf.format(dayCal.getTime());
            double xp = xpMap.containsKey(dayKey) ? xpMap.get(dayKey) : 0.0;
            entries.add(new Entry(6 - i, (float) xp));
            labels.add(displayFormat.format(dayCal.getTime()));
        }
        LineDataSet dataSet = new LineDataSet(entries, "XP");
        dataSet.setColor(Color.rgb(33, 150, 243));
        dataSet.setCircleColor(Color.rgb(33, 150, 243));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(33, 150, 243));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineData data = new LineData(dataSet);
        chartXpLast7Days.setData(data);
        chartXpLast7Days.getDescription().setEnabled(false);
        XAxis xAxis = chartXpLast7Days.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        chartXpLast7Days.getAxisLeft().setAxisMinimum(0f);
        chartXpLast7Days.getAxisRight().setEnabled(false);
        Legend legend = chartXpLast7Days.getLegend();
        legend.setEnabled(false);
        chartXpLast7Days.animateX(1000);
        chartXpLast7Days.invalidate();
    }


}



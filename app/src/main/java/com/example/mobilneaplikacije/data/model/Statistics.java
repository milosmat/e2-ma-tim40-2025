package com.example.mobilneaplikacije.data.model;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
    public int consecutiveActiveDays;
    public int totalCreatedTasks;
    public int totalCompletedTasks;
    public int totalMissedTasks;
    public int totalCancelledTasks;
    public int longestCompletionStreak;
    public Map<String, Integer> tasksByCategory = new HashMap<>();
    public Map<String, Double> xpByDay = new HashMap<>();
    public Map<String, Integer> tasksByDifficulty = new HashMap<>(); // XP po težini
    public int specialMissionsStarted;
    public int specialMissionsCompleted;
    
    public Statistics() {}
}

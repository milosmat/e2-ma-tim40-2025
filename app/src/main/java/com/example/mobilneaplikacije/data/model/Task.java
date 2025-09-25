package com.example.mobilneaplikacije.data.model;

public class Task {
    private String idHash;
    private String categoryIdHash;

    private String title;
    private String description;

    private boolean isRecurring;
    private int repeatInterval;
    private String repeatUnit;
    private long startDate;
    private long endDate;


    // difficulty: "VEOMA_LAK" | "LAK" | "TEZAK" | "EKSTREMNO_TEZAK"
    // importance: "NORMALAN" | "VAŽAN" | "EKSTREMNO_VAŽAN" | "SPECIJALAN"
    private String difficulty;
    private String importance;

    private int xpPoints;
    // status: "ACTIVE" | "DONE" | "MISSED" | "PAUSED" | "CANCELLED"
    private String status;

    // Za jednokratne
    private long dueDateTime;

    public Task() {}

    // Getteri/setteri
    public String getIdHash() { return idHash; }
    public void setIdHash(String idHash) { this.idHash = idHash; }

    public String getCategoryIdHash() { return categoryIdHash; }
    public void setCategoryIdHash(String categoryIdHash) { this.categoryIdHash = categoryIdHash; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    public int getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(int repeatInterval) { this.repeatInterval = repeatInterval; }

    public String getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(String repeatUnit) { this.repeatUnit = repeatUnit; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }

    public int getXpPoints() { return xpPoints; }
    public void setXpPoints(int xpPoints) { this.xpPoints = xpPoints; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDueDateTime() { return dueDateTime; }
    public void setDueDateTime(long dueDateTime) { this.dueDateTime = dueDateTime; }
}

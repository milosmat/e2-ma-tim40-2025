package com.example.mobilneaplikacije.data.model;

public class Task {
    private long id;
    private String title;
    private String description;
    private long categoryId;

    // Učestalost
    private boolean isRecurring;
    private int repeatInterval;   // npr. 1, 2, 3…
    private String repeatUnit;    // "DAY", "WEEK"
    private long startDate;       // millis
    private long endDate;         // millis

    // Težina i bitnost
    private String difficulty;    // "EASY", "HARD"...
    private String importance;    // "NORMAL", "SPECIAL"...

    private int xpPoints;         // ukupno XP = težina + bitnost
    private String status;        // "NEW", "DONE", "CANCELLED", "PAUSED"

    private long dueDateTime;     // millis za jednokratne zadatke

    // Prazan konstruktor
    public Task() {}

    // Konstruktor sa svim poljima
    public Task(long id, String title, String description, long categoryId,
                boolean isRecurring, int repeatInterval, String repeatUnit,
                long startDate, long endDate,
                String difficulty, String importance,
                int xpPoints, String status, long dueDateTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.isRecurring = isRecurring;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.difficulty = difficulty;
        this.importance = importance;
        this.xpPoints = xpPoints;
        this.status = status;
        this.dueDateTime = dueDateTime;
    }

    // Getteri i setteri
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

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

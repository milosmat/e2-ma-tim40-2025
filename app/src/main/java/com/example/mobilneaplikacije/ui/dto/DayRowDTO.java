package com.example.mobilneaplikacije.ui.dto;

import androidx.annotation.Nullable;

import com.example.mobilneaplikacije.data.model.Task;
import com.example.mobilneaplikacije.data.model.TaskOccurrence;

public class DayRowDTO {
    public final Task task;
    @Nullable
    public final TaskOccurrence occ;

    public DayRowDTO(Task task, @Nullable TaskOccurrence occ) {
        this.task = task;
        this.occ = occ;
    }
}
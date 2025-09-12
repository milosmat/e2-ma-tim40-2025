package com.example.mobilneaplikacije.util;

import com.example.mobilneaplikacije.data.model.Task;
import java.util.ArrayList;
import java.util.List;

public class TaskUtils {

    public static List<Long> generateOccurrencesBetween(Task t, long fromMillis, long toMillis, int maxCount) {
        java.util.ArrayList<Long> out = new java.util.ArrayList<>();
        if (!t.isRecurring()) {
            if (t.getDueDateTime() >= fromMillis && t.getDueDateTime() <= toMillis) {
                out.add(t.getDueDateTime());
            }
            return out;
        }
        if (t.getStartDate() <= 0 || t.getEndDate() <= 0 || t.getRepeatInterval() <= 0) return out;

        long step = "DAY".equals(t.getRepeatUnit())
                ? t.getRepeatInterval() * 24L*60*60*1000
                : t.getRepeatInterval() * 7L*24*60*60*1000;

        // Kreni od prvog pojavljivanja >= fromMillis
        long cur = t.getStartDate();
        if (cur < fromMillis) {
            long diff = fromMillis - cur;
            long jumps = diff / step;
            cur += jumps * step;
            while (cur < fromMillis) cur += step; // osiguraj
        }

        while (cur <= t.getEndDate() && cur <= toMillis) {
            if (cur >= fromMillis) out.add(cur);
            if (out.size() >= maxCount) break;
            cur += step;
        }
        return out;
    }

}

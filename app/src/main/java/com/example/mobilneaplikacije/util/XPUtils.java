package com.example.mobilneaplikacije.util;

public class XPUtils {

    public static int calculateXP(String difficulty, String importance) {
        int difficultyXP = 0;
        switch (difficulty) {
            case "VEOMA_LAK":
                difficultyXP = 1;
                break;
            case "LAK":
                difficultyXP = 3;
                break;
            case "TEZAK":
                difficultyXP = 7;
                break;
            case "EKSTREMNO_TEZAK":
                difficultyXP = 20;
                break;
            default:
                difficultyXP = 0;
        }

        int importanceXP = 0;
        switch (importance) {
            case "NORMALAN":
                importanceXP = 1;
                break;
            case "VAZAN":
                importanceXP = 3;
                break;
            case "EKSTREMNO_VAZAN":
                importanceXP = 10;
                break;
            case "SPECIJALAN":
                importanceXP = 100;
                break;
            default:
                importanceXP = 0;
        }

        return difficultyXP + importanceXP;
    }

    public static boolean canEarnXP(String difficulty, String importance, int countToday, int countThisWeek, int countThisMonth) {
        if (difficulty.equals("VEOMA_LAK") && importance.equals("NORMALAN")) {
            return countToday < 5;
        }
        if (difficulty.equals("LAK") && importance.equals("VAZAN")) {
            return countToday < 5;
        }
        if (difficulty.equals("TEZAK") && importance.equals("EKSTREMNO_VAZAN")) {
            return countToday < 2;
        }
        if (difficulty.equals("EKSTREMNO_TEZAK")) {
            return countThisWeek < 1;
        }
        if (importance.equals("SPECIJALAN")) {
            return countThisMonth < 1;
        }
        return true;
    }
}

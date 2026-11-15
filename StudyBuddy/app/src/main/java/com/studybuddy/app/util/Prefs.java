package com.studybuddy.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.TimeZone;

/** XP, level and daily streak. */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences("studybuddy", Context.MODE_PRIVATE);
    }

    public int xp() {
        return sp.getInt("xp", 0);
    }

    public void addXp(int amount) {
        sp.edit().putInt("xp", xp() + amount).apply();
    }

    public int level() {
        return xp() / 100 + 1;
    }

    public int xpIntoLevel() {
        return xp() % 100;
    }

    private static long today() {
        long now = System.currentTimeMillis();
        return (now + TimeZone.getDefault().getOffset(now)) / 86400000L;
    }

    /** Current streak in days (0 if the streak is broken). */
    public int streak() {
        long last = sp.getLong("lastDay", -1);
        long t = today();
        if (last == t || last == t - 1) {
            return sp.getInt("streak", 0);
        }
        return 0;
    }

    public boolean studiedToday() {
        return sp.getLong("lastDay", -1) == today();
    }

    public void markStudiedToday() {
        long last = sp.getLong("lastDay", -1);
        long t = today();
        if (last == t) {
            return;
        }
        int next = (last == t - 1) ? sp.getInt("streak", 0) + 1 : 1;
        sp.edit().putLong("lastDay", t).putInt("streak", next).apply();
    }
}

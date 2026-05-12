package com.bro.broreminder.utils;

import java.util.Calendar;

public class ScheduleUtils {
    private static final int[] HOURS = {9, 12, 15, 18, 21};

    public static long nextReminderTime() {
        return nextReminderTimeFrom(System.currentTimeMillis());
    }

    public static long nextReminderTimeFrom(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        for (int hour : HOURS) {
            if (cal.get(Calendar.HOUR_OF_DAY) < hour) {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTimeInMillis();
            }
        }
        cal.add(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, HOURS[0]);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
    public static long nextReminderTimeAfter(long time) {
        return nextReminderTimeFrom(time + 1000);
    }
    public static long nextReportTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 22);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DATE, 1);
        }
        return cal.getTimeInMillis();
    }
    public static long nextEmailTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 30);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DATE, 1);
        }
        return cal.getTimeInMillis();
    }
}

package com.bro.broreminder.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
import android.util.Log;

import com.bro.broreminder.receiver.ReminderReceiver;

public class ReminderScheduler {

    /** Controls whether a debug alarm is triggered 5 seconds after enabling a
     *  reminder. Set to {@code false} to disable debug alarms. */
    public static boolean ENABLE_DEBUG_TIMER = true;
    public static void schedule(Context ctx, Reminder r) {
        cancel(ctx, r); // clear existing
        if (r.oneTime) {
            scheduleOneTime(ctx, r);
            return;
        }
        if (r.times == null || r.days == null) return;
        Log.d("BroReminder", "Scheduling reminder " + r.id);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (int d = 0; d < 7; d++) {
            if (!r.days[d]) continue;
            for (int i = 0; i < r.times.length; i++) {
                if (r.timeEnabled == null || i >= r.timeEnabled.length || r.timeEnabled[i]) {
                    long trigger = nextOccurrence(d, r.times[i]);
                    Log.d("BroReminder", " -> day " + d + " time " + r.times[i] + " at " + trigger);
                    PendingIntent pi = pending(ctx, r, d, i);
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
                }
            }
        }
    }

    public static void cancel(Context ctx, Reminder r) {
        Log.d("BroReminder", "Canceling reminder " + r.id);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (r.times != null) {
            for (int d = 0; d < 7; d++) {
                for (int i = 0; i < r.times.length; i++) {
                    PendingIntent pi = pending(ctx, r, d, i);
                    am.cancel(pi);
                }
            }
        }
        if (r.oneTime) {
            Intent intent = new Intent(ctx, ReminderReceiver.class);
            intent.putExtra("reminderId", r.id);
            int request = ("oneTime_" + r.id).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(ctx, request, intent, PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        }
    }

    private static PendingIntent pending(Context ctx, Reminder r, int day, int timeIdx) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("reminderId", r.id);
        intent.putExtra("day", day);
        intent.putExtra("timeIdx", timeIdx);
        int request = (r.id + "_" + day + "_" + timeIdx).hashCode();
        return PendingIntent.getBroadcast(ctx, request, intent, PendingIntent.FLAG_IMMUTABLE);
    }
    public static void scheduleNext(Context ctx, Reminder r, int day, int timeIdx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        long trigger = nextOccurrence(day, r.times[timeIdx]);
        Log.d("BroReminder", "Rescheduling reminder " + r.id + " day " + day + " time " + r.times[timeIdx] + " at " + trigger);
        PendingIntent pi = pending(ctx, r, day, timeIdx);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    public static void scheduleOneTime(Context ctx, Reminder r) {
        Log.d("BroReminder", "Scheduling one-time reminder " + r.id + " at " + r.oneTimeDate);
        if (r.oneTimeDate <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("reminderId", r.id);
        int request = ("oneTime_" + r.id).hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(ctx, request, intent, PendingIntent.FLAG_IMMUTABLE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.oneTimeDate, pi);
    }

    /**
     * Schedule a one time alarm 5 seconds from now for debugging purposes.
     * This sends a broadcast to {@link ReminderReceiver} without day/time extras
     * so that it will not automatically reschedule itself.
     */
    public static void debugTriggerSoon(Context ctx, Reminder r) {
        if (!ENABLE_DEBUG_TIMER) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.putExtra("reminderId", r.id);
        int request = ("debug_" + r.id).hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(ctx, request, intent, PendingIntent.FLAG_IMMUTABLE);
        long trigger = System.currentTimeMillis() + 5000;
        Log.d("BroReminder", "Scheduling debug reminder for " + r.id + " at " + trigger);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    /**
     * Compute the next occurrence of the given day/time combination after the
     * current moment. This mirrors the logic used for scheduling alarms.
     */
    public static long computeNextOccurrence(int dayOfWeek, String time) {
        return nextOccurrence(dayOfWeek, time);
    }
    private static long nextOccurrence(int dayOfWeek, String time) {
        int h;
        int m;
        try {
            String[] parts = time.split(":" );
            if (parts.length != 2) throw new IllegalArgumentException("bad format");
            h = Integer.parseInt(parts[0]);
            m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException("out of range");
        } catch (Exception e) {
            Log.e("BroReminder", "Invalid time string: " + time, e);
            return System.currentTimeMillis() + 60000;
        }

        Calendar now = Calendar.getInstance();

        int currentDay = now.get(Calendar.DAY_OF_WEEK);

        int targetDay = (Calendar.MONDAY - 1 + dayOfWeek) % 7 + 1; // convert to Calendar's 1-7 range

        int diff = targetDay - currentDay;
        if (diff < 0) diff += 7;
        now.add(Calendar.DAY_OF_MONTH, diff);

        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.set(Calendar.HOUR_OF_DAY, h);
        now.set(Calendar.MINUTE, m);

        if (now.getTimeInMillis() <= System.currentTimeMillis()) {
            now.add(Calendar.DAY_OF_MONTH, 7);
        }
        return now.getTimeInMillis();
    }
}
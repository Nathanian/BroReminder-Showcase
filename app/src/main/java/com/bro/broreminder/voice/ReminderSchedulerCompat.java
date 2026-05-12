package com.bro.broreminder.voice;

import android.content.Context;

import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderScheduler;

/**
 * Minimal adapter to schedule reminders using existing logic.
 */
public final class ReminderSchedulerCompat {
    private ReminderSchedulerCompat() {}

    public static void schedule(Context ctx, Reminder r) {
        ReminderScheduler.scheduleOneTime(ctx, r);
    }
}

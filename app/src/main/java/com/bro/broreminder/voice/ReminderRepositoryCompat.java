package com.bro.broreminder.voice;

import android.content.Context;
import android.content.Intent;

import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderStorage;
import com.bro.broreminder.reminder.ReminderEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal adapter to persist reminders using existing storage.
 */
public final class ReminderRepositoryCompat {
    private ReminderRepositoryCompat() {}

    public static boolean add(Context ctx, Reminder r) {
        List<Reminder> list = ReminderStorage.load(ctx);
        if (list == null) list = new ArrayList<>();
        list.add(r);
        if (ReminderStorage.save(ctx, list)) {
            // Notify running UI to reload reminders so the newly created one-time
            // reminder appears in the list view for editing and deletion.
            Intent changed = new Intent(ReminderEvents.ACTION_CHANGED);
            changed.setPackage(ctx.getPackageName());
            ctx.sendBroadcast(changed);
            return true;
        }
        return false;
    }
}

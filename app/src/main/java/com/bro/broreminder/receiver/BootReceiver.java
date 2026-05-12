package com.bro.broreminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderScheduler;
import com.bro.broreminder.reminder.ReminderStorage;
import com.bro.broreminder.utils.ScheduleUtils;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            List<Reminder> list = ReminderStorage.load(context);
            for (Reminder r : list) {
                if (r.enabled) {
                    Log.d("BroReminder", "Boot scheduling reminder " + r.id);
                    ReminderScheduler.schedule(context, r);
                }
            }
            DailyReportReceiver.scheduleReport(
                    context,
                    ScheduleUtils.nextReportTime() - System.currentTimeMillis());
            EmailReportReceiver.scheduleEmail(
                    context,
                    ScheduleUtils.nextEmailTime() - System.currentTimeMillis());
        }
    }
}

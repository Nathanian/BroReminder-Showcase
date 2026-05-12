package com.bro.broreminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bro.broreminder.utils.OverlayService;
import com.bro.broreminder.reminder.ReminderScheduler;
import com.bro.broreminder.reminder.ReminderStorage;
import com.bro.broreminder.reminder.Reminder;


/** Utility BroadcastReceiver that shows the reminder dialog and also provides
 *  a helper method to schedule future reminders. */

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent != null ? intent.getStringExtra("reminderId") : null;
        if (id == null) return;
        Reminder r = ReminderStorage.getReminderById(context, id);
        if (r == null) return;
        Log.d("BroReminder", "Alarm fired for reminder " + id);
        int day = intent != null ? intent.getIntExtra("day", -1) : -1;
        int idx = intent != null ? intent.getIntExtra("timeIdx", -1) : -1;
        if (r.oneTime) {
            ReminderStorage.delete(context, r);
        } else if (day >= 0 && idx >= 0) {
            ReminderScheduler.scheduleNext(context, r, day, idx);
        }

        Intent svc = new Intent(context, OverlayService.class);
        if (r.text != null) svc.putExtra("message", r.text);
        if (r.title != null) svc.putExtra("title", r.title);
        if (r.audioPath != null) svc.putExtra("audioPath", r.audioPath);
        if (r.imagePath != null) svc.putExtra("imagePath", r.imagePath);
        if (r.ttsMessage != null && r.audioPath == null) svc.putExtra("tts", r.ttsMessage);
        svc.putExtra("playAlarm", r.playAlarm);
        svc.putExtra("yesNo", r.yesNoPrompt);
        svc.putExtra("priority", r.priority);
        svc.putExtra("autoAnswer", r.autoAnswer);
        if (r.oneTime) svc.putExtra("skipLog", true);
        svc.putExtra("reminderId", id);
        svc.putExtra("day", day);
        svc.putExtra("timeIdx", idx);
        context.startService(svc);

    }
}



package com.bro.broreminder.viewmodels;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderScheduler;
import com.bro.broreminder.reminder.ReminderStorage;
import com.bro.broreminder.reminder.ReminderEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel that stores all reminders and exposes operations
 * to create, update and delete them.
 */
public class ReminderViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Reminder>> reminders = new MutableLiveData<>();

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadReminders();
        }
    };

    public ReminderViewModel(@NonNull Application application) {
        super(application);
        application.registerReceiver(refreshReceiver, new IntentFilter(ReminderEvents.ACTION_CHANGED));
        loadReminders();
    }

    @Override
    protected void onCleared() {
        getApplication().unregisterReceiver(refreshReceiver);
        super.onCleared();
    }

    public LiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    private void loadReminders() {
        List<Reminder> list = ReminderStorage.load(getApplication());
        reminders.setValue(list);
    }

    private void saveReminders(List<Reminder> list) {
        if (!ReminderStorage.save(getApplication(), list)) {
            throw new RuntimeException("Failed to save reminders");
        }
    }

    public void addOrUpdate(Reminder r) {
        List<Reminder> list = reminders.getValue();
        if (list == null) list = new ArrayList<>();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            Reminder existing = list.get(i);
            if (existing.id != null && existing.id.equals(r.id)) {
                list.set(i, r);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(r);
        }
        saveReminders(list);
        if (r.enabled) {
            if (r.oneTime) {
                ReminderScheduler.scheduleOneTime(getApplication(), r);
            } else {
                ReminderScheduler.schedule(getApplication(), r);
            }
        } else {
            ReminderScheduler.cancel(getApplication(), r);
        }
        reminders.setValue(list);
    }

    public void delete(Reminder r) {
        List<Reminder> list = reminders.getValue();
        if (list == null) return;
        list.remove(r);
        saveReminders(list);
        ReminderScheduler.cancel(getApplication(), r);
        reminders.setValue(list);
    }

    public Reminder getById(String id) {
        List<Reminder> list = reminders.getValue();
        if (list == null) return null;
        for (Reminder r : list) {
            if (id != null && id.equals(r.id)) return r;
        }
        return null;
    }
}
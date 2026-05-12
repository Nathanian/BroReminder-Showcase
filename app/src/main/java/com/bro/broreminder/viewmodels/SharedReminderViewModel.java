package com.bro.broreminder.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bro.broreminder.reminder.Reminder;

/**
 * ViewModel used to share a selected reminder between fragments.
 */
public class SharedReminderViewModel extends ViewModel {
    private final MutableLiveData<Reminder> selected = new MutableLiveData<>();

    public void select(Reminder r) {
        selected.setValue(r);
    }

    public LiveData<Reminder> getSelected() {
        return selected;
    }
}
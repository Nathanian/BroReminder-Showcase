package com.bro.broreminder;

import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderStorage;

import org.json.JSONArray;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ReminderStorageTest {
    @Test
    public void testAudioFieldsPersisted() throws Exception {
        Reminder r = new Reminder();
        r.id = "1";
        r.title = "t";
        r.text = "body";
        r.times = new String[]{"12:00"};
        r.days = new boolean[]{true,true,true,true,true,true,true};
        r.enabled = true;
        r.timeEnabled = new boolean[]{true};
        r.playAlarm = false;
        r.audioPath = "/tmp/test.m4a";
        r.audioDuration = 1234;

        JSONArray arr = ReminderStorage.toJson(Collections.singletonList(r));
        List<Reminder> list = ReminderStorage.fromJson(arr);
        assertEquals(1, list.size());
        Reminder r2 = list.get(0);
        assertEquals(r.playAlarm, r2.playAlarm);
        assertEquals(r.audioPath, r2.audioPath);
        assertEquals(r.audioDuration, r2.audioDuration);
    }
}

package com.bro.broreminder;

import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderStorage;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ReminderStorageTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Reminder createReminder(String id) {
        Reminder r = new Reminder();
        r.id = id;
        r.title = "Title " + id;
        r.text = "Text " + id;
        r.times = new String[]{"08:00", "20:00"};
        r.days = new boolean[]{true, false, true, false, true, false, true};
        r.enabled = true;
        r.yesNoPrompt = false;
        r.repeatOnNo = id.hashCode() % 2 == 0;
        r.priority = id.hashCode() % 2 != 0;
        r.autoAnswer = id.hashCode() % 3 == 0;
        return r;
    }

    @Test
    public void testJsonRoundTrip() throws Exception {
        List<Reminder> list = new ArrayList<>();
        list.add(createReminder("a"));
        list.add(createReminder("b"));

        JSONArray json = ReminderStorage.toJson(list);
        List<Reminder> result = ReminderStorage.fromJson(json);

        assertEquals(list.size(), result.size());
        for (int i = 0; i < list.size(); i++) {
            Reminder expected = list.get(i);
            Reminder actual = result.get(i);
            assertEquals(expected.id, actual.id);
            assertEquals(expected.title, actual.title);
            assertEquals(expected.text, actual.text);
            assertArrayEquals(expected.times, actual.times);
            assertArrayEquals(expected.days, actual.days);
            assertEquals(expected.enabled, actual.enabled);
            assertEquals(expected.yesNoPrompt, actual.yesNoPrompt);
            assertEquals(expected.repeatOnNo, actual.repeatOnNo);
            assertEquals(expected.autoAnswer, actual.autoAnswer);
            assertEquals(expected.priority, actual.priority);
        }
    }

    @Test
    public void testImportFromFile() throws Exception {
        List<Reminder> list = new ArrayList<>();
        list.add(createReminder("x"));

        JSONArray json = ReminderStorage.toJson(list);
        File f = tmp.newFile("reminders.json");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(json.toString().getBytes());
        }

        List<Reminder> loaded = ReminderStorage.importFromFile(f);
        assertEquals(1, loaded.size());
        assertEquals("x", loaded.get(0).id);
    }
}
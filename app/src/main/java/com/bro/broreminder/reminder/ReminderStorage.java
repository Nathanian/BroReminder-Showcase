package com.bro.broreminder.reminder;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReminderStorage {

    private static final String TAG = "BroReminder";
    private static final String FILE = "reminders.json";

    public static List<Reminder> load(Context ctx) {
        List<Reminder> list = new ArrayList<>();
        try {
            byte[] data;
            try (FileInputStream fis = ctx.openFileInput(FILE)) {
                data = new byte[fis.available()];
                if (fis.read(data) != data.length) throw new IOException("EOF");
            }
            JSONArray arr = new JSONArray(new String(data));
            list = fromJson(arr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load reminders", e);
        }
        return list;
    }

    public static boolean save(Context ctx, List<Reminder> list) {
        JSONArray arr;
        try {
            arr = toJson(list);
            try (FileOutputStream fos = ctx.openFileOutput(FILE, Context.MODE_PRIVATE)) {
                fos.write(arr.toString().getBytes());
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save reminders", e);
            return false;
        }
    }

    public static JSONArray toJson(List<Reminder> list) throws Exception {
        JSONArray arr = new JSONArray();
        for (Reminder r : list) {
            JSONObject o = new JSONObject();
            o.put("id", r.id);
            if (r.title != null) o.put("title", r.title);
            o.put("text", r.text);
            if (r.ttsMessage != null) o.put("tts", r.ttsMessage);
            if (r.audioPath != null) o.put("audioPath", r.audioPath);
            o.put("audioDuration", r.audioDuration);
            if (r.imagePath != null) o.put("imagePath", r.imagePath);
            o.put("playAlarm", r.playAlarm);

            JSONArray times = new JSONArray();
            for (String t : r.times) times.put(t);
            o.put("times", times);

            if (r.timeEnabled != null) {
                JSONArray enables = new JSONArray();
                for (boolean b : r.timeEnabled) enables.put(b);
                o.put("timeEnabled", enables);
            }

            JSONArray days = new JSONArray();
            if (r.days != null) {
                for (boolean b : r.days) days.put(b);
            }
            o.put("days", days);

            o.put("enabled", r.enabled);
            o.put("yesNo", r.yesNoPrompt);
            o.put("repeatOnNo", r.repeatOnNo);
            o.put("autoAnswer", r.autoAnswer);
            o.put("priority", r.priority);
            if (r.repeatIntervalHours != null) o.put("repeatIntervalHours", r.repeatIntervalHours);
            o.put("oneTime", r.oneTime);
            if (r.oneTimeDate > 0) o.put("oneTimeDate", r.oneTimeDate);

            arr.put(o);
        }
        return arr;
    }

    public static List<Reminder> fromJson(JSONArray arr) throws Exception {
        List<Reminder> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Reminder r = new Reminder();
            r.id = o.getString("id");
            r.title = o.optString("title", null);
            r.text = o.getString("text");
            r.ttsMessage = o.optString("tts", null);
            r.audioPath = o.optString("audioPath", null);
            r.audioDuration = o.optLong("audioDuration", 0);
            r.imagePath = o.optString("imagePath", null);
            r.playAlarm = o.optBoolean("playAlarm", true);

            JSONArray times = o.getJSONArray("times");
            r.times = new String[times.length()];
            for (int t = 0; t < times.length(); t++) {
                r.times[t] = times.getString(t);
            }

            JSONArray enables = o.optJSONArray("timeEnabled");
            if (enables != null && enables.length() == times.length()) {
                r.timeEnabled = new boolean[times.length()];
                for (int e = 0; e < enables.length(); e++) {
                    r.timeEnabled[e] = enables.getBoolean(e);
                }
            } else {
                r.timeEnabled = new boolean[times.length()];
                for (int e = 0; e < r.timeEnabled.length; e++) r.timeEnabled[e] = true;
            }

            JSONArray days = o.getJSONArray("days");
            r.days = new boolean[7];
            for (int d = 0; d < 7 && d < days.length(); d++) {
                r.days[d] = days.getBoolean(d);
            }

            r.enabled = o.optBoolean("enabled", false);
            r.yesNoPrompt = o.optBoolean("yesNo", true);
            r.repeatOnNo = o.optBoolean("repeatOnNo", true);
            r.autoAnswer = o.optBoolean("autoAnswer", false);
            r.priority = o.optBoolean("priority", false);
            int ri = o.optInt("repeatIntervalHours", 0);
            if (ri > 0) r.repeatIntervalHours = ri;
            r.oneTime = o.optBoolean("oneTime", false);
            r.oneTimeDate = o.optLong("oneTimeDate", 0);

            list.add(r);
        }
        return list;
    }

    public static void exportToFile(Context ctx, File out) throws Exception {
        List<Reminder> all = load(ctx);
        List<Reminder> active = new ArrayList<>();
        for (Reminder r : all) {
            if (r.enabled) active.add(r);
        }
        JSONArray arr = toJson(active);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(arr.toString().getBytes());
        }
    }

    public static List<Reminder> importFromFile(File in) throws Exception {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(in)) {
            data = new byte[fis.available()];
            if (fis.read(data) != data.length) throw new IOException("EOF");
        }
        JSONArray arr = new JSONArray(new String(data));
        return fromJson(arr);
    }

    public static Reminder getReminderById(Context ctx, String id) {
        List<Reminder> list = load(ctx);
        for (Reminder r : list) {
            if (id.equals(r.id)) {
                return r;
            }
        }
        return null;
    }

    public static void delete(Context ctx, Reminder target) {
        List<Reminder> list = load(ctx);
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            if (target.id != null && target.id.equals(list.get(i).id)) {
                list.remove(i);
                break;
            }
        }
        if (!save(ctx, list)) {
            Log.e(TAG, "Failed to delete reminder");
        }
    }
}

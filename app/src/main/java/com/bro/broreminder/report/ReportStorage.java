package com.bro.broreminder.report;

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

public class ReportStorage {
    private static final String TAG = "BroReminder";

    private static String readFile(File f) throws IOException {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(f)) {
            data = new byte[fis.available()];
            if (fis.read(data) != data.length) throw new IOException("EOF");
        }
        return new String(data);
    }

    public static void exportToFile(Context ctx, File out) throws Exception {
        JSONArray arr;
        List<String> existingDates = new ArrayList<>();
        if (out.exists()) {
            byte[] data;
            try (FileInputStream fis = new FileInputStream(out)) {
                data = new byte[fis.available()];
                if (fis.read(data) != data.length) throw new IOException("EOF");
            }
            arr = new JSONArray(new String(data));
            for (int i = 0; i < arr.length(); i++) {
                existingDates.add(arr.getJSONObject(i).getString("date"));
            }
        } else {
            arr = new JSONArray();
        }

        File dir = new File(ctx.getFilesDir(), "reports");
        File[] files = dir.listFiles();
        if (files == null) files = new File[0];
        for (File f : files) {
            String date = f.getName().replace("report_", "").replace(".txt", "");
            if (existingDates.contains(date)) continue;
            try {
                JSONObject o = new JSONObject();
                o.put("date", date);
                o.put("text", readFile(f));
                arr.put(o);
            } catch (Exception e) {
                Log.e(TAG, "Failed to export report " + f.getName(), e);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(arr.toString().getBytes());
        }
    }

    public static void importFromFile(Context ctx, File in) throws Exception {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(in)) {
            data = new byte[fis.available()];
            if (fis.read(data) != data.length) throw new IOException("EOF");
        }
        JSONArray arr = new JSONArray(new String(data));

        File dir = new File(ctx.getFilesDir(), "reports");
        if (!dir.exists()) dir.mkdirs();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String date = o.getString("date");
            File f = new File(dir, "report_" + date + ".txt");
            if (f.exists()) continue;
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(o.getString("text").getBytes());
            }
        }
    }
}
package com.bro.broreminder.report;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Utility class that stores a log entry whenever a reminder dialog is answered. */
public class LogStorage {
    private static final String TAG = "BroReminder";

    public static void logReminder(Context ctx, String title, String text, String answer) {
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            File dir = new File(ctx.getFilesDir(), "reports");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "report_" + date + ".txt");
            try (FileWriter fw = new FileWriter(f, true)) {
                if (f.length() == 0) {
                    fw.write("Date: " + date + "\n");
                }
                fw.write(time + " - " + title + " - " + text + " - " + answer + "\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to log reminder", e);
        }
    }
}
package com.bro.broreminder.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Build;
import android.os.Environment;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.bro.broreminder.R;
import com.bro.broreminder.report.ReportStorage;
import com.bro.broreminder.utils.ScheduleUtils;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Generates a simple daily report summarizing how reminders were answered.
 */
public class DailyReportReceiver extends BroadcastReceiver {
    private static final String TAG = "BroReminder";

    public static void scheduleReport(Context context, long delayMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DailyReportReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);
        long triggerAt = System.currentTimeMillis() + delayMillis;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        generateDailySummary(context);
        exportReports(context);
        scheduleReport(context, ScheduleUtils.nextReportTime() - System.currentTimeMillis());
        EmailReportReceiver.scheduleEmail(context, ScheduleUtils.nextEmailTime() - System.currentTimeMillis());
    }

    static void generateDailySummary(Context ctx) {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        File dir = new File(ctx.getFilesDir(), "reports");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "report_" + date + ".txt");
        if (!f.exists() || f.length() == 0) {
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("Date: " + date + "\n");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create today's report file", e);
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String nextDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        File nf = new File(dir, "report_" + nextDate + ".txt");
        if (!nf.exists()) {
            try (FileWriter fw = new FileWriter(nf)) {
                fw.write("Date: " + nextDate + "\n");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create next day report file", e);
            }

        }
    }

    private void exportReports(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "WRITE_EXTERNAL_STORAGE permission not granted");
                return;
            }
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File out = new File(dir, "bro_reports.json");
            ReportStorage.exportToFile(ctx, out);
            Log.i(TAG, ctx.getString(R.string.export_success, out.getAbsolutePath()));
        } catch (Exception e) {
            Log.e(TAG, "Report export failed", e);
        }
    }
}

package com.bro.broreminder.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.bro.broreminder.utils.ScheduleUtils;
import com.bro.broreminder.utils.MailSender;
import com.bro.broreminder.utils.DemoConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailReportReceiver extends BroadcastReceiver {
    private static final String TAG = "BroReminder";


    public static void scheduleEmail(Context context, long delayMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, EmailReportReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_IMMUTABLE);
        long trigger = System.currentTimeMillis() + delayMillis;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        Log.d(TAG, "Scheduled next email in " + delayMillis + " ms");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long next = ScheduleUtils.nextEmailTime() - System.currentTimeMillis();
        scheduleEmail(context, next);

        final PendingResult result = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean success = sendReport(context);
            if (!success) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Failed to send report email", Toast.LENGTH_LONG).show());
            }
            result.finish();
            executor.shutdown();
        });
    }

    private boolean sendReport(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String to = prefs.getString("reportEmail", null);
        if (to == null || to.isEmpty()) {
            Log.i(TAG, "No report email configured");
            return true;
        }

        if (!DemoConfig.isConfigured()) {
            Log.w(TAG, "SMTP placeholders are not configured");
            return true;
        }

        String fileDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        File reportFile = new File(new File(ctx.getFilesDir(), "reports"), "report_" + fileDate + ".txt");
        if (!reportFile.exists()) {
            Log.i(TAG, "No report file for today");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(reportFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read report", e);
            return false;
        }

        try {
            String subjectDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            MailSender.send(
                    DemoConfig.SMTP_HOST,
                    DemoConfig.SMTP_PORT,
                    DemoConfig.SMTP_USER,
                    DemoConfig.SMTP_PASS,
                    to,
                    "Reminder-Report vom " + subjectDate,
                    sb.toString()
            );
            Log.d(TAG, "Report email sent successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send report email", e);
            return false;
        }
    }
}

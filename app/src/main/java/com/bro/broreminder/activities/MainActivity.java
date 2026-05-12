package com.bro.broreminder.activities;

import android.os.Bundle;
import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bro.broreminder.R;
import com.bro.broreminder.fragments.ReminderListFragment;
import com.bro.broreminder.receiver.DailyReportReceiver;
import com.bro.broreminder.receiver.EmailReportReceiver;
import com.bro.broreminder.utils.ScheduleUtils;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        checkPermissions();
        ensureReportAlarms();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ReminderListFragment())
                    .commit();
        }
    }
    /**
     * Schedule the daily report and email alarms if they are not already set.
     */
    private void ensureReportAlarms() {

        Intent reportIntent = new Intent(this, DailyReportReceiver.class);
        PendingIntent reportPi = PendingIntent.getBroadcast(
                this,
                1,
                reportIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (reportPi == null) {
            DailyReportReceiver.scheduleReport(
                    this,
                    ScheduleUtils.nextReportTime() - System.currentTimeMillis());
        }

        Intent emailIntent = new Intent(this, EmailReportReceiver.class);
        PendingIntent emailPi = PendingIntent.getBroadcast(
                this,
                2,
                emailIntent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (emailPi == null) {
            EmailReportReceiver.scheduleEmail(
                    this,
                    ScheduleUtils.nextEmailTime() - System.currentTimeMillis());
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            java.util.List<String> perms = new java.util.ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.RECORD_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.CAMERA);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (!perms.isEmpty()) {
                ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
            }
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, getString(R.string.overlay_permission), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }
}

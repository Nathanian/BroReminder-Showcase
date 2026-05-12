package com.bro.broreminder.fragments;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatImageView;

import com.bro.broreminder.R;
import com.bro.broreminder.receiver.DailyReportReceiver;
import com.bro.broreminder.receiver.EmailReportReceiver;
import com.bro.broreminder.reminder.ReminderStorage;
import com.bro.broreminder.report.ReportStorage;
import com.bro.broreminder.utils.ButtonUtils;
import com.bro.broreminder.utils.MailSender;
import com.bro.broreminder.utils.DemoConfig;
import com.bro.broreminder.utils.ScheduleUtils;
import com.bro.broreminder.voice.VoiceActionRegistrar;
import com.bro.broreminder.voice.FieldDictationController;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragment for settings and import/export functions.
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "BroReminder";
    private static final int REQ_PERMS = 1;


    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FieldDictationController dictationController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText emailField = view.findViewById(R.id.emailField);
        AppCompatImageView emailSpeechButton = view.findViewById(R.id.emailSpeechButton);
        Button save = view.findViewById(R.id.saveEmailButton);
        Button test = view.findViewById(R.id.sendTestEmailButton);
        Button expRem = view.findViewById(R.id.exportReminders);
        Button impRem = view.findViewById(R.id.importReminders);
        Button sendRem = view.findViewById(R.id.emailReminders);
        Button expRep = view.findViewById(R.id.exportReports);
        Button impRep = view.findViewById(R.id.importReports);
        Button sendRep = view.findViewById(R.id.emailReports);
        Button scheduleRep = view.findViewById(R.id.scheduleReports);
        Button sendNow = view.findViewById(R.id.sendReportNow);
        Button clearRep = view.findViewById(R.id.clearReports);
        Button back = view.findViewById(R.id.backSettings);
        Button reg = view.findViewById(R.id.registerVoiceAction);
        Button unreg = view.findViewById(R.id.unregisterVoiceAction);

        dictationController = new FieldDictationController();
        attachMic(emailSpeechButton, emailField);

        ButtonUtils.applyHaptic(save);
        ButtonUtils.applyHaptic(test);
        ButtonUtils.applyHaptic(expRem);
        ButtonUtils.applyHaptic(impRem);
        ButtonUtils.applyHaptic(sendRem);
        ButtonUtils.applyHaptic(expRep);
        ButtonUtils.applyHaptic(impRep);
        ButtonUtils.applyHaptic(sendRep);
        ButtonUtils.applyHaptic(scheduleRep);
        ButtonUtils.applyHaptic(sendNow);
        ButtonUtils.applyHaptic(clearRep);
        ButtonUtils.applyHaptic(back);
        ButtonUtils.applyHaptic(reg);
        ButtonUtils.applyHaptic(unreg);
        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        emailField.setText(prefs.getString("reportEmail", ""));

        save.setOnClickListener(v -> {
            prefs.edit().putString("reportEmail", emailField.getText().toString().trim()).apply();
            showToast(R.string.toast_saved);
        });

        test.setOnClickListener(v -> {
            prefs.edit().putString("reportEmail", emailField.getText().toString().trim()).apply();
            sendTestEmail();
        });
        expRem.setOnClickListener(v -> {
            if (ensurePermissions()) {
                exportReminders(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "bro_reminders.json"));
            }
        });
        impRem.setOnClickListener(v -> {
            if (ensurePermissions()) {
                importReminders(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "bro_reminders.json"));
            }
        });
        sendRem.setOnClickListener(v -> {
            if (ensurePermissions()) sendRemindersToEmail();
        });
        expRep.setOnClickListener(v -> {
            if (ensurePermissions()) {
                exportReports(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "bro_reports.json"));
            }
        });
        impRep.setOnClickListener(v -> {
            if (ensurePermissions()) {
                importReports(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "bro_reports.json"));
            }
        });
        sendRep.setOnClickListener(v -> {
            if (ensurePermissions()) sendReportsToEmail();
        });
        scheduleRep.setOnClickListener(v -> scheduleReportAlarms());
        sendNow.setOnClickListener(v -> {
            if (ensurePermissions()) sendReportNow();
        });
        clearRep.setOnClickListener(v -> clearReports());
        reg.setOnClickListener(v -> VoiceActionRegistrar.register(requireContext()));
        unreg.setOnClickListener(v -> VoiceActionRegistrar.unregister(requireContext()));
    }

    private void sendTestEmail() {
        executor.execute(() -> {
            Context context = getContext();
            if (context == null) return;
            try {
                SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                String to = prefs.getString("reportEmail", "");
                if (to.isEmpty()) return;
                if (!DemoConfig.isConfigured()) { Log.w(TAG, "SMTP placeholders are not configured"); return; }
                MailSender.send(DemoConfig.SMTP_HOST, DemoConfig.SMTP_PORT, DemoConfig.SMTP_USER, DemoConfig.SMTP_PASS, to,
                        "Test from BroReminder",
                        "This is a test message sent from the settings menu.");
                Log.d(TAG, "Test email sent");
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_sent), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to send test email", e);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_failed), Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private boolean ensurePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            boolean write = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean read = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!write || !read) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMS);
                return false;
            }
        }
        return true;
    }

    private void showToast(int resId) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
        } else {
            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show());
        }
    }

    private void exportReminders(File out) {
        try {
            ReminderStorage.exportToFile(requireContext(), out);
            Log.i(TAG, getString(R.string.export_success, out.getAbsolutePath()));
            showToast(R.string.toast_saved);
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            showToast(R.string.export_failed);
        }
    }

    private void importReminders(File in) {
        try {
            if (!in.exists()) {
                Log.i(TAG, getString(R.string.file_not_found));
                return;
            }
            java.util.List<com.bro.broreminder.reminder.Reminder> imported = ReminderStorage.importFromFile(in);
            java.util.List<com.bro.broreminder.reminder.Reminder> list = ReminderStorage.load(requireContext());
            boolean changed = false;
            for (com.bro.broreminder.reminder.Reminder r : imported) {
                boolean exists = false;
                for (com.bro.broreminder.reminder.Reminder existing : list) {
                    if (existing.id != null && existing.id.equals(r.id)) {
                        exists = true; break;
                    }
                }
                if (!exists) {
                    list.add(r);
                    if (r.enabled) {
                        com.bro.broreminder.reminder.ReminderScheduler.schedule(requireContext(), r);
                    }
                    changed = true;
                }
            }
            if (changed && !ReminderStorage.save(requireContext(), list)) {
                Log.e(TAG, "Failed to save imported reminders");
            }
            Log.i(TAG, getString(R.string.import_success));
            showToast(R.string.toast_loaded);
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            showToast(R.string.import_failed);
        }
    }

    private void exportReports(File out) {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "WRITE_EXTERNAL_STORAGE permission not granted");
            return;
        }
        try {
            ReportStorage.exportToFile(requireContext(), out);
            Log.i(TAG, getString(R.string.export_success, out.getAbsolutePath()));
            showToast(R.string.toast_saved);
        } catch (Exception e) {
            Log.e(TAG, "Report export failed", e);
            showToast(R.string.export_failed);
        }
    }

    private void importReports(File in) {
        try {
            if (!in.exists()) {
                Log.i(TAG, getString(R.string.file_not_found));
                return;
            }
            ReportStorage.importFromFile(requireContext(), in);
            Log.i(TAG, getString(R.string.import_success));
            showToast(R.string.toast_loaded);
        } catch (Exception e) {
            Log.e(TAG, "Report import failed", e);
            showToast(R.string.import_failed);
        }
    }

    private void sendReportsToEmail() {
        executor.execute(() -> {
            Context context = getContext();
            if (context == null) return;
            try {
                SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                String to = prefs.getString("reportEmail", "");
                if (to.isEmpty()) return;
                if (!DemoConfig.isConfigured()) { Log.w(TAG, "SMTP placeholders are not configured"); return; }
                File tmp = File.createTempFile("bro_reports", ".json", context.getCacheDir());
                if (tmp.exists()) tmp.delete();
                ReportStorage.exportToFile(context, tmp);
                MailSender.sendWithAttachment(DemoConfig.SMTP_HOST, DemoConfig.SMTP_PORT, DemoConfig.SMTP_USER, DemoConfig.SMTP_PASS, to,
                        "BroReminder Reports", "BroReminder Reports", tmp);
                tmp.delete();
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_sent), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to send reports", e);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_failed), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void sendRemindersToEmail() {
        executor.execute(() -> {
            Context context = getContext();
            if (context == null) return;
            try {
                SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                String to = prefs.getString("reportEmail", "");
                if (to.isEmpty()) return;
                if (!DemoConfig.isConfigured()) { Log.w(TAG, "SMTP placeholders are not configured"); return; }
                File tmp = File.createTempFile("bro_reminders", ".json", context.getCacheDir());
                if (tmp.exists()) tmp.delete();
                ReminderStorage.exportToFile(context, tmp);
                MailSender.sendWithAttachment(DemoConfig.SMTP_HOST, DemoConfig.SMTP_PORT, DemoConfig.SMTP_USER, DemoConfig.SMTP_PASS, to,
                        "BroReminder Reminders", "BroReminder Reminders", tmp);
                tmp.delete();
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_sent), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to send reminders", e);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_failed), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private boolean isReportScheduled(Class<?> receiver, int requestCode) {
        Intent intent = new Intent(requireContext(), receiver);
        PendingIntent pi = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        return pi != null;
    }
    private void scheduleReportAlarms() {
        boolean dailyScheduled = isReportScheduled(DailyReportReceiver.class, 1);
        boolean emailScheduled = isReportScheduled(EmailReportReceiver.class, 2);
        if (dailyScheduled && emailScheduled) {
            showToast(R.string.toast_already_scheduled);
            return;
        }
        if (!dailyScheduled) {
            DailyReportReceiver.scheduleReport(
                    requireContext(),
                    ScheduleUtils.nextReportTime() - System.currentTimeMillis());
        }
        if (!emailScheduled) {
            EmailReportReceiver.scheduleEmail(
                    requireContext(),
                    ScheduleUtils.nextEmailTime() - System.currentTimeMillis());
        }
        showToast(R.string.toast_scheduled);
    }
    private void clearReports() {
        File dir = new File(requireContext().getFilesDir(), "reports");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.delete()) Log.w(TAG, "Failed to delete " + f.getName());
            }
        }
        showToast(R.string.toast_reports_cleared);
    }
    private void sendReportNow() {
        executor.execute(() -> {
            Context context = getContext();
            if (context == null) return;
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                File dir = new File(context.getFilesDir(), "reports");
                File f = new File(dir, "report_" + date + ".txt");
                if (!f.exists()) return;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }

                SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                String to = prefs.getString("reportEmail", "");
                if (to.isEmpty()) return;
                if (!DemoConfig.isConfigured()) { Log.w(TAG, "SMTP placeholders are not configured"); return; }
                MailSender.send(
                        DemoConfig.SMTP_HOST, DemoConfig.SMTP_PORT, DemoConfig.SMTP_USER, DemoConfig.SMTP_PASS, to,
                        "BroReminder Report " + date,
                        sb.toString());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_sent), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to send report", e);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.toast_email_failed), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (dictationController != null) dictationController.cancel();
    }

    private void attachMic(AppCompatImageView micBtn, EditText target) {
        micBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 0);
                return;
            }
            micBtn.setEnabled(false);
            Animation anim = new AlphaAnimation(1f, 0.3f);
            anim.setDuration(500);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            micBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.orange));
            micBtn.startAnimation(anim);
            dictationController.startDictation(requireActivity(), new FieldDictationController.Listener() {
                @Override
                public void onPartialText(String text) { }

                @Override
                public void onFinalText(String text) {
                    requireActivity().runOnUiThread(() -> {
                        int start = Math.max(target.getSelectionStart(), 0);
                        int end = Math.max(target.getSelectionEnd(), 0);
                        target.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
                        micBtn.clearAnimation();
                        micBtn.clearColorFilter();
                        micBtn.setEnabled(true);
                    });
                }

                @Override
                public void onError(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.dictation_error, Toast.LENGTH_SHORT).show();
                        micBtn.clearAnimation();
                        micBtn.clearColorFilter();
                        micBtn.setEnabled(true);
                    });
                }

                @Override
                public void onTimeout() {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), R.string.dictation_timeout, Toast.LENGTH_SHORT).show();
                        micBtn.clearAnimation();
                        micBtn.clearColorFilter();
                        micBtn.setEnabled(true);
                    });
                }
            });
        });
    }
}
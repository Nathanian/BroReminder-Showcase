package com.bro.broreminder.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.media.MediaPlayer;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bro.broreminder.R;
import com.bro.broreminder.receiver.ReminderReceiver;
import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderScheduler;
import com.bro.broreminder.reminder.ReminderStorage;
import com.bro.broreminder.report.LogStorage;
import com.bro.broreminder.utils.ButtonUtils;
import com.bro.broreminder.voice.SdkTts;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that displays a fullscreen overlay for reminders. It merges the
 * queuing/audio/TTS logic of the former OverlayService with the WindowManager
 * based presentation of ReminderOverlay.
 */
public class OverlayService extends Service {

    private static final AtomicBoolean overlayActive = new AtomicBoolean(false);
    private static final Queue<Intent> queue = new ConcurrentLinkedQueue<>();
    private static boolean currentPriority = false;
    private static Intent currentIntent;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WindowManager windowManager;
    private View overlayView;
    private SdkTts tts;
    private MediaPlayer player;

    private String ttsText;
    private String audioPath;
    private String imagePath;
    private boolean playAlarm = true;
    private boolean autoAnswer = false;
    private Runnable timeoutRunnable;

    private void enqueueIntent(Intent i) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            queue.add(i);
        } else {
            mainHandler.post(() -> queue.add(i));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean incomingPriority = intent != null && intent.getBooleanExtra("priority", false);
        synchronized (OverlayService.class) {
            if (overlayActive.get()) {
                if (incomingPriority && !currentPriority) {
                    if (currentIntent != null) enqueueIntent(new Intent(currentIntent));
                    removeView();
                    stopPlayback();
                    overlayActive.set(false);
                } else {
                    enqueueIntent(new Intent(intent));
                    return START_NOT_STICKY;
                }
            }

            overlayActive.set(true);
            currentPriority = incomingPriority;
            currentIntent = intent;
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.i("BroReminder", getString(R.string.overlay_permission));
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(overlayIntent);
            overlayActive.set(false);
            stopSelf();
            return START_NOT_STICKY;
        }

        boolean skipLog = intent != null && intent.getBooleanExtra("skipLog", false);
        String message = intent != null ? intent.getStringExtra("message") : null;
        String title = intent != null ? intent.getStringExtra("title") : getString(R.string.app_name);
        boolean yesNo = intent == null || intent.getBooleanExtra("yesNo", true);
        String reminderId = intent != null ? intent.getStringExtra("reminderId") : null;
        int day = intent != null ? intent.getIntExtra("day", -1) : -1;
        int timeIdx = intent != null ? intent.getIntExtra("timeIdx", -1) : -1;
        playAlarm = intent == null || intent.getBooleanExtra("playAlarm", true);
        audioPath = intent != null ? intent.getStringExtra("audioPath") : null;
        imagePath = intent != null ? intent.getStringExtra("imagePath") : null;
        ttsText = (audioPath == null && intent != null) ? intent.getStringExtra("tts") : null;
        autoAnswer = intent != null && intent.getBooleanExtra("autoAnswer", false);

        showOverlay(skipLog, message, title, yesNo, reminderId, day, timeIdx);
        return START_NOT_STICKY;
    }

    private void showOverlay(boolean skipLog, String message, String title,
                             boolean yesNo, String reminderId, int day, int timeIdx) {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.6f;

        LayoutInflater inflater = LayoutInflater.from(this);
        int layout = imagePath != null ? R.layout.dialog_overlay_image : R.layout.dialog_overlay;
        overlayView = inflater.inflate(layout, null);

        TextView txt = overlayView.findViewById(R.id.promptText);
        if (message != null) {
            txt.setText(message);
        }
        if (imagePath != null) {
            ImageView img = overlayView.findViewById(R.id.promptImage);
            if (img != null) {
                img.setImageURI(Uri.fromFile(new File(imagePath)));
            }
        }

        Button yes = overlayView.findViewById(R.id.buttonYes);
        Button no = overlayView.findViewById(R.id.buttonNo);
        ButtonUtils.applyHaptic(yes);
        ButtonUtils.applyHaptic(no);

        if (yesNo) {
            yes.setOnClickListener(v -> {
                if (autoAnswer && timeoutRunnable != null) mainHandler.removeCallbacks(timeoutRunnable);
                if (!skipLog) {
                    LogStorage.logReminder(this, title, message != null ? message : "", "Ja");
                }
                finishOverlay();
            });

            no.setOnClickListener(v -> {
                if (autoAnswer && timeoutRunnable != null) mainHandler.removeCallbacks(timeoutRunnable);
                if (!skipLog) {
                    LogStorage.logReminder(this, title, message != null ? message : "", "Nein");
                }
                if (reminderId != null) {
                    handleRepeatNo(reminderId, day, timeIdx);
                }
                finishOverlay();
            });
        } else {
            no.setVisibility(View.GONE);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) yes.getLayoutParams();
            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            yes.setLayoutParams(lp);
            yes.setText(R.string.ok);
            yes.setOnClickListener(v -> {
                if (autoAnswer && timeoutRunnable != null) mainHandler.removeCallbacks(timeoutRunnable);
                if (!skipLog) {
                    LogStorage.logReminder(this, title, message != null ? message : "", "OK");
                }
                finishOverlay();
            });
        }

        windowManager.addView(overlayView, params);
        playSound();

        if (autoAnswer) {
            timeoutRunnable = () -> {
                if (!skipLog) {
                    LogStorage.logReminder(this, title, message != null ? message : "", "Unbeantwortet");
                }
                finishOverlay();
            };
            // Auto-answer after 60 minutes if the user does not respond
            mainHandler.postDelayed(timeoutRunnable, 60 * 60 * 1000L);
        }
    }

    /**
     * Schedule another reminder in 15 minutes unless the next configured time
     * slot is less than 30 minutes away.
     */
    private void handleRepeatNo(String reminderId, int day, int timeIdx) {
        Reminder r = ReminderStorage.getReminderById(this, reminderId);
        if (r == null || !r.repeatOnNo) return;
        long next;
        if (day >= 0 && timeIdx >= 0 && r.times != null && timeIdx < r.times.length
                && (r.timeEnabled == null || timeIdx >= r.timeEnabled.length || r.timeEnabled[timeIdx])) {
            next = ReminderScheduler.computeNextOccurrence(day, r.times[timeIdx]);
        } else {
            next = findNextScheduledTime(r);
        }
        long now = System.currentTimeMillis();
        long diff = next - now;
        if (diff >= 30 * 60 * 1000L) {
            scheduleFollowUp(reminderId, 15 * 60 * 1000L);
        } else {
            Log.d("BroReminder", "Next slot soon (" + diff + "ms), not rescheduling");
        }
    }

    private void scheduleFollowUp(String reminderId, long delay) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("reminderId", reminderId);
        int request = ("repeat_" + reminderId).hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(this, request, intent, PendingIntent.FLAG_IMMUTABLE);
        long trigger = System.currentTimeMillis() + delay;
        Log.d("BroReminder", "Scheduling follow up for " + reminderId + " at " + trigger);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    private long findNextScheduledTime(Reminder r) {
        long best = Long.MAX_VALUE;
        if (r.times == null || r.days == null) return best;
        for (int d = 0; d < r.days.length; d++) {
            if (!r.days[d]) continue;
            for (int i = 0; i < r.times.length; i++) {
                if (r.timeEnabled != null && i < r.timeEnabled.length && !r.timeEnabled[i]) continue;
                long ts = ReminderScheduler.computeNextOccurrence(d, r.times[i]);
                if (ts < best) best = ts;
            }
        }
        return best;
    }

    private void playSound() {
        if (playAlarm) {
            try {
                player = MediaPlayer.create(this, R.raw.reminder_sound);
                if (player != null) {
                    player.setOnCompletionListener(m -> {
                        m.release();
                        player = null;
                        playRecordingOrSpeak();
                    });
                    player.start();
                    return;
                }
            } catch (Exception e) {
                Log.e("BroReminder", "Failed to play sound", e);
            }
        }
        playRecordingOrSpeak();
    }

    private void playRecordingOrSpeak() {
        if (audioPath != null) {
            try {
                player = new MediaPlayer();
                player.setDataSource(audioPath);
                player.setOnCompletionListener(m -> {
                    m.release();
                    player = null;
                });
                player.prepare();
                player.start();
            } catch (Exception e) {
                Log.e("BroReminder", "Failed to play recording", e);
                playRecordingOrSpeakFallback();
            }
        } else {
            playRecordingOrSpeakFallback();
        }
    }

    private void playRecordingOrSpeakFallback() {
        speakMessage();
    }

    private void speakMessage() {
        if (ttsText == null || ttsText.isEmpty()) return;
        if (tts == null) {
            tts = new SdkTts(this);
        }
        tts.speak(ttsText, new SdkTts.Callback() {
            @Override public void onDone() { /* no-op */ }

            @Override public void onError(Exception e) {
                android.util.Log.e("OverlayService", "TTS error", e);
            }
        });
    }

    private void finishOverlay() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::finishOverlay);
            return;
        }
        overlayActive.set(false);
        removeView();
        stopPlayback();
        stopSelf();
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
        Intent next = queue.poll();
        if (next != null) {
            startService(next);
        }
    }

    private void removeView() {
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (IllegalArgumentException ignored) {
            }
            overlayView = null;
        }
    }

    private void stopPlayback() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        if (tts != null) {
            tts.stop();
            tts = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        removeView();
        stopPlayback();
        super.onDestroy();
    }
}


package com.bro.broreminder.voice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bro.broreminder.R;
import com.bro.broreminder.reminder.Reminder;
import com.bro.broreminder.reminder.ReminderScheduler;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.bro.broreminder.voice.MicArbiter;
import com.bro.broreminder.voice.SdkTts;
import com.bro.broreminder.voice.FieldDictationController;

/**
 * Foreground service that runs the in-app voice wizard for one-time reminders.
 */
public class VoiceWizardService extends Service implements VoiceWizardOverlay.Listener {
    private static final String TAG = "VoiceWizard";
    private enum Step { TITLE, NOTES, DATE, TIME, CONFIRM }

    private static final long STEP_TIMEOUT = 60000L;
    private static final long GLOBAL_TIMEOUT = 180000L;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "voice_wizard";

    /**
     * Tracks the currently active instance of the service.
     *
     * <p>The reference is static so that a newly started service can cancel a previous
     * instance if it is still alive.  To avoid leaking the old service when the system restarts
     * the service, the field is cleared in {@link #cancelSession()} and {@link #onDestroy()}.
     * Access is synchronized on {@link #RUNNING_LOCK} and the field is {@code volatile} so
     * changes are visible across threads.</p>
     */
    private static volatile VoiceWizardService running;
    private static final Object RUNNING_LOCK = new Object();

    private SdkTts tts;
    private VoiceWizardOverlay overlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService micExecutor = Executors.newSingleThreadExecutor();
    private FieldDictationController dictationController;

    private Step step;
    private int retries;
    private String title;
    private String notes;
    private Calendar when;
    private boolean cancelled;

    private final Runnable stepTimeoutRunnable = this::onTimeout;
    private final Runnable globalTimeoutRunnable = this::cancelSession;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        synchronized (RUNNING_LOCK) {
            running = this;
        }
        overlay = new VoiceWizardOverlay(this, this);
        when = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"), Locale.GERMANY);
        startForeground(NOTIFICATION_ID, createNotification());
        tts = new SdkTts(this);
        dictationController = new FieldDictationController();

        // Warm up the TTS engine so the first real utterance is not dropped.
        // Some engines have a noticeable cold-start and ignore the first speak() call
        // after creation.  We issue a silent utterance and wait for its callback before
        // beginning the user interaction.
        tts.speak("", new SdkTts.Callback() {
            @Override public void onDone() {
                handler.post(() -> {
                    if (!cancelled) startFlow();
                });
            }

            @Override public void onError(Exception e) {
                // Even if warm-up fails, continue with the flow unless the session was cancelled.
                handler.post(() -> {
                    if (!cancelled) startFlow();
                });
            }
        });
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Voice Wizard",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Voice Wizard läuft")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();
    }

    private void startFlow() {
        Log.d(TAG, "startFlow");
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            stopSelf();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            overlay.showStep("Mikrofonberechtigung erforderlich", "", 0);
            handler.postDelayed(this::cancelSession, 3000);
            return;
        }
        step = Step.TITLE;
        retries = 0;
        cancelled = false;
        handler.postDelayed(globalTimeoutRunnable, GLOBAL_TIMEOUT);
        Log.d(TAG, "First step TITLE");
        overlay.showStep("Woran soll ich dich erinnern?", null, step.ordinal());
        speak("Hallo, woran soll ich dich erinnern?", true);
    }

    private void ask(String prompt) {
        if (cancelled) return;
        Log.d(TAG, "ask step=" + step + " prompt=" + prompt);
        overlay.showStep(prompt, null, step.ordinal());
        speak(prompt, true);
    }

    private void speak(String text, boolean listenAfter) {
        if (tts == null) return;
        Log.d(TAG, "speak text=" + text + " listenAfter=" + listenAfter);
        sendBroadcast(new Intent(ApiContracts.ACTION_PAUSE_STT));
        tts.speak(text, new SdkTts.Callback() {
            @Override public void onDone() { onTtsComplete(listenAfter); }

            @Override public void onError(Exception e) { onTtsComplete(listenAfter); }
        });
    }

    private void onTtsComplete(boolean listenAfter) {
        if (listenAfter) {
            overlay.showHint("Sprich jetzt");
            handler.postDelayed(stepTimeoutRunnable, STEP_TIMEOUT);
            micExecutor.execute(() -> {
                boolean granted = MicArbiter.get(VoiceWizardService.this).requestExclusiveMic(TAG, 2000L);
                if (!granted || cancelled) {
                    handler.removeCallbacks(stepTimeoutRunnable);
                    if (granted) {
                        MicArbiter.get(VoiceWizardService.this).releaseExclusiveMic(TAG);
                    }
                    VoiceWizardService.this.sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
                    VoiceWizardService.this.micIdle();
                    return;
                }
                handler.post(() -> {
                    if (cancelled) {
                        MicArbiter.get(VoiceWizardService.this).releaseExclusiveMic(TAG);
                        return;
                    }
                    dictationController.startDictation(VoiceWizardService.this, handler, new FieldDictationController.Listener() {
                        @Override
                        public void onPartialText(String text) {
                            overlay.updateTranscript(text);
                        }

                        @Override
                        public void onFinalText(String text) {
                            handler.removeCallbacks(stepTimeoutRunnable);
                            overlay.updateTranscript(text);
                            MicArbiter.get(VoiceWizardService.this).releaseExclusiveMic(TAG);
                            handleResult(text);
                        }

                        @Override
                        public void onError(Exception e) {
                            handler.removeCallbacks(stepTimeoutRunnable);
                            MicArbiter.get(VoiceWizardService.this).releaseExclusiveMic(TAG);
                            retry();
                        }

                        @Override
                        public void onTimeout() {
                            handler.removeCallbacks(stepTimeoutRunnable);
                            MicArbiter.get(VoiceWizardService.this).releaseExclusiveMic(TAG);
                            retry();
                        }
                    });
                });
            });
        } else {
            MicArbiter.get(this).releaseExclusiveMic(TAG);
            sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
            handler.post(this::safeEndSessionAndStop);
        }
    }
    private void handleResult(String text) {
        if (cancelled) return;
        if (text == null) text = "";
        text = text.trim();
        Log.d(TAG, "Result step=" + step + " text=" + text);
        if (text.isEmpty()) {
            Log.d(TAG, "No speech recognized");
            retry();
            return;
        }
        switch (step) {
            case TITLE:
                title = text;
                step = Step.NOTES;
                ask("Woran genau soll ich dich erinnern?");
                break;
            case NOTES:
                if (text.equalsIgnoreCase("ohne text")) text = "";
                notes = text;
                step = Step.DATE;
                ask("Datum?");
                break;
            case DATE:
                Long ts = GermanDateTimeNormalizer.parseDate(text,
                        java.util.TimeZone.getTimeZone("Europe/Berlin"),
                        java.util.Locale.GERMANY);
                if (ts == null) {
                    retry();
                } else {
                    when.setTimeInMillis(ts);
                    step = Step.TIME;
                    ask("Uhrzeit?");
                }
                break;
            case TIME:
                if (!GermanDateTimeNormalizer.applyTime(text, when)) {
                    retry();
                } else {
                    step = Step.CONFIRM;
                    String confirm = "\"" + title + "\" am " + formatDate(when) + " um " + formatTime(when) + "?";
                    ask(confirm);
                }
                break;
            case CONFIRM:
                String l = text.toLowerCase(Locale.GERMANY);
                if (l.contains("ja") || l.contains("ok") || l.contains("okay")
                        || l.equals("k") || l.contains("mach")
                        || l.contains("bestaetig") || l.contains("bestätig")) {
                    saveReminder();
                } else {
                    cancelSession();
                }
                break;
        }
    }

    private void retry() {
        if (cancelled) return;
        retries++;
        Log.d(TAG, "Retry step=" + step + " count=" + retries);
        if (retries >= 2) {
            Log.w(TAG, "Max retries reached");
            cancelSession();
        } else {
            ask(getPromptForStep());
        }
    }

    private String getGrammarForStep() {
        switch (step) {
            case DATE:
                return buildGrammarDate();
            case TIME:
                return buildGrammarTime();
            case CONFIRM:
                return buildGrammarConfirm();
            default:
                return null;
        }
    }

    private String buildGrammarDate() {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        addCommonTokens(tokens);
        java.util.Collections.addAll(tokens,
                "heute", "morgen", "uebermorgen", "uebernaechsten", "uebernaechste",
                "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonntag",
                "januar", "februar", "maerz", "märz", "april", "mai", "juni", "juli",
                "august", "september", "oktober", "november", "dezember",
                "jan", "feb", "maer", "mär", "apr", "jun", "jul", "aug", "sep", "okt", "nov", "dez",
                "in", "tag", "tage", "tagen", "woche", "wochen",
                "neunzehnhundert", "zweitausend", "punkt", ".");
        addNumberTokens(tokens, 31, true);   // days
        addNumberTokens(tokens, 12, true);   // months as numbers/ordinals
        addNumberTokens(tokens, 99, false);  // year fragments
        return toJson(tokens);
    }

    private String buildGrammarTime() {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        addCommonTokens(tokens);
        java.util.Collections.addAll(tokens,
                "uhr", "um", "halb", "viertel", "nach", "vor", "mittag", "mittags",
                "mitternacht", "null", "nulluhr");
        addNumberTokens(tokens, 23, false); // hours
        addNumberTokens(tokens, 59, false); // minutes
        for (int h = 0; h <= 23; h++) {
            String w = numberWord(h);
            if (w != null) {
                tokens.add(w + "uhr");
                tokens.add(ascii(w) + "uhr");
            }
            tokens.add(h + "uhr");
        }
        return toJson(tokens);
    }

    private String buildGrammarConfirm() {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        addCommonTokens(tokens);
        java.util.Collections.addAll(tokens,
                "ja", "okay", "ok", "k", "mach", "bestaetigen", "bestätigen",
                "nein", "abbrechen", "stop", "stopp", "cancel");
        return toJson(tokens);
    }

    private void addCommonTokens(java.util.List<String> tokens) {
        java.util.Collections.addAll(tokens,
                "[unk]", "<unk>", "<UNK>",
                "am", "um", "uhr", "den", "der", "im", "in", "und", "bis", "nach", "vor",
                "naechsten", "naechste", "kommenden", "diesem", "dieser");
    }

    private String toJson(java.util.List<String> list) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>(list);
        org.json.JSONArray arr = new org.json.JSONArray();
        for (String t : set) arr.put(t);
        return arr.toString();
    }

    private void addNumberTokens(java.util.List<String> tokens, int max, boolean ordinals) {
        for (int i = 0; i <= max; i++) {
            String w = numberWord(i);
            if (w != null) {
                tokens.add(w);
                tokens.add(ascii(w));
            }
            tokens.add(String.valueOf(i));
            if (ordinals && i > 0) {
                if (i == 1) { addOrdinal(tokens, "erst"); continue; }
                if (i == 3) { addOrdinal(tokens, "dritt"); continue; }
                if (i == 7) { addOrdinal(tokens, "siebt"); continue; }
                if (i == 8) { addOrdinal(tokens, "acht"); continue; }
                String base = w;
                String[] suf = (i >= 20) ? new String[]{"ste","sten","ster","stem","stes"}
                        : new String[]{"te","ten","ter","tem","tes"};
                for (String s : suf) {
                    String f = base + s;
                    tokens.add(f);
                    tokens.add(ascii(f));
                }
            }
        }
    }

    private void addOrdinal(java.util.List<String> tokens, String stem) {
        String[] endings = {"e","en","er","em","es"};
        for (String e : endings) {
            String f = stem + e;
            tokens.add(f);
            tokens.add(ascii(f));
        }
    }

    private String numberWord(int n) {
        String[] units = {"null", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun",
                "zehn", "elf", "zwölf", "dreizehn", "vierzehn", "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn"};
        if (n < units.length) return units[n];
        String[] tens = {"", "", "zwanzig", "dreißig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig"};
        int t = n / 10; int u = n % 10;
        if (u == 0) return tens[t];
        String unit = (u == 1) ? "ein" : numberWord(u);
        return unit + "und" + tens[t];
    }

    private String ascii(String s) {
        return s.replace("ä","ae").replace("ö","oe").replace("ü","ue").replace("ß","ss");
    }

    private String getPromptForStep() {
        switch (step) {
            case TITLE: return "Woran soll ich dich erinnern?";
            case NOTES: return "Woran genau soll ich dich erinnern?";
            case DATE: return "Datum?";
            case TIME: return "Uhrzeit?";
            case CONFIRM: return "Bestätigen?";
        }
        return "";
    }

    private void saveReminder() {
        try {
            Reminder r = new Reminder();
            r.id = UUID.randomUUID().toString();
            r.title = title;
            r.text = TextUtils.isEmpty(notes) ? title : notes;
            r.oneTime = true;
            r.oneTimeDate = when.getTimeInMillis();
            r.enabled = true;
            r.times = new String[]{formatTime(when)};
            r.timeEnabled = new boolean[]{true};
            r.days = new boolean[7];
            r.yesNoPrompt = false;
            r.repeatOnNo = false;
            r.priority = false;
            r.repeatIntervalHours = null;
            if (ReminderRepositoryCompat.add(this, r)) {
                ReminderScheduler.scheduleOneTime(this, r);
                Log.d(TAG, "Reminder saved id=" + r.id);
            } else {
                Log.e(TAG, "Failed to save");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save", e);
        }
        speak("Gespeichert.", false);
    }

    private void onTimeout() {
        Log.w(TAG, "Step timeout step=" + step);
        cancelled = true;
        speak("Zeitüberschreitung, ich breche ab.", false);
    }

    private void cancelSession() {
        if (cancelled) return;
        cancelled = true;
        Log.d(TAG, "Session cancelled at step=" + step);
        handler.removeCallbacks(stepTimeoutRunnable);
        handler.removeCallbacks(globalTimeoutRunnable);
        if (dictationController != null) {
            dictationController.cancel();
        }
        MicArbiter.get(this).releaseExclusiveMic(TAG);
        sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
        safeEndSessionAndStop();
    }
    
    private void micIdle() {
        Log.d(TAG, "Broadcast MIC_IDLE");
        sendBroadcast(new Intent(ApiContracts.ACTION_MIC_IDLE));
    }

    private void safeEndSessionAndStop() {
        Log.d(TAG, "safeEndSessionAndStop");
        handler.removeCallbacks(globalTimeoutRunnable);
        handler.removeCallbacks(stepTimeoutRunnable);

        try {
            Log.d(TAG, "releaseExclusiveMic");
            MicArbiter.get(this).releaseExclusiveMic(TAG);
        } catch (Exception ignored) { }
        micIdle();
        if (tts != null) {
            tts.stop();
            tts = null;
        }
        if (overlay != null) overlay.close();
        stopForeground(true);
        synchronized (RUNNING_LOCK) {
            if (running == this) {
                running = null;
            }
        }
        stopSelf();
    }

    private String formatDate(Calendar c) {
        return String.format(Locale.GERMANY, "%1$td.%1$tm.%1$tY", c);
    }

    private String formatTime(Calendar c) {
        return String.format(Locale.GERMANY, "%1$tH:%1$tM", c);
    }

    /**
     * Replace any previous running instance when the service restarts.
     *
     * <p>The static {@link #running} reference is updated under a lock and any previous
     * instance is asked to shut down to prevent resource leaks.</p>
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        VoiceWizardService previous;
        synchronized (RUNNING_LOCK) {
            previous = running;
            running = this;
        }
        if (previous != null && previous != this) {
            previous.speak("Ich starte neu.", false);
            previous.cancelSession();
        }
        Log.d(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onBack() {
        handler.removeCallbacks(stepTimeoutRunnable);
        if (dictationController != null) {
            dictationController.cancel();
        }
        MicArbiter.get(this).releaseExclusiveMic(TAG);
        sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
        Log.d(TAG, "onBack step=" + step);
        if (step == Step.TITLE) {
            cancelSession();
            return;
        }
        switch (step) {
            case NOTES:
                step = Step.TITLE;
                ask("Woran soll ich dich erinnern?");
                break;
            case DATE:
                step = Step.NOTES;
                ask("Woran genau soll ich dich erinnern?");
                break;
            case TIME:
                step = Step.DATE;
                ask("Datum?");
                break;
            case CONFIRM:
                step = Step.TIME;
                ask("Uhrzeit?");
                break;
        }
    }

    @Override
    public void onCancel() {
        Log.d(TAG, "onCancel");
        cancelSession();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        cancelSession();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}

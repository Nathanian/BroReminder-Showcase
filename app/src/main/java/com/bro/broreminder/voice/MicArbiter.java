package com.bro.broreminder.voice;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.AudioRecordingCallback;
import android.media.AudioRecordingConfiguration;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;

/**
 * Small helper that arbitrates microphone access between services. It monitors
 * active recording sessions and only grants access when the microphone is
 * free. All methods are blocking and should be called from a background
 * thread.
 */
public final class MicArbiter {
    private static final String TAG = "MicArbiter";
    private static final long DEFAULT_WAIT_MS = 1500L;

    private static MicArbiter instance;

    private final AudioManager audioManager;
    private String owner;
    private final AudioRecordingCallback recordingCallback;

    private MicArbiter(Context ctx) {
        audioManager = (AudioManager) ctx.getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= 24 && audioManager != null) {
            recordingCallback = new AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                    synchronized (MicArbiter.this) {
                        MicArbiter.this.notifyAll();
                    }
                }
            };
            audioManager.registerAudioRecordingCallback(recordingCallback, null);
        } else {
            recordingCallback = null;
        }
    }

    public static synchronized MicArbiter get(Context ctx) {
        if (instance == null) {
            instance = new MicArbiter(ctx);
        }
        return instance;
    }

    /**
     * Requests exclusive microphone ownership. This method blocks until the
     * microphone is free or the timeout elapses. Waiting threads are notified
     * when recording configurations change via
     * {@link AudioManager.AudioRecordingCallback}.
     */
    public boolean requestExclusiveMic(String tag, long waitMs) {
        long deadline = SystemClock.elapsedRealtime() +
                (waitMs > 0 ? waitMs : DEFAULT_WAIT_MS);
        synchronized (this) {
            while (SystemClock.elapsedRealtime() < deadline) {
                if (owner == null && isMicFree()) {
                    owner = tag;
                    Log.d(TAG, "Mic granted to " + tag);
                    return true;
                }
                long remaining = deadline - SystemClock.elapsedRealtime();
                if (remaining <= 0) break;
                try {
                    wait(remaining);
                } catch (InterruptedException ignored) {
                }
            }
        }
        Log.w(TAG, "Mic request timed out for " + tag);
        return false;
    }

    /** Releases the microphone if held by this owner. */
    public synchronized void releaseExclusiveMic(String tag) {
        if (owner != null && owner.equals(tag)) {
            Log.d(TAG, "Mic released by " + tag);
            owner = null;
            notifyAll();
        }
    }

    /**
     * @return true if there are no active recording configurations.
     */
    public boolean isMicFree() {
        if (audioManager == null) return true;
        if (Build.VERSION.SDK_INT >= 24) {
            List<AudioRecordingConfiguration> cfg =
                    audioManager.getActiveRecordingConfigurations();
            return cfg == null || cfg.isEmpty();
        }
        // On older versions we optimistically assume it is free
        return true;
    }
}

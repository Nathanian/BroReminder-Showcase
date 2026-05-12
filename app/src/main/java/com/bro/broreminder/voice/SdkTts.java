package com.bro.broreminder.voice;

import android.content.Context;

import androidx.annotation.Nullable;

import com.bro.speechsystem.sdk.SpeechSystem;
import com.bro.speechsystem.sdk.TtsController;

/**
 * Small wrapper around the SDK {@link TtsController} used within the app.
 * Keeps a single controller instance and exposes a minimal API.
 */
public final class SdkTts {

    /** Simple callback mirroring the SDK's {@link TtsController.TtsCallback}. */
    public interface Callback {
        void onDone();
        void onError(Exception e);
    }

    private final Context appContext;
    @Nullable private TtsController controller;

    public SdkTts(Context context) {
        appContext = context.getApplicationContext();
        controller = SpeechSystem.getInstance(appContext).getTtsController();
    }

    public void speak(String text, @Nullable Callback cb) {
        TtsController.TtsCallback delegate = new TtsController.TtsCallback() {
            @Override public void onDone(String id) {
                if (cb != null) cb.onDone();
            }

            @Override public void onError(String id, String msg) {
                if (cb != null) cb.onError(new Exception(msg));
                else android.util.Log.e("SdkTts", "TTS error (" + id + "): " + msg);
            }
        };
        if (controller == null) {
            controller = SpeechSystem.getInstance(appContext).getTtsController();
        }
        controller.speak(text, delegate);
    }

    public void stop() {
        // The SDK exposes no stop or shutdown API.
        // Drop our reference so the system can reclaim audio resources; a
        // fresh controller will be created on the next speak() call.
        controller = null;
    }
}

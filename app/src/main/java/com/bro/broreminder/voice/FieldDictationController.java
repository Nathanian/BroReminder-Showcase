package com.bro.broreminder.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.bro.speechsystem.sdk.BaseSession;
import com.bro.speechsystem.sdk.DialogSession;
import com.bro.speechsystem.sdk.SessionOptions;
import com.bro.speechsystem.sdk.SpeechSystem;

/**
 * Helper to run short dictation sessions for EditText fields.
 */
public class FieldDictationController {

    public interface Listener {
        void onPartialText(String text);
        void onFinalText(String text);
        void onError(Exception e);
        void onTimeout();
    }

    private DialogSession session;

    public void startDictation(Activity activity, Listener listener) {
        startDictation(activity, new Handler(activity.getMainLooper()), listener);
    }

    public void startDictation(Context context, Handler handler, Listener listener) {
        context.sendBroadcast(new Intent(ApiContracts.ACTION_PAUSE_STT));
        SessionOptions opts = new SessionOptions.Builder(SessionOptions.Mode.HOT_MIC)
                .withTimeout(5000)
                .build();
        try {
            session = SpeechSystem.getInstance(context).newDialogSession(opts, new DialogSession.Listener() {
                @Override
                public void onSessionReady(BaseSession s) {
                    s.listen();
                }

                @Override
                public void onListeningStarted() { }

                @Override
                public void onListeningTimeout() {
                    end();
                    handler.post(listener::onTimeout);
                }

                @Override
                public void onSessionEnded(String reason) {
                    session = null;
                    context.sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
                }

                @Override
                public void onError(String error) {
                    end();
                    handler.post(() -> listener.onError(new Exception(error)));
                }

                @Override
                public void onFinalUserInput(String text) {
                    end();
                    handler.post(() -> listener.onFinalText(text));
                }

                @Override
                public void onPartialUserInput(String text) {
                    handler.post(() -> listener.onPartialText(text));
                }
            });
        } catch (Exception e) {
            session = null;
            context.sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
            handler.post(() -> listener.onError(e));
            return;
        }
        if (session == null) {
            context.sendBroadcast(new Intent(ApiContracts.ACTION_RESUME_STT));
            handler.post(() -> listener.onError(new Exception("Failed to create dialog session")));
        }
    }

    private void end() {
        if (session != null) {
            session.endSession();
            session = null;
        }
    }

    public void cancel() {
        if (session != null) {
            session.endSession();
            session = null;
        }
    }
}

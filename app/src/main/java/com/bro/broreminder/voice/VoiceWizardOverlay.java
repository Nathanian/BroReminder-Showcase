package com.bro.broreminder.voice;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.bro.broreminder.R;
import com.airbnb.lottie.LottieAnimationView;

/**
 * Simple overlay dialog used by {@link VoiceWizardService} to collect user
 * input. It shows a prompt, live transcript and hint text along with back and
 * cancel buttons.
 */
public class VoiceWizardOverlay {

    public interface Listener {
        void onBack();
        void onCancel();
    }

    private final Context context;
    private final Context themedContext;
    private final Listener listener;
    private final WindowManager windowManager;
    private View view;
    private View cardView;
    private TextView promptText;
    private TextView transcriptText;
    private TextView hintText;
    private Button backButton;
    private Button cancelButton;
    private LottieAnimationView animationView;

    public VoiceWizardOverlay(Context ctx, Listener l) {
        this.context = ctx;
        this.themedContext = new ContextThemeWrapper(ctx, R.style.Theme_BroReminder);
        this.listener = l;
        this.windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    public void showStep(String prompt, String transcript, int stepIndex) {
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(themedContext);
            view = inflater.inflate(R.layout.voice_wizard_overlay, null);
            cardView = view.findViewById(R.id.card);
            promptText = view.findViewById(R.id.prompt);
            transcriptText = view.findViewById(R.id.transcript);
            hintText = view.findViewById(R.id.hint);
            backButton = view.findViewById(R.id.button_back);
            cancelButton = view.findViewById(R.id.button_cancel);
            animationView = view.findViewById(R.id.robot_animation);
            animationView.setFailureListener(e -> animationView.setImageResource(R.drawable.ic_launcher_foreground));
            backButton.setOnClickListener(v -> {
                backButton.setEnabled(false);
                listener.onBack();
            });
            cancelButton.setOnClickListener(v -> {
                cancelButton.setEnabled(false);
                listener.onCancel();
            });
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.CENTER;
            windowManager.addView(view, params);
            cardView.setScaleX(0.9f);
            cardView.setScaleY(0.9f);
            cardView.setAlpha(0f);
            cardView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200)
                    .withEndAction(() -> animationView.playAnimation()).start();
        } else {
            animationView.playAnimation();
        }
        promptText.setText(prompt);
        transcriptText.setText(transcript != null ? transcript : "");
        hintText.setText("Sprich jetzt");
        backButton.setEnabled(stepIndex > 0);
        cancelButton.setEnabled(true);
    }

    public void updateTranscript(String t) {
        if (transcriptText != null) transcriptText.setText(t);
    }

    public void showHint(String h) {
        if (hintText != null) hintText.setText(h);
    }

    public void showError(String e) {
        if (hintText != null) hintText.setText(e);
    }

    public void close() {
        if (view != null) {
            try {
                windowManager.removeView(view);
            } catch (Exception ignored) {
            }
            view = null;
            if (animationView != null) animationView.cancelAnimation();
        }
    }
}

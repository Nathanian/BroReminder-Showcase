package com.bro.broreminder.utils;

import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class ButtonUtils {
    public static void applyHaptic(final View v) {
        v.setHapticFeedbackEnabled(true);
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(50).start();
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(50).start();
                    break;
            }
            return false;
        });
    }
}
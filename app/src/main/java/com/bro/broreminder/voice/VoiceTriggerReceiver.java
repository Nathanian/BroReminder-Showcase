package com.bro.broreminder.voice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.util.Log;

/**
 * Entry point for voice-triggered reminder creation.
 */
public class VoiceTriggerReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceTriggerRcvr";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) return;

        boolean supported = ApiContracts.ACTION_START.equals(action)
                || VoiceActionRegistrar.ACTION_START_WIZARD.equals(action);

        if (!supported) {
            Log.w(TAG, "Ignoring unsupported action: " + action);
            return;
        }
        int uid = Binder.getCallingUid();
        if (!CallerCheck.isCallerAllowed(context, uid)) {
            Log.w(TAG, "Unauthorized caller uid=" + uid);
            return;
        }
        Log.d(TAG, "Starting voice session via action=" + action);
        Intent svc = new Intent(context, VoiceWizardService.class);
        context.startService(svc);
    }
}

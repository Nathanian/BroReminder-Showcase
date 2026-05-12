package com.bro.broreminder.voice;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.bro.speechsystem.sdk.SpeechSystem;
import com.bro.speechsystem.sdk.SpeechSystemAdmin;
import com.bro.speechsystem.sdk.data.ActionTarget.TargetType;
import com.bro.speechsystem.sdk.data.VoiceAction;

public final class VoiceActionRegistrar {
    public static final String PHRASE_REMIND_ME_DE = "erinnere mich";
    public static final String PHRASE_REMIND_ME_EN = "remind me";

    // SDK will deliver this Intent to our receiver
    public static final String ACTION_START_WIZARD = "com.bro.broreminder.ACTION_START_WIZARD";

    private VoiceActionRegistrar() {}

    public static void register(Context ctx) {
        SpeechSystemAdmin admin = SpeechSystem.getInstance(ctx.getApplicationContext()).getAdminController();

        // Target our existing VoiceTriggerReceiver
        Intent intent = new Intent(ctx, VoiceTriggerReceiver.class);
        intent.setAction(ACTION_START_WIZARD);

        VoiceAction actionDe = new VoiceAction(
                ctx, PHRASE_REMIND_ME_DE, "Startet den Reminder-Wizard", intent, TargetType.BROADCAST);
        VoiceAction actionEn = new VoiceAction(
                ctx, PHRASE_REMIND_ME_EN, "Starts the reminder wizard", intent, TargetType.BROADCAST);

        admin.registerVoiceAction(actionDe);
        admin.registerVoiceAction(actionEn);

        Toast.makeText(ctx, "Registered global commands: " +
                PHRASE_REMIND_ME_DE + " / " + PHRASE_REMIND_ME_EN, Toast.LENGTH_SHORT).show();
    }

    public static void unregister(Context ctx) {
        SpeechSystemAdmin admin = SpeechSystem.getInstance(ctx.getApplicationContext()).getAdminController();
        admin.unregisterVoiceAction(PHRASE_REMIND_ME_DE);
        admin.unregisterVoiceAction(PHRASE_REMIND_ME_EN);
        Toast.makeText(ctx, "Unregistered global commands.", Toast.LENGTH_SHORT).show();
    }
}

package com.bro.broreminder.voice;

import java.util.Locale;

public final class ApiContracts {
    private ApiContracts() {}

    public static final String PERMISSION_VOICE_BRIDGE = "com.bro.broreminder.permission.VOICE_BRIDGE";

    public static final String ACTION_START = "com.bro.reminder.VOICE_CREATE_START";
    public static final String ACTION_ASK = "com.bro.voice.ASK";
    public static final String ACTION_ANSWER = "com.bro.reminder.ANSWER";
    public static final String ACTION_CANCEL = "com.bro.reminder.VOICE_CREATE_CANCEL";

    // Broadcasts to coordinate microphone use between services
    public static final String ACTION_PAUSE_STT = "com.bro.speech.PAUSE_STT";
    public static final String ACTION_RESUME_STT = "com.bro.speech.RESUME_STT";
    // Optional broadcast a service may send once the microphone is fully released
    public static final String ACTION_MIC_IDLE = "com.bro.speech.MIC_IDLE";

    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_PROMPT = "prompt";
    public static final String EXTRA_EXPECT = "expect";
    public static final String EXTRA_ANSWER = "answer";
    public static final String EXTRA_LANG = "lang";

    public static final String EXPECT_TITLE = "TITLE";
    public static final String EXPECT_NOTES = "NOTES";
    public static final String EXPECT_DATE = "DATE";
    public static final String EXPECT_TIME = "TIME";
    public static final String EXPECT_CONFIRM = "CONFIRM";
    public static final String EXPECT_DONE = "DONE";
    public static final String EXPECT_CANCEL = "CANCEL";

    public static final String LANG = "de-DE";

    // TODO: Populate with the package name of the trusted Speech Service
    public static final String SPEECH_SERVICE_PACKAGE = "";

    public static Locale germanLocale() {
        return Locale.GERMANY;
    }
}

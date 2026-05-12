package com.bro.broreminder.utils;

/**
 * Demo-only configuration placeholders for public showcase builds.
 * Replace these values with your own environment-specific settings.
 */
public final class DemoConfig {
    public static final String SMTP_HOST = "YOUR_SERVER_IP";
    public static final int SMTP_PORT = 587;
    public static final String SMTP_USER = "YOUR_CLIENT_ID";
    public static final String SMTP_PASS = "YOUR_API_KEY";

    private DemoConfig() {}

    public static boolean isConfigured() {
        return !"YOUR_SERVER_IP".equals(SMTP_HOST)
                && !"YOUR_CLIENT_ID".equals(SMTP_USER)
                && !"YOUR_API_KEY".equals(SMTP_PASS);
    }
}

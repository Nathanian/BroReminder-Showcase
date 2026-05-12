package com.bro.broreminder.voice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates callers of the voice API.
 * If the allowlist is empty, validation relies solely on the signature permission.
 */
public final class CallerCheck {
    private static final String TAG = "VoiceCallerCheck";
    private static final Set<String> ALLOWLIST = new HashSet<>();

    static {
        // TODO: add trusted Speech Service package names
        ALLOWLIST.addAll(Collections.emptySet());
    }

    private CallerCheck() {}

    public static boolean isCallerAllowed(Context ctx, int uid) {
        if (ALLOWLIST.isEmpty()) {
            return true; // rely on permission
        }
        PackageManager pm = ctx.getPackageManager();
        String[] pkgs = pm.getPackagesForUid(uid);
        if (pkgs == null) {
            Log.w(TAG, "No packages for uid " + uid);
            return false;
        }
        for (String p : pkgs) {
            if (ALLOWLIST.contains(p)) return true;
        }
        Log.w(TAG, "UID " + uid + " not in allowlist");
        return false;
    }
}

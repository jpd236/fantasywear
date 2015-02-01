package com.jeffpdavidson.fantasywear.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.jeffpdavidson.fantasywear.sync.SyncAdapter;

/** SharedPreferences for FantasyWear. */
public final class Preferences {
    private static final String PREF_FILE = "prefs";

    private static final String KEY_SYNC_INTERVAL_SEC = "sync_interval_sec";
    private static final int DEFAULT_SYNC_INTERVAL_SEC = 30 * 60; // 30 minutes

    private Preferences() {}

    private static volatile SharedPreferences sPrefs;

    private static SharedPreferences getPrefs(Context context) {
        if (sPrefs == null) {
            synchronized (Preferences.class) {
                if (sPrefs == null) {
                    sPrefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
                }
            }
        }
        return sPrefs;
    }

    public static int getSyncIntervalSec(Context context) {
        return getPrefs(context).getInt(KEY_SYNC_INTERVAL_SEC, DEFAULT_SYNC_INTERVAL_SEC);
    }

    public static void setSyncIntervalSec(Context context, int syncIntervalSec) {
        SyncAdapter.setPeriodicSyncIntervalSec(context, syncIntervalSec);
        getPrefs(context).edit().putInt(KEY_SYNC_INTERVAL_SEC, syncIntervalSec).apply();
    }
}

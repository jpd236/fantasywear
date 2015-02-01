package com.jeffpdavidson.fantasywear.log;

import android.util.Log;

import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;

/**
 * Helper class for logging.
 *
 * Messages are prefixed with the calling class and function for easier debugging.
 *
 * Calls to {@link FWLog#d} are filtered at runtime via {@link Log#isLoggable}. It is assumed that
 * calls to {@link FWLog#v} will be filtered at compile time via Proguard.
 */
public final class FWLog {
    private static final String TAG = "FantasyWear";

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private FWLog() {}

    public static void wtf(String msg, Object... args) {
        Log.wtf(TAG, formatMsg(msg, args));
    }

    public static void wtf(Throwable tr, String msg, Object... args) {
        Log.wtf(TAG, formatMsg(msg, args), tr);
    }

    public static void e(String msg, Object... args) {
        Log.e(TAG, formatMsg(msg, args));
    }

    public static void e(Throwable tr, String msg, Object... args) {
        Log.e(TAG, formatMsg(msg, args), tr);
    }

    public static void w(String msg, Object... args) {
        Log.w(TAG, formatMsg(msg, args));
    }

    public static void w(Throwable tr, String msg, Object... args) {
        Log.w(TAG, formatMsg(msg, args), tr);
    }

    public static void i(String msg, Object... args) {
        Log.i(TAG, formatMsg(msg, args));
    }

    public static void i(Throwable tr, String msg, Object... args) {
        Log.i(TAG, formatMsg(msg, args), tr);
    }

    public static void d(String msg, Object... args) {
        if (DEBUG) {
            Log.d(TAG, formatMsg(msg, args));
        }
    }

    public static void d(Throwable tr, String msg, Object... args) {
        if (DEBUG) {
            Log.d(TAG, formatMsg(msg, args), tr);
        }
    }

    public static void v(String msg, Object... args) {
        Log.v(TAG, formatMsg(msg, args));
    }

    public static void v(Throwable tr, String msg, Object... args) {
        Log.v(TAG, formatMsg(msg, args), tr);
    }

    /**
     * Format the provided message per {@link String#format}, and prefix the message with the
     * calling class and method.
     */
    @VisibleForTesting
    static String formatMsg(String msg, Object... args) {
        String formattedMsg = String.format(msg, args);
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        boolean foundThisClass = false;
        for (StackTraceElement element : elements) {
            String[] classNameComponents = element.getClassName().split("\\.");
            String className = classNameComponents[classNameComponents.length - 1];
            boolean isFWLog = "FWLog".equals(className);
            if (!foundThisClass) {
                // Move up the stack until we find the first instance of FWLog.
                if (isFWLog) {
                    foundThisClass = true;
                }
            } else if (!isFWLog) {
                // After we find FWLog, move up until we find another class - this is the caller.
                return String.format("[%s.%s] %s",
                        className, element.getMethodName(), formattedMsg);
            }
        }
        // As fallback in case this fails for some reason, just format the message with no prefix.
        return formattedMsg;
    }
}

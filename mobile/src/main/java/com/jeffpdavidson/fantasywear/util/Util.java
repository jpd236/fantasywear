package com.jeffpdavidson.fantasywear.util;

import android.os.Looper;

import com.jeffpdavidson.fantasywear.log.FWLog;

public final class Util {
    private Util() {}

    public static void assertNotOnMainThread() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            FWLog.wtf("Called background method on main thread");
        }
    }
}
